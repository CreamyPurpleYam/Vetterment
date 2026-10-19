package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

@Serializable
data class Medication(
    val id: String? = null,
    val case_id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val times_per_day: Int = 1,
    val scheduled_times: List<String>? = null,
    val start_date: String,
    val end_date: String? = null,
    val special_instructions: String? = null,
    val prescribed_by: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)
