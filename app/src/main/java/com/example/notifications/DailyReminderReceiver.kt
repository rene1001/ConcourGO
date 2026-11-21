package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = PreferencesRepository(appContext).getCurrentPreferences()
                if (prefs.notificationsEnabled) {
                    val stats = AppDatabase.getInstance(appContext).concoursDao().getUserStats()
                    NotificationHelper.showMotivationalNotification(
                        context = appContext,
                        streak = stats?.currentStreak ?: 0,
                        totalXp = stats?.totalXp ?: 0,
                        isTest = false
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
