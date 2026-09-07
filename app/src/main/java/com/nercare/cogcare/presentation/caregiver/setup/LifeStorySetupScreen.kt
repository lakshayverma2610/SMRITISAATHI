package com.nercare.cogcare.presentation.caregiver.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.presentation.theme.*

@Composable
fun LifeStorySetupScreen(patientId: String, onBack: () -> Unit, viewModel: LifeStorySetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var label by remember { mutableStateOf("") }; var value by remember { mutableStateOf("") }; var domain by remember { mutableStateOf("PERSONAL") }
    LaunchedEffect(patientId) { viewModel.load(patientId) }
    Column(Modifier.fillMaxSize().background(BackgroundCream).padding(24.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("What should Saathi know?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Text("Add useful context in your own words. Saathi generates patient questions dynamically; there is no fixed questionnaire.", color = TextSecondaryMuted)
        OutlinedTextField(label, { label = it }, label = { Text("Topic, e.g. favourite activity") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        OutlinedTextField(value, { value = it }, label = { Text("What you know") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2)
        OutlinedTextField(domain, { domain = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(onClick = { viewModel.save(label, value, domain); label = ""; value = "" }, enabled = value.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Text("Save context") }
        Text("Saved context", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.memories, key = { it.id }) { memory -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(memory.questionPrompt, fontWeight = FontWeight.Bold); Text(memory.value); Text(if (memory.source == com.nercare.cogcare.domain.model.MemorySource.CAREGIVER_ENTRY) "From caregiver" else "From patient", color = TextSecondaryMuted) } } } }
    }
}
