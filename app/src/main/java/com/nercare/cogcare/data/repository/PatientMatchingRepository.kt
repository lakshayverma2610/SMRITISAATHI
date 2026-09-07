package com.nercare.cogcare.data.repository

import com.nercare.cogcare.data.remote.FirebaseService
import com.nercare.cogcare.domain.model.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientMatchingRepository @Inject constructor(
    private val patients: PatientRepository,
    private val lifeStory: LifeStoryRepository,
    private val firebase: FirebaseService
) {
    suspend fun findMatches(patientId: String): List<PatientMatch> {
        val patient = patients.getPatientById(patientId) ?: return emptyList()
        val memories = lifeStory.observeAllMemoryNodes(patientId).first()
        val own = SocialProfile(patient.id, patient.preferredName.ifBlank { patient.name }, patient.profileImageUrl, deriveInterests(patient, memories))
        firebase.syncSocialProfile(own)
        val remote = firebase.fetchSocialProfiles()
        val local = patients.getAllPatientsSync().map { other ->
            SocialProfile(other.id, other.preferredName.ifBlank { other.name }, other.profileImageUrl, deriveInterests(other, emptyList()))
        }
        return (remote + local).distinctBy { it.patientId }.asSequence()
            .filter { it.patientId != patientId }
            .map { profile -> PatientMatch(profile.patientId, profile.displayName, profile.profileImageUrl, own.interests.intersect(profile.interests.toSet()).sorted()) }
            .filter { it.sharedInterests.isNotEmpty() }
            .sortedByDescending { it.sharedInterests.size }
            .toList()
    }

    private fun deriveInterests(patient: Patient, memories: List<LifeMemoryNode>): List<String> {
        val sources = listOf(patient.hobbies, patient.favoriteMusic, patient.favoriteFoods, patient.favoriteActivities, patient.profession) +
            memories.filterNot { it.domain.equals("MEDICAL", true) }.map { it.value }
        return sources.flatMap { value -> value.split(',', ';', '/', '\n') }
            .map { it.lowercase().trim().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ") }
            .filter { it.length >= 3 }.distinct().take(30)
    }
}
