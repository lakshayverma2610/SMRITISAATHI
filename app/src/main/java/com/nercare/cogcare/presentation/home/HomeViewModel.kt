package com.nercare.cogcare.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.ai.AdaptiveDifficultyEngine
import com.nercare.cogcare.ai.CognitiveScoreCalculator
import com.nercare.cogcare.data.repository.GameRepository
import com.nercare.cogcare.data.repository.PatientRepository
import com.nercare.cogcare.data.repository.ReminderRepository
import com.nercare.cogcare.domain.model.Patient
import com.nercare.cogcare.domain.model.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val patient: Patient? = null,
    val overallScore: Int? = null,
    val nextReminder: Reminder? = null,
    val todayReminders: List<String> = emptyList(),
    val encouragementMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val gameRepository: GameRepository,
    private val reminderRepository: ReminderRepository,
    private val scoreCalculator: CognitiveScoreCalculator,
    private val difficultyEngine: AdaptiveDifficultyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadPatient(patientId: String) {
        viewModelScope.launch {
            patientRepository.observePatient(patientId)
                .combine(reminderRepository.getActiveReminders(patientId)) { patient, reminders ->
                    Pair(patient, reminders)
                }
                .collect { (patient, reminders) ->
                    if (patient != null) {
                        patientRepository.updateLastActive(patientId)

                        val recentSessions = gameRepository.getRecentSessions(patientId, 20)
                        val score = scoreCalculator.computeOverallScore(recentSessions)

                        val recommendation = difficultyEngine.recommend(
                            recentSessions = recentSessions,
                            cognitiveStage = patient.diagnosisStage
                        )

                        val nowCal = Calendar.getInstance()
                        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = nowCal.get(Calendar.MINUTE)
                        val currentMinutesFromMidnight = currentHour * 60 + currentMinute

                        val todayDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)
                        val calDay = when (todayDayOfWeek) {
                            Calendar.MONDAY -> 1
                            Calendar.TUESDAY -> 2
                            Calendar.WEDNESDAY -> 3
                            Calendar.THURSDAY -> 4
                            Calendar.FRIDAY -> 5
                            Calendar.SATURDAY -> 6
                            Calendar.SUNDAY -> 7
                            else -> 1
                        }

                        val activeToday = reminders.filter { it.repeatDays.contains(calDay) }
                        val todayReminderTitles = activeToday.map { "${it.type.emoji} ${it.title} at ${formatTime(it.hour, it.minute)}" }

                        // Next upcoming reminder today
                        val next = activeToday
                            .filter { (it.hour * 60 + it.minute) >= currentMinutesFromMidnight }
                            .minByOrNull { it.hour * 60 + it.minute }
                            ?: activeToday.minByOrNull { it.hour * 60 + it.minute }

                        _uiState.update {
                            it.copy(
                                patient = patient,
                                overallScore = score,
                                nextReminder = next,
                                todayReminders = todayReminderTitles,
                                encouragementMessage = recommendation.encouragementMessage,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        return "${h}:${minute.toString().padStart(2, '0')} $amPm"
    }
}
