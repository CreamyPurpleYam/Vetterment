package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

/**
 * One veterinarian's duty entry for a specific date and shift
 * (vet_schedules table). Schedules are assigned by the head veterinarian
 * (or admin) and shown to pet owners on the booking screen.
 */
@Serializable
data class VetSchedule(
    val id: String? = null,
    val vet_id: String,
    val schedule_date: String,   // yyyy-MM-dd
    val shift: String,           // MORNING / AFTERNOON / GRAVEYARD
    val start_time: String,      // HH:mm (24-hour)
    val end_time: String,        // HH:mm (24-hour); may be < start_time for graveyard
    val notes: String? = null,
    val created_by: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

/** Shift constants and display helpers for vet schedules. */
object VetShifts {
    const val MORNING = "MORNING"
    const val AFTERNOON = "AFTERNOON"
    const val GRAVEYARD = "GRAVEYARD"

    /** Display order: Morning -> Afternoon -> Graveyard. */
    val ALL = listOf(MORNING, AFTERNOON, GRAVEYARD)

    fun label(shift: String): String = when (shift) {
        MORNING -> "Morning Shift"
        AFTERNOON -> "Afternoon Shift"
        GRAVEYARD -> "Graveyard Shift"
        else -> shift.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    /** Compact label for chips and tight rows. */
    fun shortLabel(shift: String): String = when (shift) {
        MORNING -> "Morning"
        AFTERNOON -> "Afternoon"
        GRAVEYARD -> "Graveyard"
        else -> label(shift)
    }

    /** Sort key following ALL's order; unknown shifts sort last. */
    fun order(shift: String): Int = ALL.indexOf(shift).let { if (it == -1) ALL.size else it }
}
