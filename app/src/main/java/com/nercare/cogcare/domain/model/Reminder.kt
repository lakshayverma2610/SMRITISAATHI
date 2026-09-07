package com.nercare.cogcare.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: String = "",
    val patientId: String = "",
    val type: ReminderType = ReminderType.MEDICINE,
    val title: String = "",
    val description: String = "",
    val scheduledTime: Long = 0L, // millis of day (hour * 60 + min) * 60 * 1000
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon..7=Sun
    val isActive: Boolean = true,
    val isAcknowledged: Boolean = false,
    val lastAcknowledgedAt: Long? = null,
    val medicineName: String = "",
    val medicineDosage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReminderType(val displayName: String, val emoji: String, val channelId: String) {
    MEDICINE("Medicine", "💊", "channel_medicine_v2"),
    HYDRATION("Drink Water", "💧", "channel_hydration_v2"),
    APPOINTMENT("Appointment", "🏥", "channel_appointment_v2"),
    ACTIVITY("Daily Activity", "🌟", "channel_activity_v2")
}
