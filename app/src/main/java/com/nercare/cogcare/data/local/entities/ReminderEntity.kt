package com.nercare.cogcare.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nercare.cogcare.domain.model.Reminder
import com.nercare.cogcare.domain.model.ReminderType

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val type: String,
    val title: String,
    val description: String,
    val hour: Int,
    val minute: Int,
    val repeatDaysJson: String, // JSON array of ints
    val isActive: Boolean,
    val isAcknowledged: Boolean = false,
    val lastAcknowledgedAt: Long? = null,
    val medicineName: String,
    val medicineDosage: String,
    val createdAt: Long
) {
    fun toDomain() = Reminder(
        id = id,
        patientId = patientId,
        type = ReminderType.valueOf(type),
        title = title,
        description = description,
        hour = hour,
        minute = minute,
        repeatDays = repeatDaysJson.trim('[', ']').split(",")
            .map { it.trim().toIntOrNull() ?: 1 },
        isActive = isActive,
        isAcknowledged = isAcknowledged,
        lastAcknowledgedAt = lastAcknowledgedAt,
        medicineName = medicineName,
        medicineDosage = medicineDosage,
        createdAt = createdAt
    )
}

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    patientId = patientId,
    type = type.name,
    title = title,
    description = description,
    hour = hour,
    minute = minute,
    repeatDaysJson = "[${repeatDays.joinToString(",")}]",
    isActive = isActive,
    isAcknowledged = isAcknowledged,
    lastAcknowledgedAt = lastAcknowledgedAt,
    medicineName = medicineName,
    medicineDosage = medicineDosage,
    createdAt = createdAt
)
