package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String,
    val email: String,
    val role: String,
    val created_at: String? = null
)