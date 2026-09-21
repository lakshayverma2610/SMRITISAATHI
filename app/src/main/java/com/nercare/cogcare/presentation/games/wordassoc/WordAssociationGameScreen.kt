package com.nercare.cogcare.presentation.games.wordassoc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import com.nercare.cogcare.presentation.theme.*

@Composable
fun WordAssociationGameScreen(
    patientId: String,
    difficulty: Int,
    onGameComplete: () -> Unit,
    viewModel: WordAssociationViewModel = hiltViewModel()
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
                    Text("📝 Word Association", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("Round ${uiState.round} of ${uiState.totalRounds}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Score", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = WordGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isGameOver) {
                Text(
                    uiState.message,
                    style = MaterialTheme.typography.headlineMedium,
                    color = WordGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onGameComplete,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = WordGreen)
                ) {
                    Text("Finish Game")
                }
            } else {
                Text(
                    uiState.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.message.contains("Not quite")) ErrorRed else TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Target Word
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(2.dp, WordGreen.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (uiState.translating) {
                            CircularProgressIndicator(color = WordGreen)
                        } else {
                            Text(
                                text = uiState.currentPair?.prompt ?: "",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    "Which word is most related?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Options Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.options) { option ->
                        Button(
                            onClick = { viewModel.onOptionSelected(option) },
                            modifier = Modifier.height(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceWhite,
                                contentColor = WordGreen
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(option, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
