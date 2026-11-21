package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = PreferencesRepository(appContext).getCurrentPreferences()
                if (prefs.notificationsEnabled) {
                    DailyReminderScheduler.scheduleDailyReminder(
                        context = appContext,
                        hour = prefs.reminderHour,
                        minute = prefs.reminderMinute
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
