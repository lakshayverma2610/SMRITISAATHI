package com.nercare.cogcare.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.ReminderRepository
import com.nercare.cogcare.domain.model.Reminder
import com.nercare.cogcare.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    fun loadReminders(patientId: String) {
        viewModelScope.launch {
            reminderRepository.getAllReminders(patientId)
                .collect { reminders ->
                    _uiState.update { it.copy(reminders = reminders, isLoading = false) }
                }
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val newActive = !reminder.isActive
            reminderRepository.setActive(reminder.id, newActive)
            if (newActive) {
                reminderScheduler.scheduleReminder(reminder.copy(isActive = true), "")
            } else {
                reminderScheduler.cancelReminder(reminder)
            }
        }
    }

    fun deleteReminder(reminderId: String, patientId: String) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderId)
            reminder?.let { reminderScheduler.cancelReminder(it) }
            reminderRepository.deleteReminder(reminderId)
        }
    }

    fun acknowledgeReminder(reminder: Reminder) {
        viewModelScope.launch {
            val newAck = !reminder.isAcknowledged
            reminderRepository.acknowledgeReminder(reminder.id, newAck)
        }
    }
}
