package com.jcm.vetterment.features.data_models

/**
 * Central role definitions for VETterment.
 *
 * Roles are stored as free-form strings in `profiles.role`. Keep every role
 * comparison in the app going through these constants and helpers so the role
 * system stays consistent across screens.
 *
 * Documented system roles (claude.md) mapped to stored values:
 *   Admin             -> "admin"          (system maintenance; seeded manually)
 *   Head Veterinarian -> "head_vet"       (clinic operations manager, highest staff role)
 *   Veterinarian      -> "vet"            (diagnoses and treats patients)
 *   Clinical Staff    -> "clinical_staff" (check-ins, case intake, notes, medications)
 *   Pet Owner         -> "pet_owner"
 */
object Roles {
    const val ADMIN = "admin"
    const val HEAD_VET = "head_vet"
    const val VET = "vet"
    const val CLINICAL_STAFF = "clinical_staff"
    const val PET_OWNER = "pet_owner"

    /** Legacy role strings that existed before the restructure; kept only for defensive matching. */
    val LEGACY_STAFF_ROLES = listOf("branch_manager", "nurse", "assistant_vet")

    /** All roles that use the staff app (dashboards, cases, appointments). */
    val STAFF_ROLES = listOf(HEAD_VET, VET, CLINICAL_STAFF)

    /** Roles allowed to create and manage staff accounts (head veterinarian only). */
    val STAFF_MANAGER_ROLES = listOf(HEAD_VET, ADMIN)

    /** Roles allowed to claim a treatment case and advance/diagnose (veterinarian level). */
    val VET_LEVEL_ROLES = listOf(VET, HEAD_VET)

    /** Roles the head veterinarian may create via Create Staff Account. */
    val CREATABLE_STAFF_ROLES = listOf(HEAD_VET, VET, CLINICAL_STAFF)

    /** Roles the head veterinarian may edit an existing staff account into. */
    val EDITABLE_STAFF_ROLES = listOf(HEAD_VET, VET, CLINICAL_STAFF)

    /** Display names for UI. */
    fun displayName(role: String?): String = when (role) {
        ADMIN -> "Admin"
        HEAD_VET -> "Head Veterinarian"
        VET -> "Veterinarian"
        CLINICAL_STAFF -> "Clinical Staff"
        PET_OWNER -> "Pet Owner"
        "branch_manager" -> "Head Veterinarian" // legacy value
        "assistant_vet" -> "Clinical Staff"     // legacy value
        "nurse" -> "Clinical Staff"             // legacy value
        else -> role?.replace('_', ' ')?.lowercase()
            ?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    }

    /** True if the role uses the staff side of the app. */
    fun isStaff(role: String?): Boolean =
        role in STAFF_ROLES || role in LEGACY_STAFF_ROLES || role == ADMIN

    /** True if the role may claim/advance treatment cases (veterinarian level). */
    fun isVetLevel(role: String?): Boolean =
        role in VET_LEVEL_ROLES || role == "branch_manager" // legacy value

    /** True if the role may manage staff accounts. */
    fun isStaffManager(role: String?): Boolean = role in STAFF_MANAGER_ROLES
}
