package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.CaseProgress
import com.jcm.vetterment.features.staff.cases.CaseStages
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

object GetCaseData {
    // ─── Create ─────────────────────────────────────────────────────────────

    suspend fun createCase(
        petId: String,
        caseType: String,
        appointmentId: String? = null
    ): Case? {
        if (petId.isBlank()) return null

        val normalizedType = caseType.trim().uppercase()
        if (normalizedType !in CaseStages.CREATABLE_CASE_TYPES) return null

        val currentUser =
            SupabaseClient.client.auth.currentUserOrNull() ?: return null

        val initialStatus = CaseStages.stagesFor(normalizedType).first()

        val newCase = Case(
            pet_id = petId,
            case_type = normalizedType,
            current_status = initialStatus,
            appointment_id = appointmentId,
            created_by = currentUser.id
        )

        return try {
            val created = SupabaseClient.client
                .from("cases")
                .insert(newCase) { select() }
                .decodeSingle<Case>()

            // Insert the first progress entry. The case row itself is verified
            // above, so on progress failure return the case (a null would
            // invite a duplicate-case retry) and log the gap.
            val caseId = created.id
            if (caseId != null &&
                !insertProgressWithRetry(
                    caseId = caseId,
                    status = initialStatus,
                    notes = "Case opened.",
                    recordedBy = currentUser.id
                )
            ) {
                System.err.println("Case $caseId created but its initial progress entry could not be saved")
            }

            created

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── Read ────────────────────────────────────────────────────────────────

    suspend fun getCaseById(
        caseId: String
    ): Case? {
        if (caseId.isBlank()) return null

        return try {
            SupabaseClient.client
                .from("cases")
                .select {
                    filter { eq("id", caseId) }
                }
                .decodeSingle<Case>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCaseByAppointmentId(
        appointmentId: String
    ): Case? {
        if (appointmentId.isBlank()) return null

        return try {
            SupabaseClient.client
                .from("cases")
                .select {
                    filter { eq("appointment_id", appointmentId) }
                    limit(1)
                }
                .decodeList<Case>()
                .firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCasesForPet(
        petId: String
    ): List<Case> {
        if (petId.isBlank()) return emptyList()

        return try {
            SupabaseClient.client
                .from("cases")
                .select {
                    filter { eq("pet_id", petId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Case>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllCases(): List<Case> {
        return try {
            SupabaseClient.client
                .from("cases")
                .select {
                    order("updated_at", Order.DESCENDING)
                }
                .decodeList<Case>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getProgressForCase(
        caseId: String
    ): List<CaseProgress> {
        if (caseId.isBlank()) return emptyList()

        return try {
            SupabaseClient.client
                .from("case_progress")
                .select {
                    filter { eq("case_id", caseId) }
                    order("recorded_at", Order.ASCENDING)
                }
                .decodeList<CaseProgress>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    suspend fun advanceCaseStatus(
        caseId: String,
        notes: String = ""
    ): Case? {
        val currentCase = getCaseById(caseId) ?: return null
        val stages = CaseStages.stagesFor(currentCase.case_type, currentCase.current_status)
        val currentIndex = stages.indexOf(currentCase.current_status)

        if (currentCase.assigned_vet_id == null) { return null }
        // Already at final stage
        if (currentIndex == -1 || currentIndex >= stages.lastIndex) return null

        val nextStatus = stages[currentIndex + 1]
        val currentUser =
            SupabaseClient.client.auth.currentUserOrNull() ?: return null

        return try {
            // PostgREST reports 0-row updates as success, so verify the
            // status change actually landed before recording it.
            val updatedRows = SupabaseClient.client
                .from("cases")
                .update(mapOf("current_status" to nextStatus)) {
                    filter { eq("id", caseId) }
                    select()
                }
                .decodeList<Case>()
            if (updatedRows.isEmpty()) return null

            if (!insertProgressWithRetry(
                    caseId = caseId,
                    status = nextStatus,
                    notes = notes.trim().takeIf { it.isNotBlank() },
                    recordedBy = currentUser.id
                )
            ) {
                System.err.println("Case $caseId advanced to $nextStatus but the progress entry could not be saved")
            }

            // Discharge completes the linked appointment (treatment finished)
            if (nextStatus == "DISCHARGED") {
                currentCase.appointment_id?.let { appointmentId ->
                    try {
                        if (!GetAppointmentData.completeAppointment(appointmentId)) {
                            // One retry before giving up and logging the gap
                            if (!GetAppointmentData.completeAppointment(appointmentId)) {
                                System.err.println("Case $caseId discharged but appointment $appointmentId could not be completed")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            getCaseById(caseId)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateCaseDetails(
        caseId: String,
        diagnosis: String? = null,
        treatmentPlan: String? = null,
        medicationInstructions: String? = null,
        notes: String? = null,
        dischargeSummary: String? = null,
        admissionDate: String? = null,
        expectedDischargeDate: String? = null,
        actualDischargeDate: String? = null
    ): Boolean {
        if (caseId.isBlank()) return false

        val updates = buildMap<String, Any?> {
            diagnosis?.let { put("diagnosis", it.trim().takeIf { v -> v.isNotBlank() }) }
            treatmentPlan?.let { put("treatment_plan", it.trim().takeIf { v -> v.isNotBlank() }) }
            medicationInstructions?.let { put("medication_instructions", it.trim().takeIf { v -> v.isNotBlank() }) }
            notes?.let { put("notes", it.trim().takeIf { v -> v.isNotBlank() }) }
            dischargeSummary?.let { put("discharge_summary", it.trim().takeIf { v -> v.isNotBlank() }) }
            admissionDate?.let { put("admission_date", it.trim().takeIf { v -> v.isNotBlank() }) }
            expectedDischargeDate?.let { put("expected_discharge_date", it.trim().takeIf { v -> v.isNotBlank() }) }
            actualDischargeDate?.let { put("actual_discharge_date", it.trim().takeIf { v -> v.isNotBlank() }) }
        }

        if (updates.isEmpty()) return false

        return try {
            val updatedRows = SupabaseClient.client
                .from("cases")
                .update(updates) {
                    filter { eq("id", caseId) }
                    select()
                }
                .decodeList<Case>()
            updatedRows.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun claimCase(caseId: String): Case? {
        val user = SupabaseClient.client.auth.currentUserOrNull()
            ?: return null

        return try {
            // Update with select() and decode the returned rows: an
            // RLS-blocked or stale claim affects 0 rows and must not be
            // reported as success.
            SupabaseClient.client
                .from("cases")
                .update(
                    mapOf("assigned_vet_id" to user.id)
                ) {
                    filter { eq("id", caseId) }
                    select()
                }
                .decodeList<Case>()
                .firstOrNull()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private suspend fun insertProgress(
        caseId: String,
        status: String,
        notes: String? = null,
        recordedBy: String
    ): Boolean {
        return try {
            val progress = CaseProgress(
                case_id = caseId,
                status = status,
                notes = notes,
                recorded_by = recordedBy
            )
            SupabaseClient.client
                .from("case_progress")
                .insert(progress)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Inserts a progress entry, retrying once before giving up. */
    private suspend fun insertProgressWithRetry(
        caseId: String,
        status: String,
        notes: String? = null,
        recordedBy: String
    ): Boolean {
        if (insertProgress(caseId, status, notes, recordedBy)) return true
        return insertProgress(caseId, status, notes, recordedBy)
    }
}
