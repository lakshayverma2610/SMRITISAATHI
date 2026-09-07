package com.nercare.cogcare.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nercare.cogcare.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BootReceiver re-schedules all active reminders after device reboot.
 * This is critical for AlarmManager-based reminders which don't persist across reboots.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("BootReceiver", "Device rebooted — re-scheduling all reminders")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = reminderRepository.getAllActiveReminders()
                reminders.forEach { reminder ->
                    reminderScheduler.scheduleReminder(reminder, "")
                }
                Log.d("BootReceiver", "Re-scheduled ${reminders.size} reminders")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
