package com.nercare.cogcare.data.repository

import com.nercare.cogcare.data.local.dao.ReminderDao
import com.nercare.cogcare.data.local.entities.toEntity
import com.nercare.cogcare.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {
    fun getActiveReminders(patientId: String): Flow<List<Reminder>> =
        reminderDao.getActiveRemindersForPatient(patientId).map { list -> list.map { it.toDomain() } }

    fun getAllReminders(patientId: String): Flow<List<Reminder>> =
        reminderDao.getAllRemindersForPatient(patientId).map { list -> list.map { it.toDomain() } }

    suspend fun getAllActiveReminders(): List<Reminder> =
        reminderDao.getAllActiveReminders().map { it.toDomain() }

    suspend fun saveReminder(reminder: Reminder): Reminder {
        val toSave = if (reminder.id.isEmpty()) {
            reminder.copy(id = UUID.randomUUID().toString())
        } else reminder
        reminderDao.upsertReminder(toSave.toEntity())
        return toSave
    }

    suspend fun setActive(reminderId: String, isActive: Boolean) {
        reminderDao.setReminderActive(reminderId, isActive)
    }

    suspend fun acknowledgeReminder(reminderId: String, isAcknowledged: Boolean) {
        reminderDao.acknowledgeReminder(reminderId, isAcknowledged, System.currentTimeMillis())
    }

    suspend fun deleteReminder(reminderId: String) {
        reminderDao.deleteReminderById(reminderId)
    }

    suspend fun getReminderById(id: String): Reminder? =
        reminderDao.getReminderById(id)?.toDomain()
}
