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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatternUiState(
    val questions: List<PatternQuestion> = emptyList(),
    val currentQ: Int = 0,
    val currentQuestion: PatternQuestion? = null,
    val selectedAnswer: String? = null,
    val showFeedback: Boolean = false,
    val score: Int = 0,
    val correctCount: Int = 0,
    val accuracy: Float = 0f,
    val totalQuestions: Int = 8,
    val isGameComplete: Boolean = false,
    val difficulty: Int = 2
)

@HiltViewModel
class PatternViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val difficultyEngine: AdaptiveDifficultyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatternUiState())
    val uiState: StateFlow<PatternUiState> = _uiState

    private var patientId = ""
    private var startTime = 0L
    private val responseTimes = mutableListOf<Long>()
    private var lastAnswerTime = 0L

    fun startGame(patientId: String, difficulty: Int) {
        this.patientId = patientId
        startTime = System.currentTimeMillis()
        lastAnswerTime = startTime

        val numChoices = difficultyEngine.getPatternChoices(difficulty)
        val totalQ = if (difficulty <= 2) 5 else 8
        val questions = generateQuestions(numChoices, totalQ)

        _uiState.update {
            it.copy(
                questions = questions,
                currentQ = 0,
                currentQuestion = questions.firstOrNull(),
                score = 0,
                correctCount = 0,
                accuracy = 0f,
                totalQuestions = totalQ,
                difficulty = difficulty,
                isGameComplete = false
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
        val newScore = if (isCorrect) state.score + (20 * state.difficulty) else state.score
        val newQ = state.currentQ + 1

        _uiState.update {
            it.copy(selectedAnswer = answer, showFeedback = true, score = newScore, correctCount = newCorrect)
        }

        viewModelScope.launch {
            delay(1000)
            if (newQ >= state.totalQuestions) {
                val accuracy = (newCorrect.toFloat() / state.totalQuestions.toFloat()) * 100f
                _uiState.update {
                    it.copy(
                        isGameComplete = true,
                        accuracy = accuracy,
                        showFeedback = false
                    )
                }
                saveSession(state.difficulty, newScore, newQ, newCorrect)
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

    private fun generateQuestions(numChoices: Int, total: Int): List<PatternQuestion> {
        return NER_PATTERN_SETS.shuffled().take(total).map { patternSet ->
            val answer = patternSet.last()
            val distractors = (NER_PATTERN_SETS.flatten().toSet() - answer - patternSet.toSet())
                .shuffled().take(numChoices - 1)
            val choices = (listOf(answer) + distractors).shuffled()
            PatternQuestion(
                sequence = patternSet.dropLast(1),
                answer = answer,
                choices = choices
            )
        }
    }

    private suspend fun saveSession(difficulty: Int, score: Int, total: Int, correct: Int) {
        val duration = System.currentTimeMillis() - startTime
        val avgResponse = if (responseTimes.isNotEmpty()) responseTimes.average().toLong() else 0L
        val accuracy = (correct.toFloat() / total.toFloat()) * 100f
        gameRepository.saveSession(
            GameSession(
                patientId = patientId,
                gameType = GameType.PATTERN_MATCHING,
                difficultyLevel = difficulty,
                score = score,
                maxPossibleScore = total * 20 * difficulty,
                accuracyPercent = accuracy,
                avgResponseTimeMs = avgResponse,
                durationMs = duration
            )
        )
    }
}
