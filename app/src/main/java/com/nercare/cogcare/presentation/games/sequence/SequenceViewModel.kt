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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SequencePhase { SHOWING, WAITING, SUCCESS, FAILED, COMPLETE }

data class SequenceUiState(
    val sequence: List<Int> = emptyList(),
    val playerInput: List<Int> = emptyList(),
    val phase: SequencePhase = SequencePhase.SHOWING,
    val currentRound: Int = 1,
    val totalRounds: Int = 5,
    val score: Int = 0,
    val litButton: Int? = null,
    val difficulty: Int = 2,
    val isGameComplete: Boolean = false
)

@HiltViewModel
class SequenceViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val difficultyEngine: AdaptiveDifficultyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SequenceUiState())
    val uiState: StateFlow<SequenceUiState> = _uiState

    private var patientId = ""
    private var startTime = 0L
    private var totalAttempts = 0
    private var correctAttempts = 0
    private val responseTimes = mutableListOf<Long>()
    private var inputStartTime = 0L

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        startTime = System.currentTimeMillis()
        val totalRounds = when (difficulty) { 1 -> 3; 2 -> 4; 3 -> 5; 4 -> 7; else -> 9 }
        _uiState.update { it.copy(difficulty = difficulty, totalRounds = totalRounds, currentRound = 1, score = 0) }
        nextRound(1)
    }

    private fun nextRound(round: Int) {
        viewModelScope.launch {
            val seqLength = difficultyEngine.getSequenceLength(_uiState.value.difficulty) + (round - 1)
            val sequence = List(seqLength) { (0..3).random() }
            _uiState.update {
                it.copy(
                    sequence = sequence,
                    playerInput = emptyList(),
                    phase = SequencePhase.SHOWING,
                    currentRound = round
                )
            }
            // Play sequence with delay between each
            delay(600)
            sequence.forEach { buttonIndex ->
                _uiState.update { it.copy(litButton = buttonIndex) }
                delay(500 + (200L * (3 - _uiState.value.difficulty).coerceAtLeast(0)))
                _uiState.update { it.copy(litButton = null) }
                delay(300)
            }
            _uiState.update { it.copy(phase = SequencePhase.WAITING) }
            inputStartTime = System.currentTimeMillis()
        }
    }

    fun onButtonPressed(buttonIndex: Int) {
        val state = _uiState.value
        if (state.phase != SequencePhase.WAITING) return

        viewModelScope.launch {
            // Flash the button
            _uiState.update { it.copy(litButton = buttonIndex) }
            delay(200)
            _uiState.update { it.copy(litButton = null) }

            val newInput = state.playerInput + buttonIndex
            val expectedButton = state.sequence[newInput.size - 1]
            totalAttempts++

            if (buttonIndex != expectedButton) {
                // Wrong! 
                _uiState.update { it.copy(playerInput = newInput, phase = SequencePhase.FAILED) }
                delay(2000)
                // Continue to next round anyway (dementia support — don't punish)
                if (state.currentRound < state.totalRounds) {
                    nextRound(state.currentRound + 1)
                } else {
                    completeGame()
                }
            } else {
                // Correct so far
                val now = System.currentTimeMillis()
                responseTimes.add(now - inputStartTime)
                inputStartTime = now
                correctAttempts++

                if (newInput.size == state.sequence.size) {
                    // Round complete!
                    val roundScore = 50 * state.difficulty
                    _uiState.update { it.copy(playerInput = newInput, phase = SequencePhase.SUCCESS, score = state.score + roundScore) }
                    delay(1500)
                    if (state.currentRound < state.totalRounds) {
                        nextRound(state.currentRound + 1)
                    } else {
                        completeGame()
                    }
                } else {
                    _uiState.update { it.copy(playerInput = newInput) }
                }
            }
        }
    }

    private fun completeGame() {
        _uiState.update { it.copy(phase = SequencePhase.COMPLETE, isGameComplete = true) }
        viewModelScope.launch {
            val duration = System.currentTimeMillis() - startTime
            val accuracy = if (totalAttempts > 0) (correctAttempts.toFloat() / totalAttempts.toFloat()) * 100f else 0f
            val avgResponse = if (responseTimes.isNotEmpty()) responseTimes.average().toLong() else 0L
            gameRepository.saveSession(
                GameSession(
                    patientId = patientId,
                    gameType = GameType.SEQUENCE_RECALL,
                    difficultyLevel = _uiState.value.difficulty,
                    score = _uiState.value.score,
                    maxPossibleScore = _uiState.value.totalRounds * 50 * _uiState.value.difficulty,
                    accuracyPercent = accuracy,
                    avgResponseTimeMs = avgResponse,
                    durationMs = duration
                )
            )
        }
    }
}
