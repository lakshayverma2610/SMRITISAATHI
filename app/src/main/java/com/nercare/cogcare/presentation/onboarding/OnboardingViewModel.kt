package com.nercare.cogcare.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.data.repository.PatientRepository
import com.nercare.cogcare.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientOnboardingData(
    val username: String = "", val password: String = "", val name: String = "", val preferredName: String = "", val age: String = "", val dateOfBirth: String = "",
    val gender: String = "", val language: String = "en", val city: String = "", val address: String = "", val bloodGroup: String = "", val allergies: String = "", val primaryDoctor: String = "", val doctorContact: String = "", val profileImageUrl: String = "",
    val caregiverRelationship: String = "", val caregiverContact: String = "", val emergencyContact: String = "",
    val diagnosisDetails: String = "", val conditions: String = "", val medications: String = "", val independence: String = "Unknown", val mobilityNeeds: String = "", val communicationNeeds: String = "", val dailyRoutine: String = "", val sleepPattern: String = "",
    val hobbies: String = "", val music: String = "", val foods: String = "", val activities: String = "", val profession: String = "",
    val people: String = "", val places: String = "", val events: String = "", val stage: CognitiveStage = CognitiveStage.MILD
)
data class CreatedPatientCredentials(val patientId: String, val username: String, val password: String)

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val patients: PatientRepository, private val lifeStory: LifeStoryRepository) : ViewModel() {
    private val _created = MutableStateFlow<CreatedPatientCredentials?>(null)
    val created = _created.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError = _usernameError.asStateFlow()

    private var usernameCheckJob: kotlinx.coroutines.Job? = null

    fun checkUsername(username: String) {
        usernameCheckJob?.cancel()
        val normalized = username.trim().lowercase()
        _usernameError.value = when {
            normalized.isBlank() -> "Username is required"
            !normalized.matches(Regex("[a-z0-9._-]{3,30}")) -> "Use 3–30 letters, numbers, dots, underscores or hyphens"
            else -> null
        }
        if (_usernameError.value != null) return
        usernameCheckJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            _usernameError.value = try {
                if (patients.isUsernameAvailable(normalized)) null else "Username is already taken"
            } catch (e: Exception) {
                android.util.Log.e("Onboarding", "Username check failed: ${e.message}", e)
                "Could not verify username. Check your connection."
            }
        }
    }

    fun createPatient(caregiverId: String, data: PatientOnboardingData) = viewModelScope.launch {
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val generatedUsername = data.username.trim().lowercase()
            if (!patients.isUsernameAvailable(generatedUsername)) error("Username is already taken")
            val patient = Patient(
                username = generatedUsername,
                name = data.name.trim(), preferredName = data.preferredName.trim(), age = data.age.toIntOrNull() ?: 0,
                dateOfBirth = data.dateOfBirth, gender = data.gender, language = data.language, city = data.city,
                address = data.address, bloodGroup = data.bloodGroup, allergies = data.allergies,
                primaryDoctor = data.primaryDoctor, doctorContact = data.doctorContact,
                profileImageUrl = data.profileImageUrl,
                caregiverId = caregiverId, caregiverRelationship = data.caregiverRelationship,
                caregiverContact = data.caregiverContact, emergencyContact = data.emergencyContact,
                diagnosisStage = data.stage, diagnosisDetails = data.diagnosisDetails, medicalConditions = data.conditions,
                medications = data.medications, independenceBaseline = data.independence, hobbies = data.hobbies,
                mobilityNeeds = data.mobilityNeeds, communicationNeeds = data.communicationNeeds,
                dailyRoutine = data.dailyRoutine, sleepPattern = data.sleepPattern,
                favoriteMusic = data.music, favoriteFoods = data.foods, favoriteActivities = data.activities,
                profession = data.profession, importantPeople = data.people, importantPlaces = data.places, importantEvents = data.events
            )
            val (saved, password) = patients.savePatientWithCredentials(patient, data.password)
            val nodes = listOf(
                Triple("PERSONAL", "hobbies", data.hobbies), Triple("CULINARY", "favorite_foods", data.foods),
                Triple("SPIRITUAL", "favorite_music", data.music), Triple("CAREER", "profession", data.profession),
                Triple("EMOTIONAL_ANCHORS", "important_people", data.people), Triple("CHILDHOOD", "important_places", data.places),
                Triple("PERSONAL", "favorite_activities", data.activities), Triple("PERSONAL", "important_events", data.events)
            ).filter { it.third.isNotBlank() }.map { (domain, key, value) ->
                LifeMemoryNode(patientId = saved.id, domain = domain, nodeKey = key, questionPrompt = "Caregiver onboarding: ${key.replace('_', ' ')}", value = value, source = MemorySource.CAREGIVER_ENTRY)
            }
            if (nodes.isNotEmpty()) lifeStory.saveCaregiverOnboardingNodes(nodes)
            _created.value = CreatedPatientCredentials(saved.id, saved.username, password)
        } catch (e: Exception) {
            android.util.Log.e("OnboardingViewModel", "Failed to create patient: ${e.message}", e)
            _errorMessage.value = e.localizedMessage ?: "Failed to create patient"
        } finally {
            _isLoading.value = false
        }
    }
}
