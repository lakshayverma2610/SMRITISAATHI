package com.nercare.cogcare.presentation.games.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class RoutineUiState(
    val questions: List<RoutineQuestion> = emptyList(),
    val currentQ: Int = 0,
    val currentQuestion: RoutineQuestion? = null,
    val selectedAnswer: String? = null,
    val showFeedback: Boolean = false,
    val showHint: Boolean = false,
    val score: Int = 0,
    val correctCount: Int = 0,
    val accuracy: Float = 0f,
    val totalQuestions: Int = 8,
    val isGameComplete: Boolean = false,
    val difficulty: Int = 2
)

@HiltViewModel
class DailyRoutineViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState

    private var patientId = ""
    private var startTime = 0L
    private val responseTimes = mutableListOf<Long>()
    private var lastAnswerTime = 0L

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        startTime = System.currentTimeMillis()
        lastAnswerTime = startTime
        val totalQ = if (difficulty <= 1) 5 else if (difficulty <= 3) 7 else 10
        val questions = DAILY_ROUTINE_QUESTIONS.shuffled().take(totalQ)
        _uiState.update {
            it.copy(
                questions = questions,
                currentQ = 0,
                currentQuestion = questions.firstOrNull(),
                score = 0, correctCount = 0, accuracy = 0f,
                totalQuestions = totalQ, difficulty = difficulty,
                isGameComplete = false, showHint = false,
                selectedAnswer = null, showFeedback = false
            )
        }
    }

    fun onAnswerSelected(answer: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val now = System.currentTimeMillis()
        responseTimes.add(now - lastAnswerTime)
        lastAnswerTime = now
        val isCorrect = answer == question.answer
        val newCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount
        val newScore = if (isCorrect) state.score + (10 * state.difficulty) else state.score
        val newQ = state.currentQ + 1
        _uiState.update { it.copy(selectedAnswer = answer, showFeedback = true, score = newScore, correctCount = newCorrect) }
        viewModelScope.launch {
            delay(1200)
            if (newQ >= state.totalQuestions) {
                val accuracy = (newCorrect.toFloat() / state.totalQuestions.toFloat()) * 100f
                _uiState.update { it.copy(isGameComplete = true, accuracy = accuracy, showFeedback = false) }
                saveSession(state.difficulty, newScore, state.totalQuestions, newCorrect)
            } else {
                _uiState.update {
                    it.copy(
                        currentQ = newQ,
                        currentQuestion = state.questions[newQ],
                        selectedAnswer = null,
                        showFeedback = false,
                        accuracy = (newCorrect.toFloat() / newQ.toFloat()) * 100f
                    )
                }
            }
        }
    }

    private suspend fun saveSession(difficulty: Int, score: Int, total: Int, correct: Int) {
        val duration = System.currentTimeMillis() - startTime
        val avgResponse = if (responseTimes.isNotEmpty()) responseTimes.average().toLong() else 0L
        val accuracy = (correct.toFloat() / total.toFloat()) * 100f
        gameRepository.saveSession(
            GameSession(
                patientId = patientId,
                gameType = GameType.DAILY_ROUTINE,
                difficultyLevel = difficulty,
                score = score,
                maxPossibleScore = total * 10 * difficulty,
                accuracyPercent = accuracy,
                avgResponseTimeMs = avgResponse,
                durationMs = duration
            )
        )
    }
}
