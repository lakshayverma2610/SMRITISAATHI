package com.nercare.cogcare.presentation.games.sequence

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SequenceUiState(
    val sequenceLength: Int = 3,
    val targetSequence: List<Int> = emptyList(),
    val userSequence: List<Int> = emptyList(),
    val isShowingSequence: Boolean = true,
    val highlightedIndex: Int = -1,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val message: String = "Watch the sequence...",
    val startTime: Long = 0L,
    val round: Int = 1,
    val totalRounds: Int = 5
)

@HiltViewModel
class SequenceRecallViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val adaptiveDifficultyEngine: AdaptiveDifficultyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SequenceUiState())
    val uiState: StateFlow<SequenceUiState> = _uiState.asStateFlow()

    private var patientId: String = ""
    private var baseDifficulty: Int = 1

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        this.baseDifficulty = difficulty
        
        viewModelScope.launch {
            val history = gameRepository.getRecentSessionsByGame(patientId, GameType.SEQUENCE_RECALL)
            val avgAcc = history.map { it.accuracyPercent }.average().toFloat().takeIf { !it.isNaN() } ?: 50f
            val avgTime = history.map { it.avgResponseTimeMs }.average().toLong().takeIf { it > 0 } ?: 2000L
            
            // Use LiteRT Engine
            val recommendedDiff = adaptiveDifficultyEngine.recommendDifficulty(avgAcc, avgTime, 1, difficulty)
            
            startRound(recommendedDiff)
        }
    }

    private fun startRound(diff: Int) {
        // diff 1 -> 3 items, diff 5 -> 7 items
        val length = minOf(7, maxOf(3, diff + 2))
        val sequence = (0 until length).map { (0..3).random() }
        
        _uiState.value = _uiState.value.copy(
            sequenceLength = length,
            targetSequence = sequence,
            userSequence = emptyList(),
            isShowingSequence = true,
            highlightedIndex = -1,
            isGameOver = false,
            message = "Watch closely...",
            startTime = System.currentTimeMillis()
        )
        
        playSequence(sequence)
    }

    private fun playSequence(sequence: List<Int>) {
        viewModelScope.launch {
            delay(1000)
            for (item in sequence) {
                _uiState.value = _uiState.value.copy(highlightedIndex = item)
                delay(600)
                _uiState.value = _uiState.value.copy(highlightedIndex = -1)
                delay(200)
            }
            _uiState.value = _uiState.value.copy(
                isShowingSequence = false,
                message = "Your turn! Repeat the pattern."
            )
        }
    }

    fun onTileClicked(index: Int) {
        val state = _uiState.value
        if (state.isShowingSequence || state.isGameOver) return

        val newUserSequence = state.userSequence + index
        
        // Check if correct so far
        val isCorrectSoFar = newUserSequence.indices.all { i -> newUserSequence[i] == state.targetSequence[i] }
        
        if (!isCorrectSoFar) {
            endGame(false)
            return
        }

        _uiState.value = state.copy(userSequence = newUserSequence)

        // Check if finished sequence
        if (newUserSequence.size == state.targetSequence.size) {
            val scoreBonus = 100 * state.round
            val newScore = state.score + scoreBonus
            
            if (state.round >= state.totalRounds) {
                _uiState.value = state.copy(score = newScore)
                endGame(true)
            } else {
                _uiState.value = state.copy(
                    score = newScore,
                    round = state.round + 1
                )
                // Start next round with higher difficulty
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(message = "Great job! Get ready...", isShowingSequence = true)
                    delay(1500)
                    startRound(minOf(5, baseDifficulty + state.round - 1))
                }
            }
        }
    }

    private fun endGame(completed: Boolean) {
        val state = _uiState.value
        val duration = System.currentTimeMillis() - state.startTime
        
        viewModelScope.launch {
            // Save to room -> triggers SyncWorker automatically
            val session = GameSession(
                patientId = patientId,
                gameType = GameType.SEQUENCE_RECALL,
                difficultyLevel = baseDifficulty,
                score = state.score,
                maxPossibleScore = 500, // arbitrary
                accuracyPercent = if (completed) 100f else (state.userSequence.size.toFloat() / state.targetSequence.size.toFloat()) * 100f,
                avgResponseTimeMs = duration / maxOf(1, state.userSequence.size),
                durationMs = duration
            )
            gameRepository.saveSession(session)
            
            _uiState.value = state.copy(
                isGameOver = true,
                message = "Game Over! Score: ${state.score}"
            )
        }
    }
}
