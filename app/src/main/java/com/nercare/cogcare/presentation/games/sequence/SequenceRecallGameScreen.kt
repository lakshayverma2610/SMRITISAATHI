package com.nercare.cogcare.presentation.games.sequence

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.presentation.theme.*
import kotlinx.coroutines.delay

val SEQUENCE_BUTTONS = listOf(
    Pair("🌿", Color(0xFF27AE60)),
    Pair("🌸", Color(0xFFE74C3C)),
    Pair("💛", Color(0xFFF1C40F)),
    Pair("💙", Color(0xFF3498DB))
)

@Composable
fun SequenceRecallGameScreen(
    patientId: String,
    difficulty: Int,
    onGameComplete: () -> Unit,
    viewModel: SequenceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(patientId, difficulty) {
        viewModel.startGame(patientId, difficulty)
    }

    LaunchedEffect(uiState.isGameComplete) {
        if (uiState.isGameComplete) {
            delay(2500)
            onGameComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CogCareBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onGameComplete) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("🔢 Sequence Recall", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Round", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${uiState.currentRound}", style = MaterialTheme.typography.headlineSmall, color = SequenceGold, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status text
            AnimatedContent(targetState = uiState.phase, label = "phase") { phase ->
                Text(
                    text = when (phase) {
                        SequencePhase.SHOWING -> "👀 Watch the sequence..."
                        SequencePhase.WAITING -> "🎯 Your turn! Repeat the sequence"
                        SequencePhase.SUCCESS -> "✅ Correct! Next round..."
                        SequencePhase.FAILED -> "❌ Oops! The sequence was: ${uiState.sequence.map { SEQUENCE_BUTTONS[it].first }.joinToString(" ")}"
                        SequencePhase.COMPLETE -> "🎉 Amazing! You completed all rounds!"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (phase) {
                        SequencePhase.SUCCESS -> SuccessGreen
                        SequencePhase.FAILED -> ErrorRed
                        SequencePhase.COMPLETE -> SequenceGold
                        else -> TextPrimary
                    },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sequence length indicator
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(uiState.sequence.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (index < uiState.playerInput.size) SuccessGreen
                                else CogCareSurfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2x2 button grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (col in 0..1) {
                            val idx = row * 2 + col
                            val (emoji, color) = SEQUENCE_BUTTONS[idx]
                            val isLit = uiState.litButton == idx
                            val isEnabled = uiState.phase == SequencePhase.WAITING

                            val scale by animateFloatAsState(
                                targetValue = if (isLit) 1.15f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "btn_scale_$idx"
                            )

                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        if (isLit) color else color.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        3.dp,
                                        if (isLit) Color.White.copy(alpha = 0.7f) else Color.Transparent,
                                        RoundedCornerShape(24.dp)
                                    )
                                    .clickable(enabled = isEnabled) {
                                        viewModel.onButtonPressed(idx)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 48.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Score
            Text(
                "Score: ${uiState.score}",
                style = MaterialTheme.typography.titleLarge,
                color = SequenceGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
