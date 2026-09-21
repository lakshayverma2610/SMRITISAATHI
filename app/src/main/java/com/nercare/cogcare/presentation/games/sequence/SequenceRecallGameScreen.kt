package com.nercare.cogcare.presentation.games.sequence

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.activity.compose.BackHandler
import com.nercare.cogcare.presentation.theme.*

@Composable
fun SequenceRecallGameScreen(
    patientId: String,
    difficulty: Int,
    onGameComplete: () -> Unit,
    viewModel: SequenceRecallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patientId, difficulty) {
        viewModel.startGame(patientId, difficulty)
    }

    BackHandler(enabled = !uiState.isGameOver) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Game?") },
            text = { Text("Are you sure you want to exit? Your progress will be lost.") },
            confirmButton = {
                TextButton(onClick = onGameComplete) {
                    Text("Yes, Exit", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CogCareBackground)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (!uiState.isGameOver) showExitDialog = true else onGameComplete() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔢 Sequence Recall", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("Round ${uiState.round} of ${uiState.totalRounds}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Score", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = SequenceGold, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                uiState.message,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (uiState.isGameOver) {
                Button(
                    onClick = onGameComplete,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = SequenceGold)
                ) {
                    Text("Finish Game")
                }
            } else {
                // 2x2 Grid of color pads
                val colors = listOf(MemoryCardBlue, SequenceGold, ErrorRed, PrimaryGreen)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SequencePad(color = colors[0], isHighlighted = uiState.highlightedIndex == 0) { viewModel.onTileClicked(0) }
                        SequencePad(color = colors[1], isHighlighted = uiState.highlightedIndex == 1) { viewModel.onTileClicked(1) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SequencePad(color = colors[2], isHighlighted = uiState.highlightedIndex == 2) { viewModel.onTileClicked(2) }
                        SequencePad(color = colors[3], isHighlighted = uiState.highlightedIndex == 3) { viewModel.onTileClicked(3) }
                    }
                }
            }
        }
    }
}

@Composable
fun SequencePad(color: Color, isHighlighted: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val isActive = isHighlighted || isPressed

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.5f,
        animationSpec = tween(150),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = alpha))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
    )
}
