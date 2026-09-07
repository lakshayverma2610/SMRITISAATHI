package com.nercare.cogcare.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nercare.cogcare.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReminderScheduler uses AlarmManager to schedule exact repeating reminders.
 * Handles medicine, hydration, appointments, and daily activity reminders.
 * Re-schedules on BOOT_COMPLETED via BootReceiver.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ReminderScheduler"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_PATIENT_ID = "patient_id"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_DESC = "reminder_description"
        const val EXTRA_PATIENT_NAME = "patient_name"
        const val EXTRA_REMINDER_HOUR = "reminder_hour"
        const val EXTRA_REMINDER_MINUTE = "reminder_minute"
        const val EXTRA_REPEAT_DAYS = "repeat_days"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: Reminder, patientName: String) {
        if (!reminder.isActive) {
            cancelReminder(reminder)
            return
        }

        // Schedule for each active day
        reminder.repeatDays.forEach { dayOfWeek ->
            val requestCode = "${reminder.id}_$dayOfWeek".hashCode()
            val intent = createReminderIntent(reminder, patientName, requestCode)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = getNextTriggerTime(reminder.hour, reminder.minute, dayOfWeek)

            try {
                val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()
                if (!canUseExact) throw SecurityException("Exact alarm access not granted")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled reminder: ${reminder.title} for day $dayOfWeek at ${reminder.hour}:${reminder.minute}")
            } catch (e: SecurityException) {
                Log.e(TAG, "Cannot schedule exact alarm: ${e.message}")
                // Fallback to inexact alarm
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
    }

    fun cancelReminder(reminder: Reminder) {
        reminder.repeatDays.forEach { dayOfWeek ->
            val requestCode = "${reminder.id}_$dayOfWeek".hashCode()
            val intent = createReminderIntent(reminder, "", requestCode)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
        Log.d(TAG, "Cancelled reminder: ${reminder.id}")
    }

    private fun createReminderIntent(reminder: Reminder, patientName: String, requestCode: Int): Intent {
        return Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.nercare.cogcare.REMINDER_TRIGGER"
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_PATIENT_ID, reminder.patientId)
            putExtra(EXTRA_REMINDER_TYPE, reminder.type.name)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_DESC, reminder.description)
            putExtra(EXTRA_PATIENT_NAME, patientName)
            putExtra(EXTRA_REMINDER_HOUR, reminder.hour)
            putExtra(EXTRA_REMINDER_MINUTE, reminder.minute)
            putIntegerArrayListExtra(EXTRA_REPEAT_DAYS, ArrayList(reminder.repeatDays))
        }
    }

    private fun getNextTriggerTime(hour: Int, minute: Int, dayOfWeek: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Map 1=Mon..7=Sun to Calendar constants
            val calDay = when (dayOfWeek) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                7 -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }
            set(Calendar.DAY_OF_WEEK, calDay)
            // If time already passed this week, advance by 7 days
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
