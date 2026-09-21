package com.nercare.cogcare.presentation.games.memory

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import com.nercare.cogcare.presentation.theme.*
import kotlinx.coroutines.delay



import androidx.compose.ui.platform.LocalContext
import com.nercare.cogcare.presentation.games.RegionalSoundManager

@Composable
fun MemoryCardGameScreen(
    patientId: String,
    difficulty: Int,
    onGameComplete: () -> Unit,
    viewModel: MemoryCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patientId, difficulty) {
        viewModel.startGame(patientId, difficulty)
    }

    BackHandler(enabled = !uiState.isGameComplete) {
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

    val context = LocalContext.current
    val soundManager = remember { RegionalSoundManager(context) }

    LaunchedEffect(uiState.isGameComplete) {
        if (uiState.isGameComplete) {
            soundManager.playCulturalSuccessSound()
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (!uiState.isGameComplete) showExitDialog = true else onGameComplete() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🃏 Memory Cards", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("Level ${uiState.difficulty}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                // Score
                Column(horizontalAlignment = Alignment.End) {
                    Text("Score", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${uiState.score}", style = MaterialTheme.typography.headlineSmall, color = MemoryCardBlue, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress
            LinearProgressIndicator(
                progress = { if (uiState.totalPairs == 0) 0f else uiState.matchedPairs.toFloat() / uiState.totalPairs.toFloat() },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = MemoryCardBlue,
                trackColor = CogCareSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${uiState.matchedPairs} / ${uiState.totalPairs} pairs found",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Game complete overlay
            if (uiState.isGameComplete) {
                GameCompleteOverlay(
                    score = uiState.score,
                    accuracy = uiState.accuracy
                )
            } else {
                // Card grid
                val (rows, cols) = uiState.gridSize
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.cards) { index, card ->
                        MemoryCard(
                            card = card,
                            isFlipped = uiState.flippedIndices.contains(index) || uiState.matchedIndices.contains(index),
                            isMatched = uiState.matchedIndices.contains(index),
                            onClick = { viewModel.onCardClicked(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(
    card: MemoryCardData,
    isFlipped: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_flip"
    )

    val scale by animateFloatAsState(
        targetValue = if (isMatched) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isMatched -> SuccessGreen.copy(alpha = 0.3f)
                    isFlipped -> MemoryCardBlue.copy(alpha = 0.2f)
                    else -> CogCareSurfaceVariant
                }
            )
            .border(
                width = 2.dp,
                color = when {
                    isMatched -> SuccessGreen
                    isFlipped -> MemoryCardBlue
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isFlipped && !isMatched) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isFlipped || isMatched) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = card.drawableRes),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .graphicsLayer { rotationY = rotation }
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White)
            )
        } else {
            Text(
                text = "❓",
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GameCompleteOverlay(score: Int, accuracy: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CogCareSurfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 64.sp)
            Text("Well Done!", style = MaterialTheme.typography.displayMedium, color = SuccessGreen, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Score: $score", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text("Accuracy: ${accuracy.toInt()}%", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
        }
    }
}
