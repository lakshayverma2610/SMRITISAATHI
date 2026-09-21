package com.nercare.cogcare.presentation.games.pattern

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

data class PatternUiState(
    val targetPatternId: Int = 0,
    val patternOptions: List<Int> = emptyList(),
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val message: String = "Find the matching pattern",
    val startTime: Long = 0L,
    val round: Int = 1,
    val totalRounds: Int = 5
)

@HiltViewModel
class PatternMatchingViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val adaptiveDifficultyEngine: AdaptiveDifficultyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatternUiState())
    val uiState: StateFlow<PatternUiState> = _uiState.asStateFlow()

    private var patientId: String = ""
    private var baseDifficulty: Int = 1
    
    // Using simple resource IDs or integer identifiers for patterns
    private val allPatterns = (1..10).toList() 

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        this.baseDifficulty = difficulty
        
        viewModelScope.launch {
            val history = gameRepository.getRecentSessionsByGame(patientId, GameType.PATTERN_MATCHING)
            val avgAcc = history.map { it.accuracyPercent }.average().toFloat().takeIf { !it.isNaN() } ?: 50f
            val avgTime = history.map { it.avgResponseTimeMs }.average().toLong().takeIf { it > 0 } ?: 2000L
            
            // LiteRT Engine scaling
            val recommendedDiff = adaptiveDifficultyEngine.recommendDifficulty(avgAcc, avgTime, 1, difficulty)
            
            _uiState.value = _uiState.value.copy(
                score = 0,
                round = 1,
                isGameOver = false,
                startTime = System.currentTimeMillis()
            )
            
            startRound(recommendedDiff)
        }
    }

    private fun startRound(diff: Int) {
        // diff 1 -> 2 options, diff 5 -> 6 options
        val optionCount = minOf(6, maxOf(2, diff + 1))
        
        val shuffled = allPatterns.shuffled()
        val target = shuffled[0]
        val options = shuffled.take(optionCount).shuffled()
        
        _uiState.value = _uiState.value.copy(
            targetPatternId = target,
            patternOptions = options,
            message = "Which pattern matches exactly?"
        )
    }

    fun onPatternSelected(patternId: Int) {
        val state = _uiState.value
        if (state.isGameOver) return

        if (patternId == state.targetPatternId) {
            // Correct!
            val newRound = state.round + 1
            if (newRound > state.totalRounds) {
                _uiState.value = state.copy(score = state.score + 100)
                endGame(true)
            } else {
                _uiState.value = state.copy(
                    score = state.score + 100,
                    round = newRound,
                    message = "Correct! Get ready..."
                )
                viewModelScope.launch {
                    delay(1000)
                    startRound(minOf(5, baseDifficulty + (newRound / 2)))
                }
            }
        } else {
            // Wrong
            _uiState.value = state.copy(message = "Incorrect, try again!")
            viewModelScope.launch {
                delay(1500)
                if (_uiState.value.message == "Incorrect, try again!") {
                    _uiState.value = state.copy(message = "Which pattern matches exactly?")
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
                gameType = GameType.PATTERN_MATCHING,
                difficultyLevel = baseDifficulty,
                score = state.score,
                maxPossibleScore = state.totalRounds * 100,
                accuracyPercent = if (completed) 100f else (state.round.toFloat() / state.totalRounds.toFloat()) * 100f,
                avgResponseTimeMs = duration / maxOf(1, state.round),
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
