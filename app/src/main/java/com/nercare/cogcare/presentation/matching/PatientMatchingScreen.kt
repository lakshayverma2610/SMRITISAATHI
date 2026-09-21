package com.nercare.cogcare.presentation.matching

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.VoiceChat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.PatientMatchingRepository
import com.nercare.cogcare.domain.model.PatientMatch
import com.nercare.cogcare.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchingState(
    val loading: Boolean = true, 
    val matches: List<PatientMatch> = emptyList()
)

@HiltViewModel
class PatientMatchingViewModel @Inject constructor(
    private val repository: PatientMatchingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MatchingState())
    val state = _state.asStateFlow()
    
    fun load(patientId: String) {
        _state.value = MatchingState(loading = true)
        viewModelScope.launch { 
            val realTimeMatches = repository.findMatches(patientId)
            _state.value = MatchingState(matches = realTimeMatches, loading = false) 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientMatchingScreen(
    patientId: String, 
    onBack: () -> Unit, 
    viewModel: PatientMatchingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(patientId) { 
        viewModel.load(patientId) 
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Connect", fontWeight = FontWeight.Bold, color = PrimaryGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryGreen) }
                },
                actions = {
                    IconButton(onClick = { viewModel.load(patientId) }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCream)
            )
        },
        containerColor = BackgroundCream
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Find people like you", 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Bold, 
                color = TextPrimary
            )
            Text(
                "We use your Life Story Vault to safely match you with nearby peers who share your interests.", 
                color = TextSecondaryMuted, 
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RadarAnimation()
                }
            } else if (state.matches.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = SecondaryGreen, modifier = Modifier.size(80.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("🔍", fontSize = 40.sp) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Scanning community...", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold, 
                            color = PrimaryGreen
                        )
                        Text(
                            "We couldn't find anyone with your exact hobbies nearby right now. Make sure your Life Story Vault is fully filled out!", 
                            textAlign = TextAlign.Center, 
                            color = TextSecondaryMuted, 
                            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.matches, key = { it.patientId }) { match ->
                        MatchCard(match = match)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(match: PatientMatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGreen)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape, 
                    color = PrimaryGreen.copy(alpha = 0.1f), 
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(match.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Mutual interests: ${match.sharedInterests.size}", style = MaterialTheme.typography.bodyMedium, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Interest Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                match.sharedInterests.take(3).forEach { interest ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SecondaryGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            interest.replaceFirstChar { it.uppercase() }, 
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                            style = MaterialTheme.typography.labelMedium, 
                            color = PrimaryGreen, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (match.sharedInterests.size > 3) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CogCareSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            "+${match.sharedInterests.size - 3}", 
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                            style = MaterialTheme.typography.labelMedium, 
                            color = TextSecondaryMuted
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = CogCareSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            
            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { /* Demo Action */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.VoiceChat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Voice Note", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { /* Demo Action */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Call", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(PrimaryGreen.copy(alpha = alpha))
        )
        Surface(shape = CircleShape, color = PrimaryGreen, modifier = Modifier.size(60.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("📡", fontSize = 28.sp) }
        }
    }
}
