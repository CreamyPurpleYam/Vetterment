package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.CaseProgress
import com.jcm.vetterment.features.staff.cases.CaseStages
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

object GetCaseData {
    // ─── Create ─────────────────────────────────────────────────────────────    // ─── Read ────────────────────────────────────────────────────────────────

    suspend fun createCase(
        petId: String,
        caseType: String,
        appointmentId: String? = null
    ): Case? {
        if (petId.isBlank()) return null

        val normalizedType = caseType.trim().uppercase()
        if (normalizedType !in listOf(
                "CONSULTATION", "VACCINATION", "SURGERY",
                "CONFINEMENT", "TREATMENT", "GROOMING", "EMERGENCY"
            )
        ) return null

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

            // Insert the first progress entry
            insertProgress(
                caseId = created.id ?: return null,
                status = initialStatus,
                notes = "Case opened.",
                recordedBy = currentUser.id
            )

            created

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────    // ─── Read ────────────────────────────────────────────────────────────────
    suspend fun createFollowUpCase(
        previousCase: Case
    ): Case? {
        if (previousCase.id == null) return null
        if (previousCase.current_status !in listOf("COMPLETED", "DISCHARGED")) return null

        // Follow-ups are always consultations regardless of the original case type
        val caseType = "CONSULTATION"
        val currentUser =
            SupabaseClient.client.auth.currentUserOrNull() ?: return null

        val initialStatus = CaseStages.stagesFor(caseType).first()

        // root_case_id: use the predecessor's root if it has one, else the predecessor itself
        val rootCaseId = previousCase.root_case_id ?: previousCase.id

        val newCase = Case(
            pet_id = previousCase.pet_id,
            case_type = caseType,
            current_status = initialStatus,
            previous_case_id = previousCase.id,
            root_case_id = rootCaseId,
            created_by = currentUser.id
        )

        return try {
            val created = SupabaseClient.client
                .from("cases")
                .insert(newCase) { select() }
                .decodeSingle<Case>()

            insertProgress(
                caseId = created.id ?: return null,
                status = initialStatus,
                notes = "Follow-up case opened.",
                recordedBy = currentUser.id
            )

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

    // ─── Update ─────────────────────────────────────────────────────────────    // ─── Read ────────────────────────────────────────────────────────────────

    suspend fun advanceCaseStatus(
        caseId: String,
        notes: String = ""
    ): Case? {
        val currentCase = getCaseById(caseId) ?: return null
        val stages = CaseStages.stagesFor(currentCase.case_type)
        val currentIndex = stages.indexOf(currentCase.current_status)

        if (currentCase.assigned_vet_id == null) { return null }
        // Already at final stage
        if (currentIndex == -1 || currentIndex >= stages.lastIndex) return null

        val nextStatus = stages[currentIndex + 1]
        val currentUser =
            SupabaseClient.client.auth.currentUserOrNull() ?: return null

        return try {
            SupabaseClient.client
                .from("cases")
                .update(mapOf("current_status" to nextStatus)) {
                    filter { eq("id", caseId) }
                }

            insertProgress(
                caseId = caseId,
                status = nextStatus,
                notes = notes.trim().takeIf { it.isNotBlank() },
                recordedBy = currentUser.id
            )

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
            SupabaseClient.client
                .from("cases")
                .update(updates) {
                    filter { eq("id", caseId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun claimCase(caseId: String): Case? {
        val user = SupabaseClient.client.auth.currentUserOrNull()
            ?: return null

        return try {
            SupabaseClient.client
                .from("cases")
                .update(
                    mapOf("assigned_vet_id" to user.id)
                ) {
                    filter { eq("id", caseId) }
                }

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

    // ─── Private helpers ────────────────────────────────────────────────────    // ─── Read ────────────────────────────────────────────────────────────────

    private suspend fun insertProgress(
        caseId: String,
        status: String,
        notes: String? = null,
        recordedBy: String
    ) {
        try {
            val progress = CaseProgress(
                case_id = caseId,
                status = status,
                notes = notes,
                recorded_by = recordedBy
            )
            SupabaseClient.client
                .from("case_progress")
                .insert(progress)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
