package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

@Serializable
data class Pet(

    val id: String? = null,
    val owner_id: String,
    val created_by: String? = null,
    val name: String,
    val nickname: String? = null,
    val species: String,
    val breed: String? = null,
    val sex: String,
    val birth_date: String? = null,
    val weight: Double? = null,
    val known_allergies: String? = null,
    val pre_existing_conditions: String? = null,
    val photo_url: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null
)