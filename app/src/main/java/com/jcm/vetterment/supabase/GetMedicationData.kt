package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.Medication
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

object GetMedicationData {

    /**
     * Fetch all medications for a given case, ordered by creation date ascending.
     */
    suspend fun getMedicationsForCase(caseId: String): List<Medication> {
        if (caseId.isBlank()) return emptyList()
        return try {
            SupabaseClient.client
                .from("medications")
                .select {
                    filter { eq("case_id", caseId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Medication>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Insert a new medication record.
     * Automatically stamps the current user as prescribed_by.
     * Returns the inserted record, or null on failure.
     */
    suspend fun addMedication(medication: Medication): Medication? {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull() ?: return null
        return try {
            SupabaseClient.client
                .from("medications")
                .insert(medication.copy(prescribed_by = currentUser.id)) { select() }
                .decodeSingle<Medication>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Soft-delete: set is_active = false for a medication.
     * Returns true on success, false on failure.
     */
    suspend fun deactivateMedication(medicationId: String): Boolean {
        return try {
            SupabaseClient.client
                .from("medications")
                .update(mapOf("is_active" to false)) {
                    filter { eq("id", medicationId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
