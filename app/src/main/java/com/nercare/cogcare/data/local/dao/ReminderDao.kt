package com.nercare.cogcare.data.local.dao

import androidx.room.*
import com.nercare.cogcare.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE patientId = :patientId AND isActive = 1 ORDER BY hour, minute")
    fun getActiveRemindersForPatient(patientId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE patientId = :patientId ORDER BY hour, minute")
    fun getAllRemindersForPatient(patientId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    suspend fun getAllActiveReminders(): List<ReminderEntity>

    @Query("UPDATE reminders SET isActive = :isActive WHERE id = :id")
    suspend fun setReminderActive(id: String, isActive: Boolean)

    @Query("UPDATE reminders SET isAcknowledged = :isAcknowledged, lastAcknowledgedAt = :timestamp WHERE id = :id")
    suspend fun acknowledgeReminder(id: String, isAcknowledged: Boolean, timestamp: Long)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)
}
