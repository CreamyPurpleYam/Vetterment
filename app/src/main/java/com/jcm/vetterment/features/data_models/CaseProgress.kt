package com.jcm.vetterment.features.data_models

import kotlinx.serialization.Serializable

@Serializable
data class CaseProgress(
    val id: String? = null,
    val case_id: String,
    val status: String,
    val notes: String? = null,
    val recorded_by: String? = null,
    val recorded_at: String? = null
)