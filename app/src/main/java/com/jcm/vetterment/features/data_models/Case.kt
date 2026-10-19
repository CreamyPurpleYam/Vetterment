package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

@Serializable
data class Case(
    val id: String? = null,
    val pet_id: String,
    val case_type: String,
    val current_status: String = "QUEUED",
    val appointment_id: String? = null,       // appointment that created this case (if any)
    val previous_case_id: String? = null,     // direct predecessor in a follow-up chain
    val root_case_id: String? = null,         // original case that started the chain
    val assigned_vet_id: String? = null,
    val assigned_nurse_id: String? = null,
    val assigned_assistant_id: String? = null,
    val diagnosis: String? = null,
    val treatment_plan: String? = null,
    val medication_instructions: String? = null,
    val notes: String? = null,
    val discharge_summary: String? = null,
    val admission_date: String? = null,
    val expected_discharge_date: String? = null,
    val actual_discharge_date: String? = null,
    val created_by: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)