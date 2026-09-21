package com.nercare.cogcare.presentation.games.wordassoc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.ai.AdaptiveDifficultyEngine
import com.nercare.cogcare.ai.bhashini.BhashiniService
import com.nercare.cogcare.data.repository.GameRepository
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.GameType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject

data class WordPair(val prompt: String, val answer: String, val decoys: List<String>)

data class WordUiState(
    val currentPair: WordPair? = null,
    val options: List<String> = emptyList(),
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val message: String = "Select the related word",
    val startTime: Long = 0L,
    val round: Int = 1,
    val totalRounds: Int = 5,
    val translating: Boolean = false
)

@HiltViewModel
class WordAssociationViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val adaptiveDifficultyEngine: AdaptiveDifficultyEngine,
    private val bhashiniService: BhashiniService, // For live NMT translation of the prompt
    private val geminiContentGenerator: com.nercare.cogcare.ai.gemini.GeminiContentGenerator,
    private val gameContentDao: com.nercare.cogcare.data.local.dao.GameContentDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordUiState())
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    private var patientId: String = ""
    private var baseDifficulty: Int = 1

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        this.baseDifficulty = difficulty
        
        viewModelScope.launch {
            val history = gameRepository.getRecentSessionsByGame(patientId, GameType.WORD_ASSOCIATION)
            val avgAcc = history.map { it.accuracyPercent }.average().toFloat().takeIf { !it.isNaN() } ?: 50f
            val avgTime = history.map { it.avgResponseTimeMs }.average().toLong().takeIf { it > 0 } ?: 2000L
            
            val recommendedDiff = adaptiveDifficultyEngine.recommendDifficulty(avgAcc, avgTime, 1, difficulty)
            
            _uiState.value = _uiState.value.copy(score = 0, round = 1, isGameOver = false, startTime = System.currentTimeMillis())
            startRound(recommendedDiff)
        }
    }

    private fun startRound(diff: Int) {
        val optionCount = 4
        
        _uiState.value = _uiState.value.copy(
            message = "Generating dynamic words...",
            translating = true
        )

        viewModelScope.launch {
            try {
                // 1. Try to fetch dynamic AI content via Gemini API
                val dynamicPair = geminiContentGenerator.generateWordPair()
                val options = (dynamicPair.decoys.shuffled().take(optionCount - 1) + dynamicPair.answer).shuffled()
                
                _uiState.value = _uiState.value.copy(
                    currentPair = dynamicPair,
                    options = options,
                    message = "Select the related word",
                    translating = false
                )
            } catch (e: Exception) {
                // 2. Offline Fallback: Read from Room Database Cache
                try {
                    val cachedEntity = gameContentDao.getRandomUnusedContent(GameType.WORD_ASSOCIATION.name)
                    if (cachedEntity != null) {
                        gameContentDao.markAsUsed(cachedEntity.id)
                        val pair = Json.decodeFromString<WordPair>(cachedEntity.contentJson)
                        val options = (pair.decoys.shuffled().take(optionCount - 1) + pair.answer).shuffled()
                        
                        _uiState.value = _uiState.value.copy(
                            currentPair = pair,
                            options = options,
                            message = "Select the related word",
                            translating = false
                        )
                    } else {
                        // Offline Fallback list (5 unique pairs)
                        val fallbackPairs = listOf(
                            WordPair("Rain", "Umbrella", listOf("Sun", "Star", "Tree", "Moon")),
                            WordPair("Tea", "Cup", listOf("Plate", "Fork", "Car", "Tree")),
                            WordPair("Bird", "Nest", listOf("House", "Dog", "Fish", "Car")),
                            WordPair("Book", "Read", listOf("Sing", "Run", "Eat", "Sleep")),
                            WordPair("Night", "Moon", listOf("Sun", "Day", "Cloud", "Rain"))
                        )
                        // Use round index to cycle through the 5 pairs
                        val safeIndex = (uiState.value.round - 1) % fallbackPairs.size
                        val fallbackPair = fallbackPairs[safeIndex]
                        
                        val options = (fallbackPair.decoys.shuffled().take(optionCount - 1) + fallbackPair.answer).shuffled()
                        _uiState.value = _uiState.value.copy(
                            currentPair = fallbackPair,
                            options = options,
                            message = "Select the related word",
                            translating = false
                        )
                    }
                } catch (dbError: Exception) {
                    _uiState.value = _uiState.value.copy(translating = false, message = "Database Error")
                }
            }
        }
    }

    fun onOptionSelected(option: String) {
        val state = _uiState.value
        if (state.isGameOver || state.translating) return

        if (option == state.currentPair?.answer) {
            val newRound = state.round + 1
            if (newRound > state.totalRounds) {
                _uiState.value = state.copy(score = state.score + 100)
                endGame(true)
            } else {
                _uiState.value = state.copy(score = state.score + 100, round = newRound, message = "Correct!")
                viewModelScope.launch {
                    delay(1000)
                    startRound(minOf(5, baseDifficulty + (newRound / 2)))
                }
            }
        } else {
            _uiState.value = state.copy(message = "Not quite, try again!")
            viewModelScope.launch {
                delay(1500)
                if (_uiState.value.message.startsWith("Not quite")) {
                    _uiState.value = state.copy(message = "Select the related word")
                }
            }
        }
    }

    private fun endGame(completed: Boolean) {
        val state = _uiState.value
        val duration = System.currentTimeMillis() - state.startTime
        
        viewModelScope.launch {
            val session = GameSession(
                patientId = patientId,
                gameType = GameType.WORD_ASSOCIATION,
                difficultyLevel = baseDifficulty,
                score = state.score,
                maxPossibleScore = state.totalRounds * 100,
                accuracyPercent = if (completed) 100f else (state.round.toFloat() / state.totalRounds.toFloat()) * 100f,
                avgResponseTimeMs = duration / maxOf(1, state.round),
                durationMs = duration
            )
            gameRepository.saveSession(session)
            
            _uiState.value = state.copy(isGameOver = true, message = "Game Over! Score: ${state.score}")
        }
    }
}
