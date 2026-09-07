package com.nercare.cogcare.presentation.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.ai.CognitiveScoreCalculator
import com.nercare.cogcare.data.repository.GameRepository
import com.nercare.cogcare.data.repository.PatientRepository
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.GameType
import com.nercare.cogcare.domain.model.TrendDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.nercare.cogcare.data.repository.ReminderRepository
import com.nercare.cogcare.domain.model.Reminder
import kotlinx.coroutines.flow.first

data class CaregiverUiState(
    val patientName: String = "",
    val patientId: String = "",
    val username: String = "",
    val age: Int = 0,
    val city: String = "",
    val language: String = "",
    val diagnosisStage: String = "",
    val overallScore: Int = 0,
    val sessionsThisWeek: Int = 0,
    val avgAccuracy: Float = 0f,
    val prevWeekAccuracy: Float = 0f,
    val weeklyTrend: TrendDirection = TrendDirection.STABLE,
    val gameBreakdown: Map<GameType, Float> = emptyMap(),
    val recentSessions: List<GameSession> = emptyList(),
    val alertMessages: List<String> = emptyList(),
    val escalatedReminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CaregiverViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val gameRepository: GameRepository,
    private val reminderRepository: ReminderRepository,
    private val scoreCalculator: CognitiveScoreCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaregiverUiState())
    val uiState: StateFlow<CaregiverUiState> = _uiState

    fun loadDashboard(patientId: String) {
        viewModelScope.launch {
            val patient = patientRepository.getPatientById(patientId) ?: return@launch
            val allSessions = gameRepository.getRecentSessions(patientId, 50)

            val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            val twoWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14)

            val thisWeekSessions = allSessions.filter { it.completedAt >= oneWeekAgo }
            val prevWeekSessions = allSessions.filter { it.completedAt in twoWeeksAgo until oneWeekAgo }

            val report = scoreCalculator.calculateWeeklyReport(patientId, thisWeekSessions, prevWeekSessions)
            val overallScore = scoreCalculator.computeOverallScore(allSessions.take(20))

            val prevWeekAvg = if (prevWeekSessions.isEmpty()) report.score.averageAccuracy
            else prevWeekSessions.map { it.accuracyPercent }.average().toFloat()

            val allReminders = try { reminderRepository.getAllReminders(patientId).first() } catch (_: Exception) { emptyList() }
            val calendar = java.util.Calendar.getInstance()
            val currentMinutesOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)

            val escalated = allReminders.filter { reminder ->
                if (!reminder.isActive || reminder.isAcknowledged) return@filter false
                val remMinutesOfDay = reminder.hour * 60 + reminder.minute
                currentMinutesOfDay >= remMinutesOfDay + 30
            }

            _uiState.update {
                it.copy(
                    patientName = patient.name,
                    patientId = patient.id,
                    username = patient.username,
                    age = patient.age,
                    city = patient.city,
                    language = patient.language,
                    diagnosisStage = patient.diagnosisStage.label,
                    overallScore = overallScore,
                    sessionsThisWeek = report.sessionFrequency,
                    avgAccuracy = report.score.averageAccuracy,
                    prevWeekAccuracy = prevWeekAvg,
                    weeklyTrend = report.score.trendDirection,
                    gameBreakdown = report.breakdown,
                    recentSessions = allSessions.take(10),
                    alertMessages = report.alertMessages,
                    escalatedReminders = escalated,
                    isLoading = false
                )
            }
        }
    }

    private val _generatedPassword = MutableStateFlow<String?>(null)
    val generatedPassword: StateFlow<String?> = _generatedPassword

    fun resetPatientPassword(patientId: String) {
        viewModelScope.launch {
            val pwd = patientRepository.resetPatientPassword(patientId)
            _generatedPassword.value = pwd
        }
    }

    fun dismissPasswordDialog() {
        _generatedPassword.value = null
    }

    fun deletePatient(patientId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val patient = patientRepository.getPatientById(patientId) ?: return@launch
            patientRepository.deletePatient(patient)
            onSuccess()
        }
    }
}
