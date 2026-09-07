package com.nercare.cogcare.presentation.games.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.ai.AdaptiveDifficultyEngine
import com.nercare.cogcare.data.repository.GameRepository
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.GameType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryCardData(
    val emoji: String,
    val pairId: Int
)

data class MemoryCardUiState(
    val cards: List<MemoryCardData> = emptyList(),
    val flippedIndices: Set<Int> = emptySet(),
    val matchedIndices: Set<Int> = emptySet(),
    val score: Int = 0,
    val attempts: Int = 0,
    val matchedPairs: Int = 0,
    val totalPairs: Int = 0,
    val accuracy: Float = 0f,
    val isGameComplete: Boolean = false,
    val difficulty: Int = 2,
    val gridSize: Pair<Int, Int> = Pair(2, 4)
)

@HiltViewModel
class MemoryCardViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val difficultyEngine: AdaptiveDifficultyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryCardUiState())
    val uiState: StateFlow<MemoryCardUiState> = _uiState

    private var patientId = ""
    private var startTime = 0L
    private var responseTimes = mutableListOf<Long>()
    private var lastFlipTime = 0L
    private var isProcessingFlip = false

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        val gridSize = difficultyEngine.getMemoryGridSize(difficulty)
        val totalCards = gridSize.first * gridSize.second
        val pairsNeeded = totalCards / 2

        val selectedEmojis = NER_CARD_EMOJIS.shuffled().take(pairsNeeded)
        val cards = (selectedEmojis + selectedEmojis)
            .mapIndexed { index, emoji -> MemoryCardData(emoji, selectedEmojis.indexOf(emoji)) }
            .shuffled()

        startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                cards = cards,
                difficulty = difficulty,
                totalPairs = pairsNeeded,
                gridSize = gridSize,
                flippedIndices = emptySet(),
                matchedIndices = emptySet(),
                score = 0,
                attempts = 0,
                matchedPairs = 0,
                isGameComplete = false
            )
        }
    }

    fun onCardClicked(index: Int) {
        if (isProcessingFlip) return
        val state = _uiState.value
        if (state.matchedIndices.contains(index)) return
        if (state.flippedIndices.contains(index)) return
        if (state.flippedIndices.size >= 2) return

        val now = System.currentTimeMillis()
        if (lastFlipTime > 0) responseTimes.add(now - lastFlipTime)
        lastFlipTime = now

        val newFlipped = state.flippedIndices + index

        if (newFlipped.size == 2) {
            _uiState.update { it.copy(flippedIndices = newFlipped) }
            checkForMatch(newFlipped.toList())
        } else {
            _uiState.update { it.copy(flippedIndices = newFlipped) }
        }
    }

    private fun checkForMatch(indices: List<Int>) {
        isProcessingFlip = true
        viewModelScope.launch {
            delay(800)
            val state = _uiState.value
            val card1 = state.cards[indices[0]]
            val card2 = state.cards[indices[1]]

            val isMatch = card1.pairId == card2.pairId
            val newAttempts = state.attempts + 1

            if (isMatch) {
                val newMatched = state.matchedIndices + indices.toSet()
                val newMatchedPairs = state.matchedPairs + 1
                val scoreGain = 10 * state.difficulty
                val newScore = state.score + scoreGain
                val isComplete = newMatched.size == state.cards.size

                _uiState.update {
                    it.copy(
                        flippedIndices = emptySet(),
                        matchedIndices = newMatched,
                        score = newScore,
                        attempts = newAttempts,
                        matchedPairs = newMatchedPairs,
                        accuracy = (newMatchedPairs.toFloat() / newAttempts.toFloat()) * 100f,
                        isGameComplete = isComplete
                    )
                }

                if (isComplete) {
                    saveSession(state.difficulty, newScore, newMatchedPairs, newAttempts)
                }
            } else {
                _uiState.update {
                    it.copy(
                        flippedIndices = emptySet(),
                        attempts = newAttempts,
                        accuracy = (it.matchedPairs.toFloat() / newAttempts.toFloat()) * 100f
                    )
                }
            }
            isProcessingFlip = false
        }
    }

    private fun saveSession(difficulty: Int, score: Int, matched: Int, attempts: Int) {
        viewModelScope.launch {
            val duration = System.currentTimeMillis() - startTime
            val avgResponseTime = if (responseTimes.isNotEmpty()) responseTimes.average().toLong() else 0L
            val accuracy = (matched.toFloat() / attempts.toFloat()) * 100f

            val session = GameSession(
                patientId = patientId,
                gameType = GameType.MEMORY_CARD,
                difficultyLevel = difficulty,
                score = score,
                maxPossibleScore = matched * 10 * difficulty,
                accuracyPercent = accuracy,
                avgResponseTimeMs = avgResponseTime,
                durationMs = duration
            )
            gameRepository.saveSession(session)
        }
    }
}
