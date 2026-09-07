package com.nercare.cogcare.presentation.games.routine

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

data class RoutineQuestion(
    val question: String,
    val emoji: String,
    val choices: List<String>,
    val answer: String
)

val DAILY_ROUTINE_QUESTIONS = listOf(
    RoutineQuestion("What do you use to brush your teeth?", "🦷", listOf("Toothbrush", "Comb", "Spoon", "Pen"), "Toothbrush"),
    RoutineQuestion("When do you usually wake up?", "🌅", listOf("Morning", "Midnight", "Afternoon", "Evening"), "Morning"),
    RoutineQuestion("What meal do you have first in the day?", "🍳", listOf("Breakfast", "Dinner", "Lunch", "Snack"), "Breakfast"),
    RoutineQuestion("What do you wear on your feet to go outside?", "👟", listOf("Shoes", "Hat", "Gloves", "Scarf"), "Shoes"),
    RoutineQuestion("Where do you sleep at night?", "🛏️", listOf("Bed", "Chair", "Floor", "Sofa"), "Bed"),
    RoutineQuestion("What do you drink when you are thirsty?", "💧", listOf("Water", "Sand", "Stone", "Air"), "Water"),
    RoutineQuestion("Where do you go to buy vegetables?", "🥦", listOf("Market", "Hospital", "Temple", "School"), "Market"),
    RoutineQuestion("What do you use to write?", "✏️", listOf("Pen", "Fork", "Key", "Brush"), "Pen"),
    RoutineQuestion("What do you read in the morning?", "📰", listOf("Newspaper", "Pillow", "Food", "Shoes"), "Newspaper"),
    RoutineQuestion("When do you take your medicines?", "💊", listOf("As prescribed", "Never", "Once a year", "Only in pain"), "As prescribed"),
)

@Composable
fun DailyRoutineGameScreen(
    patientId: String,
    difficulty: Int,
    onGameComplete: () -> Unit,
    viewModel: DailyRoutineViewModel = hiltViewModel()
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
        Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onGameComplete) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌅 Daily Routine", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("${uiState.currentQ + 1} / ${uiState.totalQuestions}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Score", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = RoutineOrange, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { uiState.currentQ.toFloat() / uiState.totalQuestions.toFloat() },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = RoutineOrange,
                trackColor = CogCareSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isGameComplete) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 64.sp)
                        Text("Excellent!", style = MaterialTheme.typography.displayMedium, color = RoutineOrange, fontWeight = FontWeight.Bold)
                        Text("Score: ${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text("Accuracy: ${uiState.accuracy.toInt()}%", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                    }
                }
            } else {
                uiState.currentQuestion?.let { question ->
                    // Big emoji
                    Text(question.emoji, fontSize = 80.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Question
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
                        Text(
                            question.question,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Choose your answer:", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Choices
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        question.choices.forEach { choice ->
                            val isSelected = uiState.selectedAnswer == choice
                            val isCorrect = choice == question.answer
                            val showFeedback = uiState.showFeedback
                            Card(
                                onClick = { if (!showFeedback) viewModel.onAnswerSelected(choice) },
                                modifier = Modifier.fillMaxWidth().height(72.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        showFeedback && isCorrect -> SuccessGreen.copy(alpha = 0.25f)
                                        showFeedback && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.25f)
                                        isSelected -> RoutineOrange.copy(alpha = 0.2f)
                                        else -> CogCareSurfaceVariant
                                    }
                                ),
                                border = if (isSelected) BorderStroke(2.dp, if (showFeedback && isCorrect) SuccessGreen else RoutineOrange) else null
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(choice, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
