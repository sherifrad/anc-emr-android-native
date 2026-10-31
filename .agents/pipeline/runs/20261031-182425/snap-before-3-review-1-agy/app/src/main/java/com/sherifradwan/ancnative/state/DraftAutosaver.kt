package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.DraftRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface DraftStore {
    suspend fun save(record: DraftRecord): Boolean
    suspend fun delete(slot: DraftSlot)
}

class DraftAutosaver(
    private val scope: CoroutineScope,
    private val store: DraftStore,
    private val debounceMillis: Long = 1000L,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val lock = Any()
    private val saveMutex = Mutex()
    private val pendingRecords = mutableMapOf<DraftSlot, DraftRecord>()
    private val pendingJobs = mutableMapOf<DraftSlot, Job>()
    private val latestRequestedFields = mutableMapOf<DraftSlot, Map<String, String>>()
    private val slotGenerations = mutableMapOf<DraftSlot, Long>()
    private val heldSlots = mutableSetOf<DraftSlot>()

    fun holdSlot(slot: DraftSlot) {
        synchronized(lock) {
            heldSlots.add(slot)
            pendingJobs.remove(slot)?.cancel()
            pendingRecords.remove(slot)
            latestRequestedFields.remove(slot)
            slotGenerations[slot] = (slotGenerations[slot] ?: 0L) + 1L
        }
    }

    fun releaseSlot(slot: DraftSlot) {
        synchronized(lock) {
            heldSlots.remove(slot)
        }
    }

    fun onStateChanged(before: AncState, after: AncState) {
        val closedSlot = closedDraftSlot(before, after)
        if (closedSlot != null) {
            val isHeld = synchronized(lock) { heldSlots.contains(closedSlot) }
            if (!isHeld) {
                synchronized(lock) {
                    pendingJobs.remove(closedSlot)?.cancel()
                    pendingRecords.remove(closedSlot)
                    latestRequestedFields.remove(closedSlot)
                    slotGenerations[closedSlot] = (slotGenerations[closedSlot] ?: 0L) + 1L
                }
                scope.launch {
                    saveMutex.withLock {
                        try {
                            store.delete(closedSlot)
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }

        val record = draftRecordFor(after, nowMillis())
        if (record != null) {
            val slot = DraftSlot(record.patientId, record.screenKind)
            val shouldSchedule = synchronized(lock) {
                if (!heldSlots.contains(slot) && latestRequestedFields[slot] != record.fields) {
                    latestRequestedFields[slot] = record.fields
                    pendingRecords[slot] = record
                    pendingJobs.remove(slot)?.cancel()
                    true
                } else {
                    false
                }
            }
            if (shouldSchedule) {
                val job = scope.launch {
                    delay(debounceMillis)
                    performSave(slot)
                }
                synchronized(lock) {
                    if (pendingRecords.containsKey(slot)) {
                        pendingJobs[slot] = job
                    }
                }
            }
        }
    }

    private suspend fun performSave(slot: DraftSlot) {
        val (record, gen) = synchronized(lock) {
            if (heldSlots.contains(slot)) return
            pendingJobs.remove(slot)
            val r = pendingRecords.remove(slot) ?: return
            r to (slotGenerations[slot] ?: 0L)
        }

        saveMutex.withLock {
            val isStillValid = synchronized(lock) {
                !heldSlots.contains(slot) && (slotGenerations[slot] ?: 0L) == gen
            }
            if (!isStillValid) {
                return
            }
            val success = try {
                store.save(record)
            } catch (_: Throwable) {
                false
            }
            synchronized(lock) {
                if (!success && (slotGenerations[slot] ?: 0L) == gen) {
                    if (latestRequestedFields[slot] == record.fields) {
                        latestRequestedFields.remove(slot)
                    }
                }
            }
        }
    }

    suspend fun flush() {
        try {
            val toSave = synchronized(lock) {
                val nonHeldSlots = pendingRecords.keys.filter { !heldSlots.contains(it) }
                val items = nonHeldSlots.mapNotNull { slot ->
                    pendingRecords.remove(slot)?.let { record ->
                        Triple(slot, record, slotGenerations[slot] ?: 0L)
                    }
                }
                for (slot in nonHeldSlots) {
                    pendingJobs.remove(slot)?.cancel()
                }
                items
            }
            saveMutex.withLock {
                for ((slot, record, gen) in toSave) {
                    val isStillValid = synchronized(lock) {
                        !heldSlots.contains(slot) && (slotGenerations[slot] ?: 0L) == gen
                    }
                    if (!isStillValid) continue

                    val success = try {
                        store.save(record)
                    } catch (_: Throwable) {
                        false
                    }
                    synchronized(lock) {
                        if (!success && (slotGenerations[slot] ?: 0L) == gen) {
                            if (latestRequestedFields[slot] == record.fields) {
                                latestRequestedFields.remove(slot)
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun cancelPending() {
        synchronized(lock) {
            heldSlots.clear()
            for ((_, job) in pendingJobs) {
                job.cancel()
            }
            pendingJobs.clear()
            pendingRecords.clear()
            latestRequestedFields.clear()
            for (slot in slotGenerations.keys.toList()) {
                slotGenerations[slot] = (slotGenerations[slot] ?: 0L) + 1L
            }
        }
    }
}
