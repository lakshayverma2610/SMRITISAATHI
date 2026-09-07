package com.nercare.cogcare.domain.model

/**
 * Represents a single generative question asked to the patient in the current session.
 */
data class CompanionQuestion(
    val patientId: String,
    val domain: String,
    val nodeKey: String,
    val questionPrompt: String,
    /** If non-null, the companion prefaces with the caregiver's hypothesis for patient to confirm. */
    val caregiverHypothesis: String? = null
)
