package com.nercare.cogcare.presentation.games.routine

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject

data class RoutineUiState(
    val fullRoutine: List<String> = emptyList(),
    val availableTasks: List<String> = emptyList(),
    val userSequence: List<String> = emptyList(),
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val message: String = "What happens next?",
    val startTime: Long = 0L,
    val round: Int = 1,
    val totalRounds: Int = 3
)

@HiltViewModel
class DailyRoutineViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val adaptiveDifficultyEngine: AdaptiveDifficultyEngine,
    private val geminiContentGenerator: com.nercare.cogcare.ai.gemini.GeminiContentGenerator,
    private val gameContentDao: com.nercare.cogcare.data.local.dao.GameContentDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    private var patientId: String = ""
    private var baseDifficulty: Int = 1

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        this.baseDifficulty = difficulty
        
        viewModelScope.launch {
            val history = gameRepository.getRecentSessionsByGame(patientId, GameType.DAILY_ROUTINE)
            val avgAcc = history.map { it.accuracyPercent }.average().toFloat().takeIf { !it.isNaN() } ?: 50f
            val avgTime = history.map { it.avgResponseTimeMs }.average().toLong().takeIf { it > 0 } ?: 2000L
            
            val recommendedDiff = adaptiveDifficultyEngine.recommendDifficulty(avgAcc, avgTime, 1, difficulty)
            
            _uiState.value = _uiState.value.copy(score = 0, round = 1, isGameOver = false, startTime = System.currentTimeMillis())
            startRound(recommendedDiff)
        }
    }

    private fun startRound(diff: Int) {
        // diff 1 -> 3 steps, diff 5 -> 5 steps
        val stepCount = minOf(5, maxOf(3, diff + 2))
        
        _uiState.value = _uiState.value.copy(
            message = "Generating dynamic routine...",
            fullRoutine = emptyList(),
            availableTasks = emptyList()
        )

        viewModelScope.launch {
            try {
                // 1. Fetch dynamic infinite routine from Gemini
                val dynamicRoutine = geminiContentGenerator.generateDailyRoutine(stepCount)
                _uiState.value = _uiState.value.copy(
                    fullRoutine = dynamicRoutine,
                    availableTasks = dynamicRoutine.shuffled(),
                    userSequence = emptyList(),
                    message = "Tap the tasks in the correct chronological order."
                )
            } catch (e: Exception) {
                // 2. Offline Fallback: Read from Room Database Cache
                try {
                    val cachedEntity = gameContentDao.getRandomUnusedContent(GameType.DAILY_ROUTINE.name)
                    if (cachedEntity != null) {
                        gameContentDao.markAsUsed(cachedEntity.id)
                        val routine = Json.decodeFromString<List<String>>(cachedEntity.contentJson).take(stepCount)
                        
                        _uiState.value = _uiState.value.copy(
                            fullRoutine = routine,
                            availableTasks = routine.shuffled(),
                            userSequence = emptyList(),
                            message = "Tap the tasks in the correct chronological order."
                        )
                    } else {
                        // Absolute worst-case scenario
                        val fallbackRoutine = listOf("Wake up", "Drink Water", "Brush Teeth", "Eat Breakfast", "Go outside").take(stepCount)
                        _uiState.value = _uiState.value.copy(
                            fullRoutine = fallbackRoutine,
                            availableTasks = fallbackRoutine.shuffled(),
                            userSequence = emptyList(),
                            message = "Connect to internet for more routines!"
                        )
                    }
                } catch (dbError: Exception) {
                    _uiState.value = _uiState.value.copy(message = "Database Error")
                }
            }
        }
    }

    fun onTaskSelected(task: String) {
        val state = _uiState.value
        if (state.isGameOver) return

        val expectedNextTask = state.fullRoutine[state.userSequence.size]
        
        if (task == expectedNextTask) {
            val newUserSequence = state.userSequence + task
            val newAvailable = state.availableTasks - task
            
            if (newAvailable.isEmpty()) {
                val newRound = state.round + 1
                if (newRound > state.totalRounds) {
                    _uiState.value = state.copy(score = state.score + 200, userSequence = newUserSequence, availableTasks = emptyList())
                    endGame(true)
                } else {
                    _uiState.value = state.copy(
                        score = state.score + 200,
                        userSequence = newUserSequence,
                        availableTasks = emptyList(),
                        round = newRound,
                        message = "Perfect! Getting ready for the next one..."
                    )
                    viewModelScope.launch {
                        delay(1500)
                        startRound(minOf(5, baseDifficulty + (newRound / 2)))
                    }
                }
            } else {
                _uiState.value = state.copy(
                    userSequence = newUserSequence,
                    availableTasks = newAvailable,
                    message = "Correct! What's next?"
                )
            }
        } else {
            _uiState.value = state.copy(message = "That doesn't happen next. Try again!")
            viewModelScope.launch {
                delay(1500)
                if (_uiState.value.message.startsWith("That doesn't")) {
                    _uiState.value = state.copy(message = "Tap the tasks in the correct chronological order.")
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
                gameType = GameType.DAILY_ROUTINE,
                difficultyLevel = baseDifficulty,
                score = state.score,
                maxPossibleScore = state.totalRounds * 200,
                accuracyPercent = if (completed) 100f else (state.round.toFloat() / state.totalRounds.toFloat()) * 100f,
                avgResponseTimeMs = duration / maxOf(1, state.round),
                durationMs = duration
            )
            gameRepository.saveSession(session)
            
            _uiState.value = state.copy(isGameOver = true, message = "Game Over! Score: ${state.score}")
        }
    }
}
