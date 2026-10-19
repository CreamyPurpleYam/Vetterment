package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: String? = null,
    val pet_id: String,
    val owner_id: String,
    val assigned_vet_id: String? = null,
    val appointment_type: String,
    val appointment_date: String,
    val appointment_time: String,
    val reason: String? = null,
    val status: String = "PENDING",
    val notes: String? = null,
    val confirmed_by: String? = null,
    val linked_case_id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
