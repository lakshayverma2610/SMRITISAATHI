package com.nercare.cogcare.domain.model

data class SocialProfile(
    val patientId: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val interests: List<String> = emptyList()
)

data class PatientMatch(
    val patientId: String,
    val displayName: String,
    val profileImageUrl: String,
    val sharedInterests: List<String>
)
