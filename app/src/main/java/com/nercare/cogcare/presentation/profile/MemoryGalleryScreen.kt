package com.nercare.cogcare.presentation.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.LifeMemoryNode
import com.nercare.cogcare.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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
    var newMemberPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> newMemberPhotoUri = uri }
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val member = selectedMember
            if (granted && member != null) viewModel.startRecording(member)
        }
    )

    LaunchedEffect(patientId) { viewModel.load(patientId) }

    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddMemberDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
                Spacer(Modifier.width(8.dp))
                Text("Add Member", color = Color.White)
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
                    Text("Memory Gallery", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Life stories & familiar faces", style = MaterialTheme.typography.titleMedium, color = TextSecondaryMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
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
                        items(uiState.familyMembers.chunked(2), key = { "chunk-${it.map { m -> m.id }}" }) { rowMembers ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (member in rowMembers) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        FamilyCard(
                                            member = member,
                                            isPlaying = uiState.playingMemberId == member.id,
                                            playbackState = uiState.voicePlaybackState,
                                            onClick = { selectedMember = member }
                                        )
                                    }
                                }
                                if (rowMembers.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                    if (uiState.memories.isNotEmpty()) {
                        items(uiState.memories, key = { "memory-${it.id}-${it.nodeKey}" }) { MemoryCard(it) }
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }

    // ── Member Detail Dialog ──────────────────────────────────────────────────
    val member = selectedMember
    if (member != null) {
        val isThisPlaying = uiState.playingMemberId == member.id
        val isThisRecording = uiState.recordingMemberId == member.id && uiState.recordingState == RecordingState.RECORDING
        val playbackState = uiState.voicePlaybackState

        AlertDialog(
            onDismissRequest = {
                viewModel.stopPlayback()
                selectedMember = null
            },
            title = { Text(member.fullName, color = PrimaryGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // Photo
                    if (!member.mainPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = member.mainPhotoUri,
                            contentDescription = member.fullName,
                            modifier = Modifier.size(100.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("Relation: ${member.relation.displayLabel}", style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark)
                    if (!member.favouriteSharedMemory.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("\"${member.favouriteSharedMemory}\"", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryMuted, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))

                    // Hear Voice Button — real state-driven
                    Button(
                        onClick = { viewModel.toggleVoice(member) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = playbackState != VoicePlaybackState.LOADING || isThisPlaying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isThisPlaying) Color(0xFF388E3C) else PrimaryGreen,
                            contentColor = Color.White
                        )
                    ) {
                        when {
                            isThisPlaying && playbackState == VoicePlaybackState.LOADING -> {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading...", color = Color.White)
                            }
                            isThisPlaying -> {
                                Icon(Icons.Default.Stop, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Stop", color = Color.White)
                            }
                            else -> {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Hear Voice", color = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Record Voice Button — real MediaRecorder
                    if (isThisRecording) {
                        Button(
                            onClick = { viewModel.stopRecording(member) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Stop, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop Recording", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startRecording(member)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Mic, null, tint = PrimaryGreen)
                            Spacer(Modifier.width(8.dp))
                            val hasVoice = !member.voiceNoteUri.isNullOrBlank()
                            Text(if (hasVoice) "Re-Record Voice" else "Record Voice", color = PrimaryGreen)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopPlayback()
                    selectedMember = null
                }) {
                    Text("Close", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Add Member Dialog ─────────────────────────────────────────────────────
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Family Member", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (newMemberPhotoUri != null) {
                        AsyncImage(
                            model = newMemberPhotoUri,
                            contentDescription = "Selected Photo",
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = newMemberName.isBlank()
                    )
                    Spacer(Modifier.height(8.dp))
                    var relationExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = relationExpanded,
                        onExpandedChange = { relationExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newMemberRelation.ifBlank { "Select Relation" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Relation") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = relationExpanded, onDismissRequest = { relationExpanded = false }) {
                            com.nercare.cogcare.domain.model.FamilyRelation.entries.forEach { rel ->
                                DropdownMenuItem(
                                    text = { Text(rel.displayLabel) },
                                    onClick = { newMemberRelation = rel.displayLabel; relationExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = PrimaryGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(if (newMemberPhotoUri == null) "Add Photo" else "Change Photo", color = PrimaryGreen)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMemberName.isNotBlank()) {
                            viewModel.addFamilyMember(fullName = newMemberName, relationLabel = newMemberRelation, photoUri = newMemberPhotoUri?.toString())
                            showAddMemberDialog = false; newMemberName = ""; newMemberRelation = ""; newMemberPhotoUri = null
                        }
                    },
                    enabled = newMemberName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Cancel", color = PrimaryGreen) }
            }
        )
    }
}

@Composable
private fun FamilyCard(
    member: FamilyMember,
    isPlaying: Boolean,
    playbackState: VoicePlaybackState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPlaying) PrimaryGreen.copy(alpha = 0.12f) else CogCareSurfaceVariant),
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen) else null
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
            Spacer(Modifier.height(8.dp))
            Text(member.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(member.relation.displayLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondaryMuted, maxLines = 1)
            if (isPlaying) {
                Spacer(Modifier.height(4.dp))
                if (playbackState == VoicePlaybackState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PrimaryGreen, strokeWidth = 2.dp)
                } else {
                    Text("Playing ▶", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            } else if (!member.voiceNoteUri.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("🎙 Voice saved", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
            }
        }
    }
}

@Composable
private fun EmptyGallery() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = SecondaryGreen, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("Your memories will appear here soon.", style = MaterialTheme.typography.headlineSmall, color = PrimaryGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Add a family member using the + button, or answer a Saathi question.", style = MaterialTheme.typography.bodyLarge, color = TextSecondaryMuted, textAlign = TextAlign.Center)
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
