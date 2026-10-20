package com.jcm.vetterment.features.staff.cases

object CaseStages {
    val CONSULTATION = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "CONSULTATION", "COMPLETED"
    )
    val VACCINATION = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "VACCINATION", "OBSERVATION", "COMPLETED"
    )
    val SURGERY = listOf(
        "QUEUED", "CALLED",
        "PRE_OP", "SURGERY", "RECOVERY", "DISCHARGED"
    )
    val CONFINEMENT = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "ADMISSION", "CONFINED", "TREATMENT", "RECOVERY", "DISCHARGED"
    )
    val GROOMING = listOf(
        "QUEUED", "CALLED", "INITIAL_CHECK",
        "BATHING", "DRYING", "STYLING", "FINISHING", "COMPLETED"
    )
    val TREATMENT = listOf(
        "QUEUED", "CALLED", "UNDER_EXAMINATION",
        "UNDER_TREATMENT", "COMPLETED"
    )
    val EMERGENCY = listOf(
        "CALLED", "UNDER_EXAMINATION",
        "UNDER_TREATMENT", "DISCHARGED"
    )

    fun stagesFor(caseType: String): List<String> = when (caseType) {
        "CONSULTATION" -> CONSULTATION
        "VACCINATION"  -> VACCINATION
        "SURGERY"      -> SURGERY
        "CONFINEMENT"  -> CONFINEMENT
        "GROOMING"     -> GROOMING
        "TREATMENT"    -> TREATMENT
        "EMERGENCY"    -> EMERGENCY
        else           -> CONSULTATION
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
        "DISCHARGED"        -> "Discharged"
        "COMPLETED"         -> "Completed"
        else                -> status
    }
}