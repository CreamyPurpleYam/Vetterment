package com.jcm.vetterment.features.staff.cases

object CaseStages {
    // ── Spec-compliant timelines (claude.md §4) ──────────────────────────────
    // Confinement: Confined → Recovery → Discharge (Ready for Pick-up)
    val CONFINEMENT = listOf(
        "CONFINED", "RECOVERY", "DISCHARGED"
    )
    // Surgery: Surgery → Recovery → Discharge (Ready for Pick-up)
    val SURGERY = listOf(
        "SURGERY", "RECOVERY", "DISCHARGED"
    )
    // Emergency walk-ins keep a short internal queue before treatment.
    val EMERGENCY = listOf(
        "CALLED", "UNDER_TREATMENT", "DISCHARGED"
    )

    // ── Legacy timelines ─────────────────────────────────────────────────────
    // Consultations no longer create treatment cases (summaries go to Medical
    // Records), and the types below are no longer offered for new cases. These
    // lists are kept only so historical rows still render correctly.
    private val LEGACY_CONSULTATION = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "CONSULTATION", "COMPLETED"
    )
    private val LEGACY_VACCINATION = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "VACCINATION", "OBSERVATION", "COMPLETED"
    )
    private val LEGACY_SURGERY = listOf(
        "QUEUED", "CALLED",
        "PRE_OP", "SURGERY", "RECOVERY", "DISCHARGED"
    )
    private val LEGACY_CONFINEMENT = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "ADMISSION", "CONFINED", "TREATMENT", "RECOVERY", "DISCHARGED"
    )
    private val LEGACY_GROOMING = listOf(
        "QUEUED", "CALLED", "INITIAL_CHECK",
        "BATHING", "DRYING", "STYLING", "FINISHING", "COMPLETED"
    )
    private val LEGACY_TREATMENT = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "UNDER_TREATMENT", "COMPLETED"
    )

    /** Case types that can still be created (walk-ins). */
    val CREATABLE_CASE_TYPES = listOf("CONFINEMENT", "SURGERY", "EMERGENCY")

    /**
     * Stage list for rendering a case. For confinement/surgery cases created
     * after the spec alignment this returns the 3-stage timeline; rows created
     * earlier (still at legacy stages like QUEUED or ADMISSION) are detected by
     * their current status and rendered with their original timeline.
     */
    fun stagesFor(caseType: String, currentStatus: String? = null): List<String> {
        // Legacy rows: statuses that only exist in the old timelines resolve to
        // that legacy list so historical cases keep rendering (and advancing) correctly.
        if (currentStatus != null) {
            when {
                currentStatus == "ADMISSION" || currentStatus == "TREATMENT" -> return LEGACY_CONFINEMENT
                currentStatus == "PRE_OP" -> return LEGACY_SURGERY
                currentStatus in LEGACY_CONSULTATION -> return legacyListFor(caseType) ?: legacyListFor(currentTypeFor(currentStatus)) ?: LEGACY_CONSULTATION
            }
        }
        return when (caseType) {
            "CONFINEMENT" -> CONFINEMENT
            "SURGERY"     -> SURGERY
            "EMERGENCY"   -> EMERGENCY
            "CONSULTATION" -> CONSULTATION
            "VACCINATION"  -> VACCINATION
            "GROOMING"     -> GROOMING
            "TREATMENT"    -> TREATMENT
            else           -> CONSULTATION
        }
    }

    private fun currentTypeFor(status: String): String = when (status) {
        "VACCINATION", "OBSERVATION" -> "VACCINATION"
        "BATHING", "DRYING", "STYLING", "FINISHING", "INITIAL_CHECK" -> "GROOMING"
        else -> "CONSULTATION"
    }

    // Kept for legacy rendering support
    val CONSULTATION get() = LEGACY_CONSULTATION
    val VACCINATION get() = LEGACY_VACCINATION
    val GROOMING get() = LEGACY_GROOMING
    val TREATMENT get() = LEGACY_TREATMENT

    private fun legacyListFor(caseType: String): List<String>? = when (caseType) {
        "VACCINATION"  -> LEGACY_VACCINATION
        "GROOMING"     -> LEGACY_GROOMING
        "TREATMENT"    -> LEGACY_TREATMENT
        "CONSULTATION" -> LEGACY_CONSULTATION
        else           -> null
    }

    fun displayName(status: String): String = when (status) {
        "QUEUED"            -> "Queued"
        "CALLED"            -> "Called"
        "UNDER_EXAMINATION" -> "Examination"
        "CONSULTATION"      -> "Consultation"
        "VACCINATION"       -> "Vaccination"
        "OBSERVATION"       -> "Observation"
        "PRE_OP"            -> "Pre-op"
        "SURGERY"           -> "Surgery"
        "RECOVERY"          -> "Recovery"
        "ADMISSION"         -> "Admission"
        "CONFINED"          -> "Confined"
        "TREATMENT"         -> "Treatment"
        "UNDER_TREATMENT"   -> "Treatment"
        "INITIAL_CHECK"     -> "Initial Check"
        "BATHING"           -> "Bathing"
        "DRYING"            -> "Drying"
        "STYLING"           -> "Styling"
        "FINISHING"         -> "Finishing"
        "DISCHARGED"        -> "Discharge (Ready for Pick-up)"
        "COMPLETED"         -> "Completed"
        else                -> status
    }
}
