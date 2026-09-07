package com.nercare.cogcare.presentation.matching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.PatientMatchingRepository
import com.nercare.cogcare.domain.model.PatientMatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
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

data class MatchingState(val loading: Boolean = true, val matches: List<PatientMatch> = emptyList())
@HiltViewModel
class PatientMatchingViewModel @Inject constructor(private val repository: PatientMatchingRepository) : ViewModel() {
    private val _state = MutableStateFlow(MatchingState())
    val state = _state.asStateFlow()
    fun load(patientId: String) = viewModelScope.launch { _state.value = MatchingState(matches = repository.findMatches(patientId), loading = false) }
}

@Composable
fun PatientMatchingScreen(patientId: String, onBack: () -> Unit, viewModel: PatientMatchingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(patientId) { viewModel.load(patientId) }
    Column(Modifier.fillMaxSize().background(BackgroundCream).padding(24.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("People like you", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Text("Suggested from interests you have both shared.", color = TextSecondaryMuted, modifier = Modifier.padding(bottom = 16.dp))
        if (state.loading) CircularProgressIndicator()
        else if (state.matches.isEmpty()) Text("No matches yet. More people will appear as shared interests are discovered.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.matches, key = { it.patientId }) { match ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text(match.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("You both enjoy: ${match.sharedInterests.joinToString()}", color = PrimaryGreen) } }
            }
        }
    }
}
