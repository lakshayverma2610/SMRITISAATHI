package com.nercare.cogcare.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nercare.cogcare.data.repository.ReminderRepository
import com.nercare.cogcare.domain.model.Reminder
import com.nercare.cogcare.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    fun loadReminder(reminderId: String, onLoaded: (Reminder) -> Unit) {
        viewModelScope.launch {
            reminderRepository.getReminderById(reminderId)?.let(onLoaded)
        }
    }

    fun saveReminder(reminder: Reminder, onSaved: () -> Unit) {
        viewModelScope.launch {
            if (reminder.id.isNotBlank()) {
                reminderRepository.getReminderById(reminder.id)?.let(reminderScheduler::cancelReminder)
            }
            val saved = reminderRepository.saveReminder(reminder)
            if (saved.isActive) {
                reminderScheduler.scheduleReminder(saved, "")
            }
            onSaved()
        }
    }
}
