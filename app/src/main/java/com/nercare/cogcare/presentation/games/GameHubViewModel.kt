package com.nercare.cogcare.presentation.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.ai.AdaptiveDifficultyEngine
import com.nercare.cogcare.data.repository.GameRepository
import com.nercare.cogcare.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameHubUiState(
    val recommendation: AdaptiveDifficultyEngine.DifficultyRecommendation? = null,
    val recommendedDifficulty: Int = 2
)

@HiltViewModel
class GameHubViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val gameRepository: GameRepository,
    private val difficultyEngine: AdaptiveDifficultyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameHubUiState())
    val uiState: StateFlow<GameHubUiState> = _uiState

    fun loadRecommendation(patientId: String) {
        viewModelScope.launch {
            val patient = patientRepository.getPatientById(patientId) ?: return@launch
            val recentSessions = gameRepository.getRecentSessions(patientId, 5)
            val recommendation = difficultyEngine.recommend(
                recentSessions = recentSessions,
                cognitiveStage = patient.diagnosisStage
            )
            _uiState.update {
                it.copy(
                    recommendation = recommendation,
                    recommendedDifficulty = recommendation.level
                )
            }
        }
    }
}
