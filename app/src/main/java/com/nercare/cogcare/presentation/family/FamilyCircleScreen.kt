package com.nercare.cogcare.presentation.family

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.FamilyRelation
import com.nercare.cogcare.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyCircleScreen(
    patientId: String,
    onBack: () -> Unit,
    viewModel: FamilyCircleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var memberToDelete by remember { mutableStateOf<FamilyMember?>(null) }

    LaunchedEffect(patientId) {
        viewModel.load(patientId)
    }

    if (showAddDialog) {
        AddFamilyMemberDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, relation, phone, memory ->
                viewModel.addFamilyMember(
                    patientId = patientId,
                    fullName = name,
                    relation = relation,
                    phone = phone,
                    favouriteMemory = memory
                )
                showAddDialog = false
            }
        )
    }

    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Remove Loved One") },
            text = { Text("Remove ${memberToDelete?.fullName} from your circle?") },
            confirmButton = {
                TextButton(onClick = {
                    memberToDelete?.let { viewModel.deleteFamilyMember(it.id) }
                    memberToDelete = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Loved One") },
                containerColor = PrimaryGreen,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
                .padding(paddingValues)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Family & Loved Ones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        "Familiar voices and loving faces",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else if (uiState.members.isEmpty()) {
                EmptyFamilyCircleView(onAddClick = { showAddDialog = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.members, key = { it.id }) { member ->
                        val isPlaying = uiState.playingMemberId == member.id
                        FamilyMemberCard(
                            member = member,
                            isPlaying = isPlaying,
                            onPlayVoice = { viewModel.toggleVoiceNote(member) },
                            onCall = {
                                member.primaryPhone?.let { phone ->
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                }
                            },
                            onDelete = { memberToDelete = member }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMember,
    isPlaying: Boolean,
    onPlayVoice: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo or Avatar
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(SecondaryGreen)
                        .border(
                            width = if (isPlaying) 3.dp else 1.dp,
                            color = if (isPlaying) PrimaryGreen else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!member.mainPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = member.mainPhotoUri,
                            contentDescription = member.fullName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(relationEmoji(member.relation), fontSize = 32.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            member.relation.displayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondaryMuted
                    )
                }
            }

            // Shared Memory Quote
            if (!member.favouriteSharedMemory.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = BackgroundCream,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💭", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            member.favouriteSharedMemory,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Hear Voice / Call)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlayVoice,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) CogCareSecondary else PrimaryGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isPlaying) "Playing Voice..." else "Hear Voice ❤️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (!member.primaryPhone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCall,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFamilyCircleView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏡", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your Family Circle",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add sons, daughters, spouses, and friends so the patient can always hear their voice and see their smiling faces.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryMuted
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add First Loved One")
        }
    }
}

@Composable
private fun AddFamilyMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, relation: FamilyRelation, phone: String, memory: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRelation by remember { mutableStateOf(FamilyRelation.ELDER_SON) }
    var phone by remember { mutableStateOf("") }
    var memory by remember { mutableStateOf("") }
    var expandedRelation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Family Circle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Relation Picker Dropdown
                Box {
                    OutlinedButton(
                        onClick = { expandedRelation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Relationship: ${selectedRelation.displayLabel}")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expandedRelation,
                        onDismissRequest = { expandedRelation = false }
                    ) {
                        listOf(
                            FamilyRelation.SPOUSE,
                            FamilyRelation.ELDER_SON,
                            FamilyRelation.YOUNGER_SON,
                            FamilyRelation.ELDER_DAUGHTER,
                            FamilyRelation.YOUNGER_DAUGHTER,
                            FamilyRelation.GRANDCHILD,
                            FamilyRelation.ELDER_BROTHER,
                            FamilyRelation.ELDER_SISTER,
                            FamilyRelation.CLOSE_FRIEND,
                            FamilyRelation.DOCTOR
                        ).forEach { rel ->
                            DropdownMenuItem(
                                text = { Text(rel.displayLabel) },
                                onClick = {
                                    selectedRelation = rel
                                    expandedRelation = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memory,
                    onValueChange = { memory = it },
                    label = { Text("Warm Greeting or Memory") },
                    placeholder = { Text("e.g. Always loved gardening with you") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedRelation, phone, memory) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun relationEmoji(relation: FamilyRelation): String = when (relation) {
    FamilyRelation.SPOUSE -> "💛"
    FamilyRelation.ELDER_SON, FamilyRelation.YOUNGER_SON -> "👦"
    FamilyRelation.ELDER_DAUGHTER, FamilyRelation.YOUNGER_DAUGHTER -> "👧"
    FamilyRelation.GRANDCHILD -> "👶"
    FamilyRelation.DOCTOR, FamilyRelation.ASHA_WORKER -> "🩺"
    else -> "👤"
}
