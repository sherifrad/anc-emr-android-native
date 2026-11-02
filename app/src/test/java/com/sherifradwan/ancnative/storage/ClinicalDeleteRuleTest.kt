package com.sherifradwan.ancnative.storage

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The restore guard chain, run once per deletable type (see DeleteFixtures for the hard-coded expectations). */
class ClinicalDeleteRuleTest {
    private fun eachType(block: (DeleteFixture) -> Unit) = DeleteFixtures.all.forEach(block)

    private fun assertConflict(name: String, receipt: ClinicalCommandReceipt?, latest: ClinicalCommandReceipt?, row: Any?, cmd: ClinicalCommand) =
        assertEquals(name, ClinicalCommandFailure.RECORD_CONFLICT, ClinicalDeleteRule.evaluateRestore(receipt, latest, row, cmd))

    @Test
    fun matchingLatestDeleteReceiptWithAbsentRowAllowsRestore() = eachType { f ->
        val receipt = f.deleteReceipt()
        assertNull(f.type.name, ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, f.restore()))
    }

    @Test
    fun missingReceiptIsRecordConflict() = eachType { f ->
        assertConflict(f.type.name, null, null, null, f.restore())
    }

    @Test
    fun wrongReceiptKindIsRecordConflict() = eachType { f ->
        listOf(f.updateKind, f.restoreKind, f.otherDeleteKind).forEach { kind ->
            val wrong = f.deleteReceipt().copy(recordKind = kind.name)
            assertConflict("${f.type.name} $kind", wrong, wrong, null, f.restore())
        }
    }

    @Test
    fun receiptForAnotherPatientIsRecordConflict() = eachType { f ->
        val receipt = f.deleteReceipt().copy(patientId = "patient-2")
        assertConflict(f.type.name, receipt, receipt, null, f.restore())
    }

    @Test
    fun receiptForAnotherRecordIsRecordConflict() = eachType { f ->
        val receipt = f.deleteReceipt().copy(recordId = f.otherRecordId)
        assertConflict(f.type.name, receipt, receipt, null, f.restore())
    }

    @Test
    fun fingerprintMismatchIsRecordConflict() = eachType { f ->
        val tampered = f.deleteReceipt().copy(fingerprint = f.delete(recordRevision = 9).computeFingerprint())
        assertConflict(f.type.name, tampered, tampered, null, f.restore())
    }

    @Test
    fun deleteReceiptThatIsNotTheLatestIsRecordConflict() = eachType { f ->
        val receipt = f.deleteReceipt()
        val later = receipt.copy(
            operationUuid = UUID.randomUUID().toString(),
            recordKind = f.restoreKind.name,
            committedRevision = 5,
            committedPatientRevision = 6,
        )
        assertConflict(f.type.name, receipt, later, null, f.restore())
        assertConflict("${f.type.name} no latest", receipt, null, null, f.restore())
    }

    @Test
    fun existingRowIsRecordConflict() = eachType { f ->
        val receipt = f.deleteReceipt()
        assertConflict(f.type.name, receipt, receipt, f.recordOf(5), f.restore())
    }

    @Test
    fun recordHashDifferingFromDeletedHashIsInvalidCommand() = eachType { f ->
        val receipt = f.deleteReceipt()
        f.fieldVariants.filter { it.id == f.recordId && it.patientId == "patient-1" }.forEach { edited ->
            assertEquals(
                f.type.name,
                ClinicalCommandFailure.INVALID_COMMAND,
                ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, f.restore(record = edited)),
            )
        }
    }

    @Test
    fun revisionDifferenceAloneDoesNotBlockRestore() = eachType { f ->
        val receipt = f.deleteReceipt()
        assertNull(f.type.name, ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, f.restore(record = f.recordOf(77))))
    }

    @Test
    fun aCommandThatIsNotARestoreIsInvalidCommand() = eachType { f ->
        val receipt = f.deleteReceipt()
        assertEquals(
            f.type.name,
            ClinicalCommandFailure.INVALID_COMMAND,
            ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, f.delete()),
        )
    }

    @Test
    fun aSaveCommandIsInvalidCommand() {
        val save = SaveMedicationCommand(
            operationUuid = UUID.randomUUID().toString(), patientId = "patient-1", expectedPatientRevision = 4,
            medicationId = "med-1", expectedMedicationRevision = null, name = "Ferrous Sulfate", dosage = "200 mg",
            route = "Oral", frequency = "Once daily", startDate = "2026-03-15", endDate = null, status = "Active",
            notes = null, isProphylaxis = false,
        )
        val receipt = DeleteFixtures.medication.deleteReceipt()
        assertEquals(
            ClinicalCommandFailure.INVALID_COMMAND,
            ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, save),
        )
    }

    private fun otherOf(f: DeleteFixture) = DeleteFixtures.all.first { it.type != f.type }

    @Test
    fun recordFromAnotherPatientIsInvalidCommandAndNeverRestorable() = eachType { f ->
        val receipt = f.deleteReceipt()
        val foreign = f.fieldVariants.firstOrNull { it.patientId != "patient-1" } ?: f.recordOf(3).let { rec ->
            when (rec) {
                is com.sherifradwan.ancnative.model.DemoMedicationRecord -> rec.copy(patientId = "patient-2")
                is com.sherifradwan.ancnative.model.DemoLabInvestigation -> rec.copy(patientId = "patient-2")
                else -> error("unknown record")
            }
        }
        val cmd = f.restore(record = foreign)
        assertEquals(f.type.name, false, cmd.isStructurallyValid())
        assertEquals(f.type.name, ClinicalCommandFailure.INVALID_COMMAND, ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, cmd))
        // and the command's own patient differing from the record's is refused the same way
        val cmd2 = f.restore(patientId = "patient-2")
        assertEquals(f.type.name, ClinicalCommandFailure.INVALID_COMMAND, ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, cmd2))
    }

    @Test
    fun everyTypeMismatchIsInvalidCommandAndNeverThrows() = eachType { f ->
        val o = otherOf(f)
        val receipt = f.deleteReceipt()
        val cases = mapOf(
            "cmd type differs from record and source" to f.restore().copy(type = o.type),
            "source type differs from cmd and record" to f.restore(source = f.delete().copy(type = o.type)),
            "record differs from cmd and source" to f.restore(record = o.recordOf(3)),
            "cmd and source agree, record differs" to f.restore(source = f.delete().copy(type = o.type)).copy(type = o.type),
            "cmd and record agree, source differs" to o.restore(record = o.recordOf(3), source = f.delete()),
        )
        cases.forEach { (name, cmd) ->
            assertEquals("${f.type.name} $name", false, cmd.isStructurallyValid())
            assertEquals(
                "${f.type.name} $name",
                ClinicalCommandFailure.INVALID_COMMAND,
                ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, cmd),
            )
        }
    }
}
