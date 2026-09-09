package com.nercare.cogcare.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.presentation.theme.*

@Composable
fun OnboardingScreen(caregiverId: String, onComplete: (String) -> Unit, onBack: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    var step by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf(PatientOnboardingData()) }
    val created by viewModel.created.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val usernameError by viewModel.usernameError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(created) {
        if (created != null) {
            showSuccessDialog = true
            snackbarHostState.showSnackbar("Patient created successfully!")
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    if (showSuccessDialog && created != null) {
        val creds = created!!
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Patient Created Successfully! 🎉") },
            text = {
                Column {
                    Text("Please note down the login credentials:")
                    Spacer(Modifier.height(12.dp))
                    Text("Username: ${creds.username}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Password: ${creds.password}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PrimaryGreen)
                    Spacer(Modifier.height(8.dp))
                    Text("The patient will use this username and password to log in.", style = MaterialTheme.typography.bodySmall, color = TextSecondaryMuted)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onComplete(creds.patientId)
                }) {
                    Text("Go to Dashboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("View Here")
                }
            }
        )
    }
    
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(Modifier.fillMaxSize().background(BackgroundCream).verticalScroll(rememberScrollState()).padding(paddingValues).padding(24.dp)) {
        Text("Add a patient", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Text("Step ${step + 1} of 4", color = TextSecondaryMuted)
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        when (step) {
            0 -> { SectionTitle("Basic details"); Field("Username", data.username, usernameError) { data = data.copy(username = it.lowercase().filter { c -> c.isLetterOrDigit() || c in "._-" }); viewModel.checkUsername(data.username) }; Field("Full name", data.name) { data = data.copy(name = it) }; Field("Preferred name", data.preferredName) { data = data.copy(preferredName = it) }; Field("Date of birth", data.dateOfBirth) { data = data.copy(dateOfBirth = it) }; Field("Age", data.age) { data = data.copy(age = it.filter(Char::isDigit).take(3)) }; Field("Gender", data.gender) { data = data.copy(gender = it) }; Field("City", data.city) { data = data.copy(city = it) }; Field("Full address", data.address) { data = data.copy(address = it) }; Field("Language (en/as/bn)", data.language) { data = data.copy(language = it) }; Field("Profile photo URL (optional)", data.profileImageUrl) { data = data.copy(profileImageUrl = it) } }
            1 -> { SectionTitle("Care and safety"); Field("Your relationship to the patient", data.caregiverRelationship) { data = data.copy(caregiverRelationship = it) }; Field("Caregiver contact", data.caregiverContact) { data = data.copy(caregiverContact = it) }; Field("Emergency contact", data.emergencyContact) { data = data.copy(emergencyContact = it) }; Field("Blood group", data.bloodGroup) { data = data.copy(bloodGroup = it) }; Field("Allergies", data.allergies) { data = data.copy(allergies = it) }; Field("Primary doctor", data.primaryDoctor) { data = data.copy(primaryDoctor = it) }; Field("Doctor contact", data.doctorContact) { data = data.copy(doctorContact = it) }; Text("Cognitive stage", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) { com.nercare.cogcare.domain.model.CognitiveStage.entries.forEach { stage -> FilterChip(selected = data.stage == stage, onClick = { data = data.copy(stage = stage) }, label = { Text(stage.label) }) } }; Field("Diagnosis details (optional)", data.diagnosisDetails) { data = data.copy(diagnosisDetails = it) }; Field("Medical conditions (optional / Unknown)", data.conditions) { data = data.copy(conditions = it) }; Field("Medications (optional / Unknown)", data.medications) { data = data.copy(medications = it) }; Field("Daily independence baseline", data.independence) { data = data.copy(independence = it) }; Field("Mobility or accessibility needs", data.mobilityNeeds) { data = data.copy(mobilityNeeds = it) }; Field("Communication needs", data.communicationNeeds) { data = data.copy(communicationNeeds = it) }; Field("Typical daily routine", data.dailyRoutine) { data = data.copy(dailyRoutine = it) }; Field("Sleep pattern", data.sleepPattern) { data = data.copy(sleepPattern = it) } }
            2 -> { SectionTitle("Personalisation"); Text("These details power relevant conversations, games and patient matching.", color = TextSecondaryMuted); Field("Hobbies", data.hobbies) { data = data.copy(hobbies = it) }; Field("Favourite music", data.music) { data = data.copy(music = it) }; Field("Favourite foods", data.foods) { data = data.copy(foods = it) }; Field("Favourite activities", data.activities) { data = data.copy(activities = it) }; Field("Profession", data.profession) { data = data.copy(profession = it) }; Field("Important people", data.people) { data = data.copy(people = it) }; Field("Important places", data.places) { data = data.copy(places = it) }; Field("Important events", data.events) { data = data.copy(events = it) } }
            else -> {
                SectionTitle("Patient access")
                val credentials = created
                if (credentials == null) {
                    Text("Choose the password the patient will use to sign in.")
                    OutlinedTextField(data.password, { data = data.copy(password = it) }, label = { Text("Patient password") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true, visualTransformation = PasswordVisualTransformation(), supportingText = { Text("At least 6 characters") })
                    Button(onClick = { viewModel.createPatient(caregiverId, data) }, enabled = data.name.isNotBlank() && data.username.isNotBlank() && usernameError == null && data.password.length >= 6 && caregiverId.isNotBlank() && !isLoading, modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp)) {
                        if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) else Text("Create patient")
                    }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = SecondaryGreen), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("Save these credentials", fontWeight = FontWeight.Bold); Text("Username: ${credentials.username}", style = MaterialTheme.typography.titleLarge); Text("Password: ${credentials.password}", style = MaterialTheme.typography.titleLarge); Text("The password is stored only as a secure hash and cannot be shown again.", color = TextSecondaryMuted) } }
                    Button(onClick = { onComplete(credentials.patientId) }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp)) { Text("Open caregiver dashboard") }
                }
            }
        }
        }
        if (step < 3) Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = { if (step == 0) onBack() else step-- }) { Text("Back") }; Button(onClick = { step++ }, enabled = step != 0 || (data.name.isNotBlank() && data.username.isNotBlank() && usernameError == null)) { Text("Next") } }
    }
}
@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp)) }
@Composable private fun Field(label: String, value: String, error: String? = null, update: (String) -> Unit) { OutlinedTextField(value, update, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), singleLine = true, isError = error != null, supportingText = error?.let { message -> { Text(message) } }) }
