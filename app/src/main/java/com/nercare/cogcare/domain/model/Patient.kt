package com.nercare.cogcare.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Patient(
    val id: String = "",
    val username: String = "",
    val name: String = "",
    val preferredName: String = "",
    val age: Int = 0,
    val dateOfBirth: String = "",
    val gender: String = "",
    val language: String = "en", // en, as, bn
    val city: String = "",
    val address: String = "",
    val bloodGroup: String = "",
    val allergies: String = "",
    val primaryDoctor: String = "",
    val doctorContact: String = "",
    val diagnosisStage: CognitiveStage = CognitiveStage.MILD,
    val diagnosisDetails: String = "",
    val medicalConditions: String = "",
    val medications: String = "",
    val independenceBaseline: String = "Unknown",
    val mobilityNeeds: String = "",
    val communicationNeeds: String = "",
    val dailyRoutine: String = "",
    val sleepPattern: String = "",
    val caregiverId: String = "",
    val caregiverRelationship: String = "",
    val caregiverContact: String = "",
    val emergencyContact: String = "",
    val hobbies: String = "",
    val favoriteMusic: String = "",
    val favoriteFoods: String = "",
    val favoriteActivities: String = "",
    val profession: String = "",
    val importantPeople: String = "",
    val importantPlaces: String = "",
    val importantEvents: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

enum class CognitiveStage(val label: String, val difficultyMultiplier: Float) {
    EARLY("Early Stage", 1.0f),
    MILD("Mild", 0.8f),
    MODERATE("Moderate", 0.6f),
    SEVERE("Severe", 0.4f)
}
