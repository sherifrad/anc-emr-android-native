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
    private val lastSavedValues = mutableMapOf<DraftSlot, Map<String, String>>()

    fun onStateChanged(before: AncState, after: AncState) {
        val closedSlot = closedDraftSlot(before, after)
        if (closedSlot != null) {
            synchronized(lock) {
                pendingJobs.remove(closedSlot)?.cancel()
                pendingRecords.remove(closedSlot)
                lastSavedValues.remove(closedSlot)
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

        val record = draftRecordFor(after, nowMillis())
        if (record != null) {
            val slot = DraftSlot(record.patientId, record.screenKind)
            val shouldSchedule = synchronized(lock) {
                val isSameAsPending = pendingRecords[slot]?.fields == record.fields
                val isSameAsSaved = !pendingRecords.containsKey(slot) && lastSavedValues[slot] == record.fields
                if (!isSameAsPending && !isSameAsSaved) {
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
        val record = synchronized(lock) {
            pendingJobs.remove(slot)
            pendingRecords.remove(slot)
        } ?: return

        saveMutex.withLock {
            val success = try {
                store.save(record)
            } catch (_: Throwable) {
                false
            }
            synchronized(lock) {
                if (success) {
                    lastSavedValues[slot] = record.fields
                } else {
                    lastSavedValues.remove(slot)
                }
            }
        }
    }

    suspend fun flush() {
        try {
            val toSave = synchronized(lock) {
                val records = pendingRecords.values.toList()
                for ((_, job) in pendingJobs) {
                    job.cancel()
                }
                pendingJobs.clear()
                pendingRecords.clear()
                records
            }
            saveMutex.withLock {
                for (record in toSave) {
                    val slot = DraftSlot(record.patientId, record.screenKind)
                    val success = try {
                        store.save(record)
                    } catch (_: Throwable) {
                        false
                    }
                    synchronized(lock) {
                        if (success) {
                            lastSavedValues[slot] = record.fields
                        } else {
                            lastSavedValues.remove(slot)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun cancelPending() {
        synchronized(lock) {
            for ((_, job) in pendingJobs) {
                job.cancel()
            }
            pendingJobs.clear()
            pendingRecords.clear()
            lastSavedValues.clear()
        }
    }
}
