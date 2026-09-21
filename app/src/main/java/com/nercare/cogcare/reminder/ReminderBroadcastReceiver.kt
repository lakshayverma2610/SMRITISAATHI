package com.nercare.cogcare.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nercare.cogcare.CogCareApp
import com.nercare.cogcare.MainActivity
import com.nercare.cogcare.R
import com.nercare.cogcare.domain.model.ReminderType

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
        val patientId = intent.getStringExtra(ReminderScheduler.EXTRA_PATIENT_ID).orEmpty()
        val reminderTypeName = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE) ?: "ACTIVITY"
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "Reminder"
        val description = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_DESC) ?: ""
        val patientName = intent.getStringExtra(ReminderScheduler.EXTRA_PATIENT_NAME) ?: ""

        val reminderType = try {
            ReminderType.valueOf(reminderTypeName)
        } catch (e: IllegalArgumentException) {
            ReminderType.ACTIVITY
        }

        // 1. Show the standard notification
        showNotification(context, reminderId, patientId, reminderType, title, description, patientName)

        // 2. Trigger Voice Reminder TTS using WorkManager
        val workData = Data.Builder()
            .putString("title", title)
            .putString("patientName", patientName)
            .build()
        val voiceWorkRequest = OneTimeWorkRequestBuilder<VoiceReminderWorker>()
            .setInputData(workData)
            .build()
        WorkManager.getInstance(context).enqueue(voiceWorkRequest)

        // AlarmManager alarms are one-shot. Re-arm the weekly schedule after firing.
        val repeatDays = intent.getIntegerArrayListExtra(ReminderScheduler.EXTRA_REPEAT_DAYS).orEmpty()
        if (repeatDays.isNotEmpty()) {
            ReminderScheduler(context.applicationContext).scheduleReminder(
                com.nercare.cogcare.domain.model.Reminder(
                    id = reminderId,
                    patientId = patientId,
                    type = reminderType,
                    title = title,
                    description = description,
                    hour = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_HOUR, 8),
                    minute = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_MINUTE, 0),
                    repeatDays = repeatDays,
                    isActive = true
                ),
                patientName
            )
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: String,
        patientId: String,
        type: ReminderType,
        title: String,
        description: String,
        patientName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_screen", "reminders")
            putExtra(ReminderScheduler.EXTRA_PATIENT_ID, patientId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val displayTitle = if (patientName.isNotEmpty()) "$patientName — $title" else title
        val displayText = description.ifEmpty { "${type.emoji} ${type.displayName}" }

        val notification = NotificationCompat.Builder(context, type.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(reminderId.hashCode(), notification)
    }
}
