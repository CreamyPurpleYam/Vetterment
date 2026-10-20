package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Case
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

object GetAppointmentData {
    // ─── Create ───────────────────────────────────────────────────────────────

    suspend fun createAppointment(
        appointment: Appointment
    ): Appointment? {
        if (appointment.pet_id.isBlank()) return null
        if (appointment.owner_id.isBlank()) return null
        if (appointment.appointment_date.isBlank()) return null
        if (appointment.appointment_time.isBlank()) return null

        val normalizedType = appointment.appointment_type.trim().uppercase()
        if (normalizedType !in listOf(
                "CONSULTATION", "VACCINATION", "FOLLOW_UP", "SURGERY",
                "GROOMING", "EMERGENCY", "CONFINEMENT_FOLLOW_UP", "OTHER"
            )
        ) return null

        return try {
            SupabaseClient.client
                .from("appointments")
                .insert(appointment.copy(appointment_type = normalizedType)) { select() }
                .decodeSingle<Appointment>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    suspend fun getAppointmentById(
        appointmentId: String
    ): Appointment? {
        if (appointmentId.isBlank()) return null

        return try {
            SupabaseClient.client
                .from("appointments")
                .select { filter { eq("id", appointmentId) } }
                .decodeSingle<Appointment>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAllAppointments(): List<Appointment> {
        return try {
            SupabaseClient.client
                .from("appointments")
                .select {
                    order("appointment_date", Order.ASCENDING)
                    order("appointment_time", Order.ASCENDING)
                }
                .decodeList<Appointment>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAppointmentsForOwner(
        ownerId: String
    ): List<Appointment> {
        if (ownerId.isBlank()) return emptyList()

        return try {
            SupabaseClient.client
                .from("appointments")
                .select {
                    filter { eq("owner_id", ownerId) }
                    order("appointment_date", Order.DESCENDING)
                }
                .decodeList<Appointment>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── Status updates ───────────────────────────────────────────────────────

    suspend fun confirmAppointment(
        appointmentId: String
    ): Boolean {
        val currentUser =
            SupabaseClient.client.auth.currentUserOrNull() ?: return false

        return try {
            SupabaseClient.client
                .from("appointments").update(mapOf("status" to "CONFIRMED",
                        "confirmed_by" to currentUser.id
                    )
                ) { filter { eq("id", appointmentId) } }
            // Create a QUEUED case immediately on confirmation
            val appt = getAppointmentById(appointmentId)
            if (appt != null && appt.linked_case_id == null) {
                val caseType = when (appt.appointment_type) {
                    "FOLLOW_UP", "CONFINEMENT_FOLLOW_UP", "OTHER" -> "CONSULTATION"
                    else -> appt.appointment_type
                }
                val createdCase = GetCaseData.createCase(
                    petId = appt.pet_id,
                    caseType = caseType,
                    appointmentId = appointmentId
                )
                createdCase?.id?.let { caseId ->
                    SupabaseClient.client
                        .from("appointments")
                        .update(mapOf("linked_case_id" to caseId)) {
                            filter { eq("id", appointmentId) }
                        }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun cancelAppointment(
        appointmentId: String,
        notes: String? = null
    ): Boolean {
        return try {
            val update = buildMap<String, Any?> {
                put("status", "CANCELLED")
                notes?.trim()?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
            }
            SupabaseClient.client
                .from("appointments")
                .update(update) { filter { eq("id", appointmentId) } }
            // Also cancel the linked case if one exists
            val appointment = getAppointmentById(appointmentId)
            appointment?.linked_case_id?.let { caseId ->
                SupabaseClient.client
                    .from("cases")
                    .update(mapOf("current_status" to "CANCELLED")) {
                        filter { eq("id", caseId) }
                    }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun markNoShow(appointmentId: String): Boolean {
        return try {
            SupabaseClient.client
                .from("appointments")
                .update(mapOf("status" to "NO_SHOW")) {
                    filter { eq("id", appointmentId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun checkInAppointment(
        appointmentId: String
    ): Boolean {
        val appointment = getAppointmentById(appointmentId)
            ?: return false

        if (appointment.linked_case_id == null)
            return false
        return try {
            SupabaseClient.client
                .from("appointments")
                .update(mapOf("status" to "CHECKED_IN")) {
                    filter { eq("id", appointmentId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ─── Create case from appointment ─────────────────────────────────────────

    suspend fun getLinkedCase(
        appointmentId: String
    ): Case? {
        val appointment = getAppointmentById(appointmentId)
            ?: return null

        val caseId = appointment.linked_case_id
            ?: return null

        return GetCaseData.getCaseById(caseId)
    }
}
