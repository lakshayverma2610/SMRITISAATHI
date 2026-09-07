package com.nercare.cogcare.presentation.games.wordassoc

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

data class WordQuestion(
    val prompt: String,
    val promptEmoji: String,
    val answer: String,
    val choices: List<String>,
    val hint: String = ""
)

// NER-culturally relevant word associations
val WORD_ASSOCIATION_DATA = listOf(
    WordQuestion("Rain goes with...", "🌧️", "Umbrella", listOf("Umbrella", "Sun", "Sand", "Wind"), "We use it to stay dry"),
    WordQuestion("Tea is served in a...", "🍵", "Cup", listOf("Cup", "Plate", "Pot", "Basket"), "Round container for drinks"),
    WordQuestion("Flowers bloom in...", "🌸", "Spring", listOf("Spring", "Winter", "Night", "Storm"), "Season after winter"),
    WordQuestion("We read books with our...", "📚", "Eyes", listOf("Eyes", "Hands", "Nose", "Feet"), "For seeing"),
    WordQuestion("Doctor works in a...", "👨‍⚕️", "Hospital", listOf("Hospital", "School", "Market", "Farm"), "For treating patients"),
    WordQuestion("Fish lives in...", "🐟", "Water", listOf("Water", "Tree", "Land", "Sky"), "Essential for life"),
    WordQuestion("We sleep on a...", "😴", "Bed", listOf("Bed", "Table", "Chair", "Floor"), "Piece of furniture for rest"),
    WordQuestion("Birds have...", "🦅", "Wings", listOf("Wings", "Fins", "Wheels", "Shoes"), "Used for flying"),
    WordQuestion("We cook food on a...", "🍳", "Stove", listOf("Stove", "Fridge", "Clock", "Tap"), "Heat source for cooking"),
    WordQuestion("Bamboo is a...", "🎋", "Plant", listOf("Plant", "Animal", "Mineral", "Metal"), "Common in NER"),
)

@Composable
fun WordAssociationGameScreen(
    patientId: String,
    difficulty: Int,
    onGameComplete: () -> Unit,
    viewModel: WordAssocViewModel = hiltViewModel()
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
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onGameComplete) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝 Word Association", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("${uiState.currentQ + 1} / ${uiState.totalQuestions}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Score", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = WordGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { uiState.currentQ.toFloat() / uiState.totalQuestions.toFloat() },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = WordGreen,
                trackColor = CogCareSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isGameComplete) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 64.sp)
                        Text("Fantastic!", style = MaterialTheme.typography.displayMedium, color = WordGreen, fontWeight = FontWeight.Bold)
                        Text("Score: ${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text("Accuracy: ${uiState.accuracy.toInt()}%", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                    }
                }
            } else {
                uiState.currentQuestion?.let { question ->
                    // Question card
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)) {
                        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(question.promptEmoji, fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(question.prompt, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, textAlign = TextAlign.Center)
                            if (uiState.showHint) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Hint: ${question.hint}", style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Answer choices
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        question.choices.forEach { choice ->
                            val isSelected = uiState.selectedAnswer == choice
                            val isCorrect = choice == question.answer
                            val showFeedback = uiState.showFeedback
                            Card(
                                onClick = { if (!showFeedback) viewModel.onAnswerSelected(choice) },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        showFeedback && isCorrect -> SuccessGreen.copy(alpha = 0.25f)
                                        showFeedback && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.25f)
                                        isSelected -> WordGreen.copy(alpha = 0.2f)
                                        else -> CogCareSurfaceVariant
                                    }
                                ),
                                border = if (isSelected) BorderStroke(2.dp, if (showFeedback && isCorrect) SuccessGreen else WordGreen) else null
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        choice,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hint button
                    if (!uiState.showHint && !uiState.showFeedback) {
                        TextButton(onClick = { viewModel.showHint() }) {
                            Text("💡 Need a hint?", color = WarningAmber, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
