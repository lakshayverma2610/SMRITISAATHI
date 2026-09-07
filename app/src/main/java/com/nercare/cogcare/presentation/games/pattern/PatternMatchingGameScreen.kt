package com.nercare.cogcare.presentation.games.pattern

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.presentation.theme.*
import kotlinx.coroutines.delay

// Patterns using NER traditional motifs
data class PatternQuestion(
    val sequence: List<String>,   // shown pattern
    val answer: String,            // correct next item
    val choices: List<String>      // all choices
)

val NER_PATTERN_SETS = listOf(
    listOf("🌿", "🌸", "🎋", "🌿", "🌸"),
    listOf("🦋", "🐘", "🦋", "🐘", "🦋"),
    listOf("⭕", "🔷", "⭕", "🔷", "⭕"),
    listOf("🏔️", "🌊", "🏔️", "🌊", "🏔️"),
    listOf("🌙", "⭐", "🌙", "⭐", "🌙"),
    listOf("🔴", "🔵", "🟡", "🔴", "🔵"),
    listOf("🌺", "🌻", "🌺", "🌻", "🌺"),
    listOf("🦅", "🦚", "🦅", "🦚", "🦅")
)

@Composable
fun PatternMatchingGameScreen(
    patientId: String,
    difficulty: Int,
    onGameComplete: () -> Unit,
    viewModel: PatternViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(patientId, difficulty) {
        viewModel.startGame(patientId, difficulty)
    }

    LaunchedEffect(uiState.isGameComplete) {
        if (uiState.isGameComplete) {
            delay(2000)
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
                IconButton(onClick = onGameComplete) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔷 Pattern Matching", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("${uiState.currentQ + 1} / ${uiState.totalQuestions}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Score", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = PatternPurple, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { (uiState.currentQ).toFloat() / uiState.totalQuestions.toFloat() },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = PatternPurple,
                trackColor = CogCareSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isGameComplete) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 64.sp)
                        Text("Well Done!", style = MaterialTheme.typography.displayMedium, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        Text("Score: ${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text("Accuracy: ${uiState.accuracy.toInt()}%", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                    }
                }
            } else {
                uiState.currentQuestion?.let { question ->
                    // Pattern display
                    Text("What comes next?", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
                        Row(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            question.sequence.forEach { emoji ->
                                Text(emoji, fontSize = 36.sp)
                            }
                            Text("❓", fontSize = 36.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Choose the answer:", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Choice buttons
                    val cols = if (question.choices.size <= 4) 2 else 3
                    for (rowStart in question.choices.indices step cols) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (i in rowStart until minOf(rowStart + cols, question.choices.size)) {
                                val choice = question.choices[i]
                                val isSelected = uiState.selectedAnswer == choice
                                val isCorrect = choice == question.answer
                                val showFeedback = uiState.showFeedback
                                Card(
                                    onClick = { if (!showFeedback) viewModel.onAnswerSelected(choice) },
                                    modifier = Modifier.weight(1f).height(80.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            showFeedback && isCorrect -> SuccessGreen.copy(alpha = 0.3f)
                                            showFeedback && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.3f)
                                            isSelected -> PatternPurple.copy(alpha = 0.3f)
                                            else -> CogCareSurfaceVariant
                                        }
                                    ),
                                    border = if (isSelected) BorderStroke(2.dp, if (showFeedback && isCorrect) SuccessGreen else PatternPurple) else null
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(choice, fontSize = 40.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
