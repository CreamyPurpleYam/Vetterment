package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Case
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

object GetAppointmentData {
    /** The three appointment options available to pet owners. */
    val APPOINTMENT_TYPES = listOf("CONSULTATION", "CONFINEMENT", "SURGERY")

    /** Initial doctor's fee per appointment type (PHP). Final billing is handled at the clinic. */
    fun doctorFee(appointmentType: String?): Int = when (appointmentType?.trim()?.uppercase()) {
        "CONFINEMENT", "SURGERY" -> 700
        else -> 400
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    suspend fun createAppointment(
        appointment: Appointment
    ): Appointment? {
        if (appointment.pet_id.isBlank()) return null
        if (appointment.owner_id.isBlank()) return null
        if (appointment.appointment_date.isBlank()) return null
        if (appointment.appointment_time.isBlank()) return null

        val normalizedType = appointment.appointment_type.trim().uppercase()
        if (normalizedType !in APPOINTMENT_TYPES) return null

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

    /** Vet-initiated follow-up checkup from a pet profile. Always a consultation — never a timeline. */
    suspend fun createFollowUpAppointment(
        petId: String,
        ownerId: String,
        date: String,
        time: String,
        reason: String? = null
    ): Appointment? {
        if (petId.isBlank() || ownerId.isBlank()) return null
        if (date.isBlank() || time.isBlank()) return null

        val followUp = Appointment(
            pet_id = petId,
            owner_id = ownerId,
            appointment_type = "CONSULTATION",
            appointment_date = date.trim(),
            appointment_time = "${time.trim()}:00",
            reason = reason?.trim()?.takeIf { it.isNotBlank() },
            notes = "Follow-up checkup scheduled by the clinic."
        )
        return try {
            SupabaseClient.client
                .from("appointments")
                .insert(followUp) { select() }
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

    /**
     * Confirms an appointment. Treatment is NOT started here — a case is only
     * created when the clinic physically accepts the pet (see [startTreatment]).
     */
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

    /** Marks an appointment as checked in. Treatment itself starts via [startTreatment]. */
    suspend fun checkInAppointment(
        appointmentId: String
    ): Boolean {
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

    /**
     * Completes a visit (consultation follow-through, or post-discharge cleanup).
     * Appointments with linked treatment cases auto-complete at discharge instead.
     */
    suspend fun completeAppointment(
        appointmentId: String
    ): Boolean {
        return try {
            SupabaseClient.client
                .from("appointments")
                .update(mapOf("status" to "COMPLETED")) {
                    filter { eq("id", appointmentId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Begins treatment for a confinement/surgery appointment at physical check-in.
     * This is the spec-compliant moment a treatment case is created — never at
     * booking or confirmation. Returns the created case, or null on failure.
     */
    suspend fun startTreatment(
        appointmentId: String
    ): Case? {
        val appointment = getAppointmentById(appointmentId) ?: return null

        // Consultations never get a treatment case
        if (appointment.appointment_type !in listOf("CONFINEMENT", "SURGERY")) return null

        // Reuse the existing case if treatment was already started
        appointment.linked_case_id?.let { caseId ->
            return GetCaseData.getCaseById(caseId)
        }

        val treatmentCase = GetCaseData.createCase(
            petId = appointment.pet_id,
            caseType = appointment.appointment_type,
            appointmentId = appointmentId
        ) ?: return null

        return try {
            SupabaseClient.client
                .from("appointments")
                .update(mapOf("linked_case_id" to treatmentCase.id)) {
                    filter { eq("id", appointmentId) }
                }
            treatmentCase
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── Linked case ──────────────────────────────────────────────────────────

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
