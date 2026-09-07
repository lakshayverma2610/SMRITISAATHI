package com.nercare.cogcare.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.LifeMemoryNode
import com.nercare.cogcare.presentation.theme.*

@Composable
fun MemoryGalleryScreen(
    patientId: String,
    onBack: () -> Unit,
    viewModel: MemoryGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(patientId) { viewModel.load(patientId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CogCareBackground)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(top = 16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
            }
            Text(
                "Memory Gallery",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimaryDark,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            uiState.memories.isEmpty() && uiState.familyMembers.isEmpty() -> EmptyGallery()
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.familyMembers.isNotEmpty()) {
                    item { SectionTitle("Family & friends") }
                    items(uiState.familyMembers, key = { "family-${it.id}" }) { FamilyCard(it) }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (uiState.memories.isNotEmpty()) {
                    item { SectionTitle("Life stories") }
                    items(uiState.memories, key = { "memory-${it.id}-${it.nodeKey}" }) { MemoryCard(it) }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyGallery() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = SecondaryGreen, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("Your memories will appear here soon.", style = MaterialTheme.typography.headlineSmall, color = PrimaryGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Answer a Saathi question or ask your caregiver to add details in the Life Story Vault.", style = MaterialTheme.typography.bodyLarge, color = TextSecondaryMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = PrimaryGreen, fontWeight = FontWeight.Bold)
}

@Composable
private fun MemoryCard(memory: LifeMemoryNode) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!memory.photoUri.isNullOrBlank()) {
                AsyncImage(model = memory.photoUri, contentDescription = memory.value, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            } else {
                Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = SecondaryGreen) {
                    Box(contentAlignment = Alignment.Center) { Text(domainEmoji(memory.domain), fontSize = 26.sp) }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(memory.questionPrompt, style = MaterialTheme.typography.labelLarge, color = TextSecondaryMuted)
                Spacer(Modifier.height(4.dp))
                Text(memory.value, style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FamilyCard(member: FamilyMember) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!member.mainPhotoUri.isNullOrBlank()) {
                AsyncImage(model = member.mainPhotoUri, contentDescription = member.fullName, modifier = Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = SecondaryGreen) {
                    Box(contentAlignment = Alignment.Center) { Text("👤", fontSize = 26.sp) }
                }
            }
            Column {
                Text(member.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                Text(member.relation.displayLabel, style = MaterialTheme.typography.bodyMedium, color = TextSecondaryMuted)
            }
        }
    }
}

private fun domainEmoji(domain: String): String = when (domain) {
    "CULINARY" -> "🍲"
    "CHILDHOOD" -> "🏡"
    "CAREER" -> "💼"
    "ROMANCE" -> "💛"
    "PARENTHOOD" -> "👪"
    "SPIRITUAL" -> "🙏"
    "NER_CULTURE" -> "🎶"
    "MEDICAL" -> "🩺"
    else -> "🌿"
}
