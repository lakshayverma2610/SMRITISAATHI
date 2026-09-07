package com.nercare.cogcare

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CogCareApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Medicine reminder channel
            NotificationChannel(
                CHANNEL_MEDICINE,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take medicines on time"
                enableVibration(true)
                setSound(sound, audioAttributes)
                notificationManager.createNotificationChannel(this)
            }

            // Hydration reminder channel
            NotificationChannel(
                CHANNEL_HYDRATION,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to drink water"
                setSound(sound, audioAttributes)
                notificationManager.createNotificationChannel(this)
            }

            // Appointment reminder channel
            NotificationChannel(
                CHANNEL_APPOINTMENT,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medical appointment reminders"
                enableVibration(true)
                setSound(sound, audioAttributes)
                notificationManager.createNotificationChannel(this)
            }

            // Activity reminder channel
            NotificationChannel(
                CHANNEL_ACTIVITY,
                "Daily Activity Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily routine and activity reminders"
                setSound(sound, audioAttributes)
                notificationManager.createNotificationChannel(this)
            }

            // Caregiver alert channel
            NotificationChannel(
                CHANNEL_CAREGIVER_ALERT,
                "Caregiver Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for caregivers about patient status"
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_MEDICINE = "channel_medicine_v2"
        const val CHANNEL_HYDRATION = "channel_hydration_v2"
        const val CHANNEL_APPOINTMENT = "channel_appointment_v2"
        const val CHANNEL_ACTIVITY = "channel_activity_v2"
        const val CHANNEL_CAREGIVER_ALERT = "channel_caregiver_alert"
    }
}
