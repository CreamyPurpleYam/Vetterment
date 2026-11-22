package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.features.data_models.Roles
import com.jcm.vetterment.features.data_models.VetSchedule
import com.jcm.vetterment.features.data_models.VetShifts
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

/**
 * Veterinarian work schedules (vet_schedules table).
 *
 * Schedules are assigned per specific date by the head veterinarian (or
 * admin) and are read by pet owners to see who is on duty when booking.
 *
 * Writes are row-verified: PostgREST reports 0-row updates/deletes as HTTP
 * success, so every update/delete checks the returned rows before claiming
 * the write landed. Reads throw on failure so callers can show an error
 * instead of mistaking a network failure for an empty roster.
 */
object GetScheduleData {

    private val TIME_FORMAT = Regex("([01]\\d|2[0-3]):[0-5]\\d")
    private val DATE_FORMAT = Regex("\\d{4}-\\d{2}-\\d{2}")

    /**
     * All schedules for one date, sorted Morning -> Afternoon -> Graveyard,
     * then by start time.
     */
    suspend fun getSchedulesForDate(date: String): List<VetSchedule> {
        if (date.isBlank()) return emptyList()
        val schedules = SupabaseClient.client
            .from("vet_schedules")
            .select { filter { eq("schedule_date", date.trim()) } }
            .decodeList<VetSchedule>()
        return schedules.sortedWith(
            compareBy({ VetShifts.order(it.shift) }, { it.start_time })
        )
    }

    /**
     * Vets / head vets on duty for a date with their schedule entries,
     * sorted by shift then start time.
     *
     * Read-only and informational — the clinic stays first-come,
     * first-served; the attending veterinarian is assigned at the clinic.
     */
    suspend fun getOnDutyForDate(date: String): List<Pair<Profile, VetSchedule>> {
        val schedules = getSchedulesForDate(date)
        if (schedules.isEmpty()) return emptyList()

        val profilesById =
            GetProfileData.getProfilesByIds(schedules.map { it.vet_id }.distinct())
        return schedules.mapNotNull { schedule ->
            val profile = profilesById[schedule.vet_id]
            // Only veterinarian-level staff belong on the duty list, even if
            // a stray schedule row ever references another role.
            if (profile != null && Roles.isVetLevel(profile.role)) profile to schedule else null
        }
    }

    /** All schedules for one veterinarian, optionally from a date onwards. */
    suspend fun getSchedulesForVet(vetId: String, fromDate: String? = null): List<VetSchedule> {
        if (vetId.isBlank()) return emptyList()
        return SupabaseClient.client
            .from("vet_schedules")
            .select {
                filter {
                    eq("vet_id", vetId.trim())
                    fromDate?.let { gte("schedule_date", it.trim()) }
                }
            }
            .decodeList<VetSchedule>()
            .sortedWith(
                compareBy({ it.schedule_date }, { VetShifts.order(it.shift) }, { it.start_time })
            )
    }

    /**
     * Inserts a new schedule entry or updates an existing one. A new entry
     * whose vet/date/shift combination already exists updates that row
     * (UNIQUE constraint) instead of failing. Editing updates by id.
     */
    suspend fun saveSchedule(schedule: VetSchedule): Result<VetSchedule> {
        val vetId = schedule.vet_id.trim()
        val date = schedule.schedule_date.trim()
        val startTime = schedule.start_time.trim()
        val endTime = schedule.end_time.trim()

        if (vetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Please choose a veterinarian."))
        }
        if (!DATE_FORMAT.matches(date)) {
            return Result.failure(IllegalArgumentException("Please pick a schedule date."))
        }
        if (schedule.shift !in VetShifts.ALL) {
            return Result.failure(IllegalArgumentException("Please choose a shift."))
        }
        if (!TIME_FORMAT.matches(startTime)) {
            return Result.failure(IllegalArgumentException("Start time must be HH:mm (24-hour)."))
        }
        if (!TIME_FORMAT.matches(endTime)) {
            return Result.failure(IllegalArgumentException("End time must be HH:mm (24-hour)."))
        }
        if (startTime == endTime) {
            return Result.failure(IllegalArgumentException("Start and end time cannot be the same."))
        }

        // Client-side guard; RLS on vet_schedules is the real gate.
        val currentRole = GetProfileData.getCurrentRole()
        if (!Roles.isStaffManager(currentRole)) {
            return Result.failure(IllegalStateException("Only the head veterinarian can manage schedules."))
        }

        return try {
            if (schedule.id != null) {
                // Editing: block colliding with a different entry of the same
                // vet/date/shift, then update by id.
                val collidingId = findScheduleId(vetId, date, schedule.shift)
                if (collidingId != null && collidingId != schedule.id) {
                    return Result.failure(
                        IllegalArgumentException(
                            "This veterinarian already has a schedule for that shift on that date."
                        )
                    )
                }
                updateScheduleRow(
                    rowId = schedule.id,
                    vetId = vetId,
                    date = date,
                    shift = schedule.shift,
                    startTime = startTime,
                    endTime = endTime,
                    notes = schedule.notes
                )
            } else {
                // New entry: the UNIQUE (vet_id, schedule_date, shift)
                // constraint means a re-added shift must update the existing
                // row instead of erroring.
                val existingId = findScheduleId(vetId, date, schedule.shift)
                if (existingId != null) {
                    updateScheduleRow(
                        rowId = existingId,
                        vetId = vetId,
                        date = date,
                        shift = schedule.shift,
                        startTime = startTime,
                        endTime = endTime,
                        notes = schedule.notes
                    )
                } else {
                    val inserted = SupabaseClient.client
                        .from("vet_schedules")
                        .insert(
                            mapOf(
                                "vet_id" to vetId,
                                "schedule_date" to date,
                                "shift" to schedule.shift,
                                "start_time" to startTime,
                                "end_time" to endTime,
                                "notes" to schedule.notes?.trim()?.takeIf { it.isNotBlank() },
                                "created_by" to SupabaseClient.client.auth.currentUserOrNull()?.id
                            )
                        ) { select() }
                        .decodeSingle<VetSchedule>()
                    Result.success(inserted)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /** Finds the id of an existing vet/date/shift entry, if any. */
    private suspend fun findScheduleId(vetId: String, date: String, shift: String): String? {
        val existing = SupabaseClient.client
            .from("vet_schedules")
            .select {
                filter {
                    eq("vet_id", vetId)
                    eq("schedule_date", date)
                    eq("shift", shift)
                }
                limit(1)
            }
            .decodeList<VetSchedule>()
        return existing.firstOrNull()?.id
    }

    /** Row-verified update of one schedule entry. */
    private suspend fun updateScheduleRow(
        rowId: String,
        vetId: String,
        date: String,
        shift: String,
        startTime: String,
        endTime: String,
        notes: String?
    ): Result<VetSchedule> {
        val updatedRows = SupabaseClient.client
            .from("vet_schedules")
            .update(
                mapOf(
                    "vet_id" to vetId,
                    "schedule_date" to date,
                    "shift" to shift,
                    "start_time" to startTime,
                    "end_time" to endTime,
                    "notes" to notes?.trim()?.takeIf { it.isNotBlank() }
                )
            ) {
                filter { eq("id", rowId) }
                select()
            }
            .decodeList<VetSchedule>()

        // PostgREST reports 0-row updates as success, so verify the write
        // actually landed before claiming it did.
        return if (updatedRows.isEmpty()) {
            Result.failure(IllegalStateException("Schedule entry not found or no longer manageable."))
        } else {
            Result.success(updatedRows.first())
        }
    }

    /** Deletes a schedule entry. True only when a row was actually removed. */
    suspend fun deleteSchedule(id: String): Boolean {
        if (id.isBlank()) return false
        return try {
            // PostgREST reports 0-row deletes as success, so verify the row
            // was actually removed before claiming it was.
            val deletedRows = SupabaseClient.client
                .from("vet_schedules")
                .delete {
                    filter { eq("id", id) }
                    select()
                }
                .decodeList<VetSchedule>()
            deletedRows.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
