package com.nercare.cogcare.presentation.games

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.domain.model.GameType
import com.nercare.cogcare.presentation.theme.*

data class CognitiveGameItem(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val domain: String,
    val color: Color,
    val isNativeEngine: Boolean = false,
    val nativeGameType: GameType? = null
)

@Composable
fun GameHubScreen(
    patientId: String,
    onNavigateToGame: (String, Int) -> Unit,
    onNavigateToGenericGame: (String, String, String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    viewModel: GameHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDomainFilter by remember { mutableStateOf("All") }

    LaunchedEffect(patientId) {
        viewModel.loadRecommendation(patientId)
    }

    val domains = listOf("All", "Memory", "Language", "Visuospatial", "Executive", "Attention")

    val all15Games = listOf(
        // Domain 1: Memory & Life Recall
        CognitiveGameItem("memory_card", "Memory Cards", "Match pairs of culturally familiar images", "🃏", "Memory", MemoryCardBlue, true, GameType.MEMORY_CARD),
        CognitiveGameItem("spot_change", "Spot the Change", "Notice subtle differences in daily scenes", "🔍", "Memory", AccentTeal),
        CognitiveGameItem("life_story_recall", "My Life Story Recall", "Remember family, hometown, and joyous milestones", "📖", "Memory", PrimaryGreen),
        CognitiveGameItem("familiar_music", "Familiar Melody", "Complete classic melodies and festive songs", "🎵", "Memory", PatternPurple),

        // Domain 2: Language & Semantic Association
        CognitiveGameItem("word_assoc", "Word-Picture Connect", "Connect words and names that belong together", "📝", "Language", WordGreen, true, GameType.WORD_ASSOCIATION),
        CognitiveGameItem("complete_phrase", "Complete the Phrase", "Recall timeless proverbs and folk sayings", "💬", "Language", WarningYellow),
        CognitiveGameItem("category_word", "Category Word Finder", "Identify fruits, spices, and household items", "🗂️", "Language", AccentMint),

        // Domain 3: Visuospatial & Orientation
        CognitiveGameItem("day_time", "Day & Time Orientation", "Anchor yourself in today's day, season, and time", "⏰", "Visuospatial", InfoBlue),
        CognitiveGameItem("my_home", "My Home Map", "Recognize favorite rooms and familiar spaces", "🏡", "Visuospatial", SecondaryGreen),
        CognitiveGameItem("find_your_way", "Find Your Way", "Gentle step-by-step spatial navigation", "🧭", "Visuospatial", SequenceGold),

        // Domain 4: Executive Function & Daily Living
        CognitiveGameItem("daily_routine", "Daily Routine Sequencer", "Order your daily morning, meal, and tea steps", "🌅", "Executive", RoutineOrange, true, GameType.DAILY_ROUTINE),
        CognitiveGameItem("object_sorting", "Object Sorting", "Organize items into kitchen and wardrobe drawers", "🧺", "Executive", TertiaryGreen),
        CognitiveGameItem("virtual_grocery", "Virtual Grocery Cart", "Select wholesome items for traditional recipes", "🛒", "Executive", SuccessGreen),
        CognitiveGameItem("sequence_recall", "Picture Sequence", "Recall the order of steps and patterns", "🔢", "Executive", SequenceGold, true, GameType.SEQUENCE_RECALL),

        // Domain 5: Attention & Emotional Well-Being
        CognitiveGameItem("emotion_match", "Emotion Match", "Identify warm feelings, smiles, and calm moments", "😊", "Attention", AccentPink),
        CognitiveGameItem("saathi_challenge", "Saathi Daily Brain Challenge", "Fun pattern puzzle tailored to your day", "🔷", "Attention", PatternPurple, true, GameType.PATTERN_MATCHING)
    )

    val groupedGames = all15Games.groupBy { it.domain }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(20.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🎮 SmritiSaathi Game Hub",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Text(
                            text = "15 tailored cognitive exercises for mind & joy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Recommendation card
                uiState.recommendation?.let { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondaryGreen),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌿", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Personalized Recommendation", style = MaterialTheme.typography.titleMedium, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(rec.encouragementMessage, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigateToGame(rec.nextGame.name, rec.level) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("▶  Play ${rec.nextGame.displayName}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            groupedGames.forEach { (domain, gamesInDomain) ->
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        color = BackgroundCream.copy(alpha = 0.95f)
                    ) {
                        Text(
                            text = "$domain Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                items(gamesInDomain) { game ->
                    Card(
                        onClick = {
                            if (game.isNativeEngine && game.nativeGameType != null) {
                                onNavigateToGame(game.nativeGameType.name, uiState.recommendedDifficulty)
                            } else {
                                onNavigateToGenericGame(game.id, game.title, game.domain)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(game.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(game.emoji, fontSize = 28.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = game.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryMuted,
                                    maxLines = 2
                                )
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
