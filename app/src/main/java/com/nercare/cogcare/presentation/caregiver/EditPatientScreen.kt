package com.nercare.cogcare.presentation.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.PatientRepository
import com.nercare.cogcare.domain.model.CognitiveStage
import com.nercare.cogcare.domain.model.Patient
import com.nercare.cogcare.presentation.theme.BackgroundCream
import com.nercare.cogcare.presentation.theme.PrimaryGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditPatientUiState(
    val patient: Patient? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class EditPatientViewModel @Inject constructor(private val repository: PatientRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditPatientUiState())
    val state = _state.asStateFlow()

    fun load(patientId: String) = viewModelScope.launch {
        _state.value = EditPatientUiState(patient = repository.getPatientById(patientId), loading = false)
    }

    fun update(transform: (Patient) -> Patient) {
        _state.value.patient?.let { _state.value = _state.value.copy(patient = transform(it), error = null) }
    }

    fun save() = viewModelScope.launch {
        val patient = _state.value.patient ?: return@launch
        if (patient.name.isBlank()) {
            _state.value = _state.value.copy(error = "Full name is required")
            return@launch
        }
        _state.value = _state.value.copy(saving = true, error = null)
        runCatching { repository.savePatient(patient) }
            .onSuccess { _state.value = _state.value.copy(patient = it, saving = false, saved = true) }
            .onFailure { _state.value = _state.value.copy(saving = false, error = it.localizedMessage ?: "Could not save patient") }
    }
}

@Composable
fun EditPatientScreen(
    patientId: String,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditPatientViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(patientId) { viewModel.load(patientId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Column(Modifier.fillMaxSize().background(BackgroundCream).verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Edit patient information", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Spacer(Modifier.height(12.dp))
        val patient = state.patient
        if (state.loading) {
            CircularProgressIndicator()
        } else if (patient == null) {
            Text("Patient could not be found", color = MaterialTheme.colorScheme.error)
        } else {
            EditField("Full name", patient.name) { viewModel.update { p -> p.copy(name = it) } }
            EditField("Preferred name", patient.preferredName) { viewModel.update { p -> p.copy(preferredName = it) } }
            EditField("Date of birth", patient.dateOfBirth) { viewModel.update { p -> p.copy(dateOfBirth = it) } }
            EditField("Age", patient.age.takeIf { it > 0 }?.toString().orEmpty()) { value -> viewModel.update { p -> p.copy(age = value.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) } }
            EditField("Gender", patient.gender) { viewModel.update { p -> p.copy(gender = it) } }
            EditField("City", patient.city) { viewModel.update { p -> p.copy(city = it) } }
            EditField("Full address", patient.address) { viewModel.update { p -> p.copy(address = it) } }
            EditField("Language (en/as/bn)", patient.language) { viewModel.update { p -> p.copy(language = it) } }

            Text("Care and medical information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CognitiveStage.entries.forEach { stage ->
                    FilterChip(selected = patient.diagnosisStage == stage, onClick = { viewModel.update { it.copy(diagnosisStage = stage) } }, label = { Text(stage.label) })
                }
            }
            EditField("Diagnosis details", patient.diagnosisDetails) { viewModel.update { p -> p.copy(diagnosisDetails = it) } }
            EditField("Medical conditions", patient.medicalConditions) { viewModel.update { p -> p.copy(medicalConditions = it) } }
            EditField("Medications", patient.medications) { viewModel.update { p -> p.copy(medications = it) } }
            EditField("Blood group", patient.bloodGroup) { viewModel.update { p -> p.copy(bloodGroup = it) } }
            EditField("Allergies", patient.allergies) { viewModel.update { p -> p.copy(allergies = it) } }
            EditField("Primary doctor", patient.primaryDoctor) { viewModel.update { p -> p.copy(primaryDoctor = it) } }
            EditField("Doctor contact", patient.doctorContact) { viewModel.update { p -> p.copy(doctorContact = it) } }
            EditField("Emergency contact", patient.emergencyContact) { viewModel.update { p -> p.copy(emergencyContact = it) } }
            EditField("Independence baseline", patient.independenceBaseline) { viewModel.update { p -> p.copy(independenceBaseline = it) } }
            EditField("Mobility or accessibility needs", patient.mobilityNeeds) { viewModel.update { p -> p.copy(mobilityNeeds = it) } }
            EditField("Communication needs", patient.communicationNeeds) { viewModel.update { p -> p.copy(communicationNeeds = it) } }
            EditField("Typical daily routine", patient.dailyRoutine) { viewModel.update { p -> p.copy(dailyRoutine = it) } }
            EditField("Sleep pattern", patient.sleepPattern) { viewModel.update { p -> p.copy(sleepPattern = it) } }

            Text("Personal details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
            EditField("Hobbies", patient.hobbies) { viewModel.update { p -> p.copy(hobbies = it) } }
            EditField("Favourite music", patient.favoriteMusic) { viewModel.update { p -> p.copy(favoriteMusic = it) } }
            EditField("Favourite foods", patient.favoriteFoods) { viewModel.update { p -> p.copy(favoriteFoods = it) } }
            EditField("Favourite activities", patient.favoriteActivities) { viewModel.update { p -> p.copy(favoriteActivities = it) } }
            EditField("Profession", patient.profession) { viewModel.update { p -> p.copy(profession = it) } }
            EditField("Important people", patient.importantPeople) { viewModel.update { p -> p.copy(importantPeople = it) } }
            EditField("Important places", patient.importantPlaces) { viewModel.update { p -> p.copy(importantPlaces = it) } }
            EditField("Important events", patient.importantEvents) { viewModel.update { p -> p.copy(importantEvents = it) } }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
            Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).height(54.dp)) {
                if (state.saving) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Save changes")
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
}
