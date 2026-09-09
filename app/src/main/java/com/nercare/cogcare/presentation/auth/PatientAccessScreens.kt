package com.nercare.cogcare.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.CaregiverAuthRepository
import com.nercare.cogcare.data.repository.PatientRepository
import com.nercare.cogcare.data.repository.SessionRepository
import com.nercare.cogcare.domain.model.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.presentation.theme.*

@HiltViewModel
class PatientSelectionViewModel @Inject constructor(private val auth: CaregiverAuthRepository, private val session: SessionRepository, repository: PatientRepository) : ViewModel() {
    val caregiverId = auth.currentCaregiverId.orEmpty()
    val patients = (if (caregiverId.isBlank()) flowOf(emptyList()) else repository.getPatientsForCaregiver(caregiverId))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun signOut() {
        auth.signOut()
        viewModelScope.launch { session.clear() }
    }
    init { if (caregiverId.isNotBlank()) viewModelScope.launch { repository.refreshPatientsForCaregiver(caregiverId) } }
}

@Composable
fun PatientSelectionScreen(onPatient: (String) -> Unit, onAdd: (String) -> Unit, onSignedOut: () -> Unit, viewModel: PatientSelectionViewModel = hiltViewModel()) {
    val patients by viewModel.patients.collectAsState()
    Column(Modifier.fillMaxSize().background(BackgroundCream).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Your patients", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            TextButton(onClick = { viewModel.signOut(); onSignedOut() }) { Text("Sign out") }
        }
        Button(onClick = { onAdd(viewModel.caregiverId) }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Add patient") }
        Spacer(Modifier.height(18.dp))
        if (patients.isEmpty()) Text("No patients yet. Add the first patient to begin.", color = TextSecondaryMuted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(patients, key = { it.id }) { patient ->
                Card(onClick = { onPatient(patient.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(patient.preferredName.ifBlank { patient.name }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Patient ID: ${patient.id}", color = TextSecondaryMuted)
                    }
                }
            }
        }
    }
}

data class PatientLoginState(val loading: Boolean = false, val error: String? = null, val patient: Patient? = null)
@HiltViewModel
class PatientLoginViewModel @Inject constructor(private val repository: PatientRepository, private val session: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(PatientLoginState())
    val state = _state.asStateFlow()
    fun login(username: String, password: String) = viewModelScope.launch {
        _state.value = PatientLoginState(loading = true)
        val patient = repository.authenticatePatient(username, password)
        _state.value = if (patient == null) PatientLoginState(error = "Username or password is incorrect") else {
            session.savePatientSession(patient.id)
            PatientLoginState(patient = patient)
        }
    }
}

@Composable
fun PatientLoginScreen(onSuccess: (String) -> Unit, onBack: () -> Unit, viewModel: PatientLoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state.patient) { state.patient?.let { onSuccess(it.id) } }
    Column(Modifier.fillMaxSize().background(BackgroundCream).padding(28.dp), verticalArrangement = Arrangement.Center) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Patient sign in", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Text("Use the Username and password provided by your caregiver.", color = TextSecondaryMuted, modifier = Modifier.padding(vertical = 14.dp))
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Button(onClick = { viewModel.login(username, password) }, enabled = username.isNotBlank() && password.isNotBlank() && !state.loading, modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp)) { Text("Open my dashboard") }
    }
}
