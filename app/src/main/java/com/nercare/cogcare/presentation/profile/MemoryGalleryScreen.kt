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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberRelation by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    
    LaunchedEffect(patientId) { viewModel.load(patientId) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddMemberDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
                Spacer(Modifier.width(8.dp))
                Text("Add Member")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CogCareBackground)
        ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Memory Gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Life stories & familiar faces",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
                    items(uiState.familyMembers.chunked(2), key = { "family-chunk-${it.hashCode()}" }) { rowMembers ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (member in rowMembers) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FamilyCard(member = member, onClick = { selectedMember = member })
                                }
                            }
                            if (rowMembers.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (uiState.memories.isNotEmpty()) {
                    items(uiState.memories, key = { "memory-${it.id}-${it.nodeKey}" }) { MemoryCard(it) }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (selectedMember != null) {
        AlertDialog(
            onDismissRequest = { 
                selectedMember = null
                isRecording = false
            },
            title = { Text(selectedMember?.fullName ?: "", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (!selectedMember?.mainPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = selectedMember?.mainPhotoUri,
                            contentDescription = selectedMember?.fullName,
                            modifier = Modifier.size(100.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    Text("Relation: ${selectedMember?.relation?.displayLabel}", style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { Toast.makeText(context, "Playing actual voice...", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Hear Actual Voice", color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isRecording) {
                        Button(
                            onClick = { 
                                isRecording = false
                                Toast.makeText(context, "Voice saved!", Toast.LENGTH_SHORT).show() 
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Mic, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop Recording", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { isRecording = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Mic, null, tint = PrimaryGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Record New Voice", color = PrimaryGreen)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    selectedMember = null
                    isRecording = false
                }) {
                    Text("Close", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Family Member", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMemberRelation,
                        onValueChange = { newMemberRelation = it },
                        label = { Text("Relation (e.g. Son)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { Toast.makeText(context, "Select photo from gallery...", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = PrimaryGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Photo", color = PrimaryGreen)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        Toast.makeText(context, "Member added!", Toast.LENGTH_SHORT).show()
                        showAddMemberDialog = false
                        newMemberName = ""
                        newMemberRelation = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Cancel", color = PrimaryGreen) }
            }
        )
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
            }
            Column(Modifier.weight(1f)) {
                Text(memory.questionPrompt, style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                Text(memory.value, style = MaterialTheme.typography.bodyLarge, color = TextSecondaryMuted)
            }
        }
    }
}

@Composable
private fun FamilyCard(member: FamilyMember, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!member.mainPhotoUri.isNullOrBlank()) {
                AsyncImage(model = member.mainPhotoUri, contentDescription = member.fullName, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = SecondaryGreen) {
                    Box(contentAlignment = Alignment.Center) { Text("👤", fontSize = 28.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(member.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(member.relation.displayLabel, style = MaterialTheme.typography.bodyMedium, color = TextSecondaryMuted, maxLines = 1)
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
