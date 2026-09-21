package com.nercare.cogcare.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.presentation.theme.*

@Composable
fun HomeScreen(
    patientId: String,
    onNavigateToGames: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToCaregiverDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMatching: () -> Unit,
    onNavigateToVoice: () -> Unit = {},
    onNavigateToLifeStory: () -> Unit = {},
    onNavigateToFamilyCircle: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(patientId) {
        viewModel.loadPatient(patientId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Namaste, ${uiState.patient?.name ?: "Friend"}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "What would you like to do today?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Voice Companion Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = SecondaryGreen
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated pulse indicator
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Say \"Remind me at 5 PM\" or \"Play a memory game\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryGreen,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToVoice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "🌿 Tap to speak with Saathi",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Dynamic Tell Us About Yourself Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .clickable { onNavigateToLifeStory() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryGreen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🌸", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tell us about yourself",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "Let Saathi get to know you better",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryGreen
                )
            }
        }

        if (uiState.caregiverMemories.isNotEmpty()) {
            Text(
                "Memories shared by your caregiver",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToProfile() }.padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGreen)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.caregiverMemories.forEach { memory ->
                        Column {
                            Text(memory.questionPrompt.ifBlank { memory.nodeKey.replace('_', ' ').replaceFirstChar { it.uppercase() } }, style = MaterialTheme.typography.labelMedium, color = TextSecondaryMuted)
                            Text(memory.value, style = MaterialTheme.typography.bodyLarge, color = TextPrimaryDark)
                        }
                    }
                    Text("View all memories", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Dedicated Family & Loved Ones Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .clickable { onNavigateToFamilyCircle() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🏡", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Family & Loved Ones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "Hear loving voice notes & familiar faces",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryGreen
                )
            }
        }

        // Dynamic Next Reminder Card
        val nextRem = uiState.nextReminder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceWhite
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = SecondaryGreen
            )
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Accent left border
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(130.dp)
                        .background(if (nextRem != null) WarningYellow else AccentMint)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (nextRem != null) "NEXT REMINDER" else "SCHEDULE STATUS",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (nextRem != null) WarningYellow else PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (nextRem != null) {
                        Text(
                            text = "${nextRem.type.emoji} ${nextRem.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )

                        val timeFormatted = viewModel.formatTime(nextRem.hour, nextRem.minute)
                        Text(
                            text = "$timeFormatted • ${if (nextRem.description.isNotBlank()) nextRem.description else "Scheduled for today"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryMuted,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )
                    } else {
                        Text(
                            text = "All caught up today! 🌿",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No pending reminders right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryMuted,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = onNavigateToReminders,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryGreen,
                            contentColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("View all reminders", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                }
            }
        }

        // Action Grid (Games, Memories)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                modifier = Modifier.weight(1f),
                title = "Play Games",
                icon = Icons.Filled.PlayArrow,
                onClick = onNavigateToGames
            )

            ActionCard(
                modifier = Modifier.weight(1f),
                title = "Life Memories",
                icon = Icons.Filled.Face,
                onClick = onNavigateToProfile
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Find people with shared interests",
            icon = Icons.Filled.Person,
            onClick = onNavigateToMatching
        )


    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = SecondaryGreen
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = SecondaryGreen,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Open",
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
