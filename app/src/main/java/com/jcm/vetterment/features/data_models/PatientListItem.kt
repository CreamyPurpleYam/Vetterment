package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

@Serializable
data class PatientListItem(//for retrieving necessary data for displaying patient items

    val pet_id: String,
    val pet_name: String,
    val species: String,
    val breed: String?,
    val owner_id: String,
    val owner_name: String,
    val active_status: String,
    val photo_url: String?
)