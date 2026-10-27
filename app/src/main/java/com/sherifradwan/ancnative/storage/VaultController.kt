package com.sherifradwan.ancnative.storage

import android.content.Context
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal enum class ProvisioningStage {
    DESCRIPTOR_PERSISTED,
    WRAPPING_ALIAS_GENERATED,
    ENVELOPE_PERSISTED,
    DATABASE_SEEDED,
    ACTIVE_POINTER_PUBLISHED,
}

internal fun interface ProvisioningHook {
    fun reached(stage: ProvisioningStage)
}

internal fun interface OpeningHook {
    fun beforeSessionAdmission()
}

class VaultController internal constructor(
    context: Context,
    private val files: VaultFiles,
    private val keys: VaultKeyOperations,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val random: SecureRandom = SecureRandom(),
    private val provisioningHook: ProvisioningHook = ProvisioningHook {},
    private val openingHook: OpeningHook = OpeningHook {},
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val storageMutex = Mutex()
    private val stateLock = Any()
    private val gate = EpochGate()
    private val authIds = AtomicLong(0)
    private val _view = MutableStateFlow(VaultSessionView())
    val view: StateFlow<VaultSessionView> = _view.asStateFlow()

    @Volatile private var foreground = true
    @Volatile private var pendingAuthentication: PendingAuthentication? = null
    private var session: VaultSession? = null
    private var retainedUnadmittedHandle: OpenedRecordsDatabase? = null

    init {
        val inspectionEpoch = gate.current()
        scope.launch { inspectStorage(inspectionEpoch) }
    }

    fun onForegrounded() {
        synchronized(stateLock) { foreground = true }
    }

    fun onBackgrounded(authenticationOverlayActive: Boolean) {
        synchronized(stateLock) { foreground = false }
        if (!authenticationOverlayActive) lock()
    }

    fun beginInitialization() {
        val reservationEpoch = synchronized(stateLock) {
            if (_view.value.state != VaultState.Uninitialized) return
            gate.advance().also {
                _view.value = VaultSessionView(VaultState.Opening(AuthenticationPurpose.INITIALIZE))
            }
        }
        scope.launch {
            storageMutex.withLock {
                if (!openingReservationIsCurrent(reservationEpoch, AuthenticationPurpose.INITIALIZE)) return@withLock
                val vaultUuid = UUID.randomUUID().toString()
                val aliasId = UUID.randomUUID().toString()
                try {
                    if (files.inspect() != VaultDiskState.Empty || keys.hasAnyAlias()) {
                        blockIfCurrent(reservationEpoch, VaultBlockReason.INCOMPLETE_SETUP)
                        return@withLock
                    }
                    files.prepareDescriptor(VaultDescriptor(vaultUuid, aliasId))
                    provisioningHook.reached(ProvisioningStage.DESCRIPTOR_PERSISTED)
                    when (keys.generate(aliasId)) {
                        is KeyAccessResult.Success -> Unit
                        is KeyAccessResult.Failure -> {
                            blockIfCurrent(reservationEpoch, VaultBlockReason.INCOMPLETE_SETUP)
                            return@withLock
                        }
                    }
                    provisioningHook.reached(ProvisioningStage.WRAPPING_ALIAS_GENERATED)
                    if (!openingReservationIsCurrent(reservationEpoch, AuthenticationPurpose.INITIALIZE)) {
                        blockIfCurrent(reservationEpoch, VaultBlockReason.INCOMPLETE_SETUP)
                        return@withLock
                    }
                    val request = AuthenticationRequest(
                        id = authIds.incrementAndGet(),
                        purpose = AuthenticationPurpose.INITIALIZE,
                    )
                    synchronized(stateLock) {
                        if (openingReservationIsCurrentLocked(reservationEpoch, AuthenticationPurpose.INITIALIZE)) {
                            pendingAuthentication = PendingAuthentication(
                                request,
                                VaultDescriptor(vaultUuid, aliasId),
                                reservationEpoch,
                            )
                            _view.value = VaultSessionView(VaultState.Authenticating(request))
                        }
                    }
                } catch (_: Throwable) {
                    blockIfCurrent(reservationEpoch, VaultBlockReason.INCOMPLETE_SETUP)
                }
            }
        }
    }

    fun beginUnlock() {
        val reservationEpoch = synchronized(stateLock) {
            if (_view.value.state != VaultState.Locked) return
            gate.advance().also {
                _view.value = VaultSessionView(VaultState.Opening(AuthenticationPurpose.UNLOCK))
            }
        }
        scope.launch {
            storageMutex.withLock {
                if (!openingReservationIsCurrent(reservationEpoch, AuthenticationPurpose.UNLOCK)) return@withLock
                try {
                    if (retainedUnadmittedHandle != null) {
                        blockIfCurrent(reservationEpoch, VaultBlockReason.STORAGE_BUSY)
                        return@withLock
                    }
                    val descriptor = when (val disk = files.inspect()) {
                        is VaultDiskState.Active -> disk.descriptor
                        VaultDiskState.Empty -> {
                            publishIfCurrent(reservationEpoch, VaultSessionView(VaultState.Uninitialized))
                            return@withLock
                        }
                        VaultDiskState.Incomplete -> {
                            blockIfCurrent(reservationEpoch, VaultBlockReason.INCOMPLETE_SETUP)
                            return@withLock
                        }
                        VaultDiskState.Invalid -> {
                            blockIfCurrent(reservationEpoch, VaultBlockReason.DESCRIPTOR_INVALID)
                            return@withLock
                        }
                    }
                    when (files.envelopeState(descriptor.vaultUuid)) {
                        VaultArtifactState.MISSING -> {
                            blockIfCurrent(reservationEpoch, VaultBlockReason.ENVELOPE_MISSING)
                            return@withLock
                        }
                        VaultArtifactState.INVALID -> {
                            blockIfCurrent(reservationEpoch, VaultBlockReason.ENVELOPE_INVALID)
                            return@withLock
                        }
                        VaultArtifactState.PRESENT -> Unit
                    }
                    when (files.databaseState(descriptor.vaultUuid)) {
                        VaultArtifactState.MISSING -> {
                            blockIfCurrent(reservationEpoch, VaultBlockReason.DATABASE_MISSING)
                            return@withLock
                        }
                        VaultArtifactState.INVALID -> {
                            blockIfCurrent(reservationEpoch, VaultBlockReason.DATABASE_INTEGRITY_FAILED)
                            return@withLock
                        }
                        VaultArtifactState.PRESENT -> Unit
                    }
                    if (!keys.contains(descriptor.aliasId)) {
                        blockIfCurrent(reservationEpoch, VaultBlockReason.KEY_MISSING)
                        return@withLock
                    }
                    val request = AuthenticationRequest(
                        id = authIds.incrementAndGet(),
                        purpose = AuthenticationPurpose.UNLOCK,
                    )
                    synchronized(stateLock) {
                        if (openingReservationIsCurrentLocked(reservationEpoch, AuthenticationPurpose.UNLOCK)) {
                            pendingAuthentication = PendingAuthentication(request, descriptor, reservationEpoch)
                            _view.value = VaultSessionView(VaultState.Authenticating(request))
                        }
                    }
                } catch (_: Throwable) {
                    blockIfCurrent(reservationEpoch, VaultBlockReason.DESCRIPTOR_INVALID)
                }
            }
        }
    }

    fun completeAuthentication(requestId: Long) {
        val pending = synchronized(stateLock) {
            val candidate = pendingAuthentication
            val state = _view.value.state as? VaultState.Authenticating
            if (
                candidate == null ||
                candidate.request.id != requestId ||
                state?.request?.id != requestId ||
                !foreground ||
                !gate.accepts(candidate.epoch)
            ) {
                return
            }
            pendingAuthentication = null
            _view.value = VaultSessionView(VaultState.Opening(candidate.request.purpose))
            candidate
        }
        scope.launch {
            storageMutex.withLock {
                when (pending.request.purpose) {
                    AuthenticationPurpose.INITIALIZE -> finishInitialization(pending)
                    AuthenticationPurpose.UNLOCK -> openActiveSession(pending)
                }
            }
        }
    }

    fun cancelAuthentication(requestId: Long) {
        synchronized(stateLock) {
            val pending = pendingAuthentication ?: return
            val state = _view.value.state as? VaultState.Authenticating ?: return
            if (pending.request.id != requestId || state.request.id != requestId) return
            pendingAuthentication = null
            gate.advance()
            if (pending.request.purpose == AuthenticationPurpose.INITIALIZE) {
                scope.launch {
                    storageMutex.withLock {
                        if (!files.activePointerFile.exists()) {
                            files.cleanupUnpublishedArtifacts()
                            keys.deleteOrphanAliases()
                        }
                    }
                }
                _view.value = VaultSessionView(VaultState.Uninitialized)
            } else {
                _view.value = VaultSessionView(VaultState.Locked)
            }
        }
    }

    fun authenticationDetached(requestId: Long) = cancelAuthentication(requestId)

    fun deviceCredentialUnavailable() {
        synchronized(stateLock) {
            if (_view.value.state is VaultState.Unlocked) return
            val purpose = pendingAuthentication?.request?.purpose
            gate.advance()
            pendingAuthentication = null
            _view.value = when (purpose) {
                AuthenticationPurpose.INITIALIZE -> {
                    scope.launch {
                        storageMutex.withLock {
                            if (!files.activePointerFile.exists()) {
                                files.cleanupUnpublishedArtifacts()
                                keys.deleteOrphanAliases()
                            }
                        }
                    }
                    VaultSessionView(VaultState.Uninitialized)
                }
                AuthenticationPurpose.UNLOCK -> VaultSessionView(VaultState.Locked)
                null -> VaultSessionView(VaultState.Blocked(VaultBlockReason.DEVICE_CREDENTIAL_UNAVAILABLE))
            }
        }
    }

    fun retryInspection() {
        val inspectionEpoch = synchronized(stateLock) {
            if (_view.value.state !is VaultState.Blocked) return
            gate.advance().also { _view.value = VaultSessionView(VaultState.Inspecting) }
        }
        scope.launch {
            storageMutex.withLock {
                if (!files.activePointerFile.exists()) {
                    files.cleanupUnpublishedArtifacts()
                    keys.deleteOrphanAliases()
                }
            }
            inspectStorage(inspectionEpoch)
        }
    }

    fun lock() {
        val closingEpoch = synchronized(stateLock) {
            val current = _view.value.state
            if (
                current is VaultState.Locked ||
                current is VaultState.Uninitialized ||
                current is VaultState.Inspecting ||
                current is VaultState.Closing
            ) {
                return
            }
            gate.advance().also {
                pendingAuthentication = null
                _view.value = VaultSessionView(VaultState.Closing())
            }
        }
        scope.launch {
            val reporter = launch {
                delay(CLOSE_REPORTING_DEADLINE_MS)
                synchronized(stateLock) {
                    if (gate.accepts(closingEpoch) && _view.value.state is VaultState.Closing) {
                        _view.value = VaultSessionView(VaultState.Closing(busy = true))
                    }
                }
            }
            var closeFailed = false
            storageMutex.withLock {
                try {
                    session?.close()
                    session = null
                    retainedUnadmittedHandle?.close()
                    retainedUnadmittedHandle = null
                } catch (_: Throwable) {
                    closeFailed = true
                }
            }
            reporter.cancel()
            val next = if (closeFailed || session != null || retainedUnadmittedHandle != null) {
                VaultSessionView(VaultState.Blocked(VaultBlockReason.STORAGE_BUSY))
            } else {
                try {
                    when (files.inspect()) {
                        is VaultDiskState.Active -> VaultSessionView(VaultState.Locked)
                        VaultDiskState.Empty -> VaultSessionView(VaultState.Uninitialized)
                        VaultDiskState.Incomplete -> VaultSessionView(VaultState.Blocked(VaultBlockReason.INCOMPLETE_SETUP))
                        VaultDiskState.Invalid -> VaultSessionView(VaultState.Blocked(VaultBlockReason.DESCRIPTOR_INVALID))
                    }
                } catch (_: Throwable) {
                    VaultSessionView(VaultState.Blocked(VaultBlockReason.DESCRIPTOR_INVALID))
                }
            }
            synchronized(stateLock) {
                if (gate.accepts(closingEpoch) && _view.value.state is VaultState.Closing) {
                    _view.value = next
                }
            }
        }
    }

    suspend fun save(
        command: SaveVisitCommand,
        sessionEpoch: Long,
        onReconciliation: suspend () -> Unit,
    ): SaveOutcome = withContext(dispatcher) {
        storageMutex.withLock {
            val active = session
            if (
                active == null ||
                _view.value.state != VaultState.Unlocked(sessionEpoch) ||
                !gate.accepts(sessionEpoch)
            ) {
                return@withLock SaveOutcome.Obsolete
            }
            val coordinator = SaveCoordinator(active.repository, gate::accepts)
            val result = coordinator.execute(command, sessionEpoch, onReconciliation)
            if (result is SaveOutcome.Committed) {
                synchronized(stateLock) {
                    if (
                        gate.accepts(sessionEpoch) &&
                        _view.value.state == VaultState.Unlocked(sessionEpoch)
                    ) {
                        _view.value = VaultSessionView(VaultState.Unlocked(sessionEpoch), result.snapshot)
                    }
                }
            }
            result
        }
    }

    suspend fun execute(
        command: PatientCommand,
        sessionEpoch: Long,
        onReconciliation: suspend () -> Unit,
    ): PatientCommandOutcome = withContext(dispatcher) {
        storageMutex.withLock {
            val active = session
            if (
                active == null ||
                _view.value.state != VaultState.Unlocked(sessionEpoch) ||
                !gate.accepts(sessionEpoch)
            ) {
                return@withLock PatientCommandOutcome.Obsolete
            }
            var result = active.repository.execute(command)
            if (!gate.accepts(sessionEpoch)) return@withLock PatientCommandOutcome.Obsolete
            if (result is PatientCommandOutcome.Ambiguous) {
                onReconciliation()
                if (!gate.accepts(sessionEpoch)) return@withLock PatientCommandOutcome.Obsolete
                result = active.repository.reconcile(command)
            }
            if (!gate.accepts(sessionEpoch)) return@withLock PatientCommandOutcome.Obsolete
            if (result is PatientCommandOutcome.Committed) {
                synchronized(stateLock) {
                    if (
                        gate.accepts(sessionEpoch) &&
                        _view.value.state == VaultState.Unlocked(sessionEpoch)
                    ) {
                        _view.value = VaultSessionView(VaultState.Unlocked(sessionEpoch), result.snapshot)
                    }
                }
            }
            result
        }
    }

    suspend fun execute(
        command: ClinicalCommand,
        sessionEpoch: Long,
        onReconciliation: suspend () -> Unit,
    ): ClinicalCommandOutcome = withContext(dispatcher) {
        storageMutex.withLock {
            val active = session
            if (
                active == null ||
                _view.value.state != VaultState.Unlocked(sessionEpoch) ||
                !gate.accepts(sessionEpoch)
            ) {
                return@withLock ClinicalCommandOutcome.Obsolete
            }
            var result = active.repository.execute(command)
            if (!gate.accepts(sessionEpoch)) return@withLock ClinicalCommandOutcome.Obsolete
            if (result is ClinicalCommandOutcome.Ambiguous) {
                onReconciliation()
                if (!gate.accepts(sessionEpoch)) return@withLock ClinicalCommandOutcome.Obsolete
                result = active.repository.reconcile(command)
            }
            if (!gate.accepts(sessionEpoch)) return@withLock ClinicalCommandOutcome.Obsolete
            if (result is ClinicalCommandOutcome.Committed) {
                synchronized(stateLock) {
                    if (
                        gate.accepts(sessionEpoch) &&
                        _view.value.state == VaultState.Unlocked(sessionEpoch)
                    ) {
                        _view.value = VaultSessionView(VaultState.Unlocked(sessionEpoch), result.snapshot)
                    }
                }
            }
            result
        }
    }

    suspend fun saveAttachment(
        attachment: com.sherifradwan.ancnative.model.DemoAttachment,
        sessionEpoch: Long,
    ): Boolean = withContext(dispatcher) {
        storageMutex.withLock {
            val active = session ?: return@withLock false
            if (_view.value.state != VaultState.Unlocked(sessionEpoch)) return@withLock false
            val success = active.repository.saveAttachment(attachment)
            if (success) {
                val snapshot = active.repository.loadSnapshot()
                synchronized(stateLock) {
                    if (_view.value.state == VaultState.Unlocked(sessionEpoch)) {
                        _view.value = VaultSessionView(VaultState.Unlocked(sessionEpoch), snapshot)
                    }
                }
            }
            success
        }
    }

    suspend fun deleteAttachment(
        attachmentId: String,
        patientId: String,
        sessionEpoch: Long,
    ): Boolean = withContext(dispatcher) {
        storageMutex.withLock {
            val active = session ?: return@withLock false
            if (_view.value.state != VaultState.Unlocked(sessionEpoch)) return@withLock false
            val success = active.repository.deleteAttachment(attachmentId, patientId)
            if (success) {
                val snapshot = active.repository.loadSnapshot()
                synchronized(stateLock) {
                    if (_view.value.state == VaultState.Unlocked(sessionEpoch)) {
                        _view.value = VaultSessionView(VaultState.Unlocked(sessionEpoch), snapshot)
                    }
                }
            }
            success
        }
    }

    suspend fun exportEncryptedBackup(passphrase: CharArray): ByteArray? = withContext(dispatcher) {
        storageMutex.withLock {
            val active = session ?: return@withLock null
            val snapshot = active.repository.loadSnapshot()
            val vaultUuid = active.repository.vaultUuid
            EncryptedBackupManager.createEncryptedBackup(
                vaultUuid = vaultUuid,
                patients = snapshot.patients,
                passphrase = passphrase,
            )
        }
    }

    suspend fun restoreEncryptedBackup(
        backupBytes: ByteArray,
        passphrase: CharArray,
    ): BackupInspectionResult = withContext(dispatcher) {
        storageMutex.withLock {
            val active = session ?: return@withLock BackupInspectionResult.Error("Vault not unlocked")
            val inspection = EncryptedBackupManager.inspectBackup(backupBytes, passphrase)
            if (inspection !is BackupInspectionResult.Valid) {
                return@withLock inspection
            }
            val payload = EncryptedBackupManager.decryptAndParseBackup(backupBytes, passphrase)
                ?: return@withLock BackupInspectionResult.InvalidPassphraseOrCorrupted

            val newSnapshot = try {
                active.repository.restoreCanonicalBackup(payload)
            } catch (t: Throwable) {
                return@withLock BackupInspectionResult.Error(t.message ?: "Failed to restore database")
            }

            synchronized(stateLock) {
                val epoch = gate.current()
                _view.value = VaultSessionView(VaultState.Unlocked(epoch), newSnapshot)
            }
            inspection
        }
    }

    private suspend fun inspectStorage(inspectionEpoch: Long) = storageMutex.withLock {
        if (session != null || retainedUnadmittedHandle != null) {
            publishIfCurrent(
                inspectionEpoch,
                VaultSessionView(VaultState.Blocked(VaultBlockReason.STORAGE_BUSY)),
            )
            return@withLock
        }
        val inspected = try {
            when (val disk = files.inspect()) {
                VaultDiskState.Empty -> {
                    if (keys.hasAnyAlias()) {
                        VaultSessionView(VaultState.Blocked(VaultBlockReason.INCOMPLETE_SETUP))
                    } else {
                        VaultSessionView(VaultState.Uninitialized)
                    }
                }
                VaultDiskState.Incomplete -> VaultSessionView(VaultState.Blocked(VaultBlockReason.INCOMPLETE_SETUP))
                VaultDiskState.Invalid -> VaultSessionView(VaultState.Blocked(VaultBlockReason.DESCRIPTOR_INVALID))
                is VaultDiskState.Active -> VaultSessionView(VaultState.Locked)
            }
        } catch (_: Throwable) {
            VaultSessionView(VaultState.Blocked(VaultBlockReason.DESCRIPTOR_INVALID))
        }
        synchronized(stateLock) {
            if (gate.accepts(inspectionEpoch) && _view.value.state is VaultState.Inspecting) {
                _view.value = inspected
            }
        }
    }

    private suspend fun finishInitialization(pending: PendingAuthentication) {
        val descriptor = pending.descriptor
        if (!authenticationStillCurrent(pending)) return
        val passphrase = ByteArray(PASSPHRASE_BYTES).also(random::nextBytes)
        try {
            val wrapped = when (val result = keys.wrap(descriptor.vaultUuid, descriptor.aliasId, passphrase)) {
                is KeyAccessResult.Success -> result.value
                is KeyAccessResult.Failure -> {
                    blockAuthentication(pending, result.reason.toBlockReason())
                    return
                }
            }
            if (!authenticationStillCurrent(pending)) return
            files.writeEnvelope(wrapped)
            provisioningHook.reached(ProvisioningStage.ENVELOPE_PERSISTED)
            if (!authenticationStillCurrent(pending)) return

            val opened = RecordsDatabase.open(
                appContext,
                files.databaseFile(descriptor.vaultUuid).absolutePath,
                passphrase,
            )
            try {
                val repository = RecordsRepository(opened.database, descriptor.vaultUuid)
                repository.seedInitialSyntheticFixtures()
                when (repository.validateAndLoad()) {
                    is RepositoryOpenResult.Success -> Unit
                    is RepositoryOpenResult.Failure -> throw IllegalStateException("seed-validation-failed")
                }
                opened.database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(PASSIVE)").close()
            } finally {
                opened.close()
            }
            provisioningHook.reached(ProvisioningStage.DATABASE_SEEDED)
            if (!authenticationStillCurrent(pending)) return
            files.publishActive(descriptor.vaultUuid)
            provisioningHook.reached(ProvisioningStage.ACTIVE_POINTER_PUBLISHED)
            synchronized(stateLock) {
                if (authenticationStillCurrentLocked(pending)) {
                    gate.advance()
                    _view.value = VaultSessionView(VaultState.Locked)
                }
            }
        } catch (_: Throwable) {
            blockAuthentication(pending, VaultBlockReason.INCOMPLETE_SETUP)
        } finally {
            passphrase.fill(0)
        }
    }

    private suspend fun openActiveSession(pending: PendingAuthentication) {
        val descriptor = pending.descriptor
        if (!authenticationStillCurrent(pending)) return
        when (files.envelopeState(descriptor.vaultUuid)) {
            VaultArtifactState.MISSING -> {
                blockAuthentication(pending, VaultBlockReason.ENVELOPE_MISSING)
                return
            }
            VaultArtifactState.INVALID -> {
                blockAuthentication(pending, VaultBlockReason.ENVELOPE_INVALID)
                return
            }
            VaultArtifactState.PRESENT -> Unit
        }
        when (files.databaseState(descriptor.vaultUuid)) {
            VaultArtifactState.MISSING -> {
                blockAuthentication(pending, VaultBlockReason.DATABASE_MISSING)
                return
            }
            VaultArtifactState.INVALID -> {
                blockAuthentication(pending, VaultBlockReason.DATABASE_INTEGRITY_FAILED)
                return
            }
            VaultArtifactState.PRESENT -> Unit
        }
        val envelope = try {
            files.readEnvelope(descriptor.vaultUuid)
        } catch (_: Throwable) {
            blockAuthentication(pending, VaultBlockReason.ENVELOPE_INVALID)
            return
        }
        if (envelope.vaultUuid != descriptor.vaultUuid || envelope.aliasId != descriptor.aliasId) {
            blockAuthentication(pending, VaultBlockReason.VAULT_METADATA_MISMATCH)
            return
        }
        if (!authenticationStillCurrent(pending)) return
        val passphrase = when (val result = keys.unwrap(envelope)) {
            is KeyAccessResult.Success -> result.value
            is KeyAccessResult.Failure -> {
                blockAuthentication(pending, result.reason.toBlockReason())
                return
            }
        }
        if (passphrase.size != PASSPHRASE_BYTES) {
            passphrase.fill(0)
            blockAuthentication(pending, VaultBlockReason.ENVELOPE_INVALID)
            return
        }
        if (!authenticationStillCurrent(pending)) {
            passphrase.fill(0)
            return
        }
        var opened: OpenedRecordsDatabase? = null
        var admitted = false
        var failureReason: VaultBlockReason? = null
        try {
            val openedDatabase = try {
                RecordsDatabase.open(
                    appContext,
                    files.databaseFile(descriptor.vaultUuid).absolutePath,
                    passphrase,
                )
            } catch (failure: Throwable) {
                val reason = RecordsMigration.failureReason(failure) ?: if (
                    failure.javaClass.name.endsWith("SQLiteNotADatabaseException")
                ) {
                    VaultBlockReason.DATABASE_INTEGRITY_FAILED
                } else {
                    VaultBlockReason.DATABASE_OPEN_FAILED
                }
                blockAuthentication(pending, reason)
                return
            }
            opened = openedDatabase
            val repository = RecordsRepository(openedDatabase.database, descriptor.vaultUuid)
            when (val validation = repository.validateAndLoad()) {
                is RepositoryOpenResult.Failure -> {
                    failureReason = validation.reason
                }
                is RepositoryOpenResult.Success -> {
                    openedDatabase.database.openHelper.writableDatabase.query("SELECT count(*) FROM patients").close()
                    openingHook.beforeSessionAdmission()
                    synchronized(stateLock) {
                        if (authenticationStillCurrentLocked(pending)) {
                            val sessionEpoch = gate.advance()
                            session = VaultSession(openedDatabase, repository)
                            _view.value = VaultSessionView(VaultState.Unlocked(sessionEpoch), validation.snapshot)
                            admitted = true
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            failureReason = VaultBlockReason.DATABASE_OPEN_FAILED
        } finally {
            if (!admitted) {
                opened?.let { handle ->
                    if (!closeUnadmitted(handle)) {
                        failureReason = VaultBlockReason.STORAGE_BUSY
                    }
                }
                failureReason?.let { blockAuthentication(pending, it) }
            }
            passphrase.fill(0)
        }
    }

    private fun closeUnadmitted(opened: OpenedRecordsDatabase): Boolean = try {
        opened.close()
        true
    } catch (_: Throwable) {
        retainedUnadmittedHandle = opened
        false
    }

    private fun authenticationStillCurrent(pending: PendingAuthentication): Boolean = synchronized(stateLock) {
        authenticationStillCurrentLocked(pending)
    }

    private fun authenticationStillCurrentLocked(pending: PendingAuthentication): Boolean {
        val opening = _view.value.state as? VaultState.Opening
        return foreground &&
            gate.accepts(pending.epoch) &&
            opening?.purpose == pending.request.purpose
    }

    private fun blockAuthentication(pending: PendingAuthentication, reason: VaultBlockReason) {
        synchronized(stateLock) {
            if (authenticationStillCurrentLocked(pending)) {
                gate.advance()
                _view.value = VaultSessionView(VaultState.Blocked(reason))
            }
        }
    }

    private fun openingReservationIsCurrent(epoch: Long, purpose: AuthenticationPurpose): Boolean =
        synchronized(stateLock) { openingReservationIsCurrentLocked(epoch, purpose) }

    private fun openingReservationIsCurrentLocked(epoch: Long, purpose: AuthenticationPurpose): Boolean {
        val opening = _view.value.state as? VaultState.Opening
        return foreground && gate.accepts(epoch) && opening?.purpose == purpose
    }

    private fun blockIfCurrent(epoch: Long, reason: VaultBlockReason) {
        synchronized(stateLock) {
            if (gate.accepts(epoch)) {
                gate.advance()
                _view.value = VaultSessionView(VaultState.Blocked(reason))
            }
        }
    }

    private fun publishIfCurrent(epoch: Long, view: VaultSessionView) {
        synchronized(stateLock) {
            if (gate.accepts(epoch)) {
                gate.advance()
                _view.value = view
            }
        }
    }

    private fun KeyAccessFailure.toBlockReason(): VaultBlockReason = when (this) {
        KeyAccessFailure.Missing -> VaultBlockReason.KEY_MISSING
        KeyAccessFailure.PermanentlyInvalidated -> VaultBlockReason.KEY_PERMANENTLY_INVALIDATED
        KeyAccessFailure.AuthenticationRequired -> VaultBlockReason.DEVICE_CREDENTIAL_UNAVAILABLE
        KeyAccessFailure.AuthenticationFailed -> VaultBlockReason.GCM_AUTHENTICATION_FAILED
        KeyAccessFailure.InvalidEnvelope -> VaultBlockReason.ENVELOPE_INVALID
    }

    private data class PendingAuthentication(
        val request: AuthenticationRequest,
        val descriptor: VaultDescriptor,
        val epoch: Long,
    )

    private class VaultSession(
        private val openedDatabase: OpenedRecordsDatabase,
        val repository: RecordsRepository,
    ) : AutoCloseable {
        override fun close() = openedDatabase.close()
    }

    companion object {
        private const val PASSPHRASE_BYTES = 32
        private const val CLOSE_REPORTING_DEADLINE_MS = 2_000L

        @Volatile private var canonicalInstance: VaultController? = null

        fun get(context: Context): VaultController = canonicalInstance ?: synchronized(this) {
            canonicalInstance ?: VaultController(
                context = context.applicationContext,
                files = VaultFiles.canonical(context.applicationContext),
                keys = AndroidVaultKeys(
                    aliasPrefix = VaultFiles.canonical(context.applicationContext).aliasPrefix,
                    orphanDetectionPrefixes = setOf(VaultFiles.LEGACY_CANONICAL_ALIAS_PREFIX),
                ),
            ).also { canonicalInstance = it }
        }
    }
}
