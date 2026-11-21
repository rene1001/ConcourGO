package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "concoursgo_prefs")

data class UserPreferences(
    val offlineModeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val offlineGuestEntered: Boolean = false,
    val hasPremiumPass: Boolean = false,
    val pendingPaymentJson: String = "",
    val lastPaymentReceipt: String = ""
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val OFFLINE_GUEST_ENTERED = booleanPreferencesKey("offline_guest_entered")
        val HAS_PREMIUM_PASS = booleanPreferencesKey("has_premium_pass")
        val PENDING_PAYMENT_JSON = stringPreferencesKey("pending_payment_json")
        val LAST_PAYMENT_RECEIPT = stringPreferencesKey("last_payment_receipt")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            offlineModeEnabled = prefs[Keys.OFFLINE_MODE] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 19,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            offlineGuestEntered = prefs[Keys.OFFLINE_GUEST_ENTERED] ?: false,
            hasPremiumPass = prefs[Keys.HAS_PREMIUM_PASS] ?: false,
            pendingPaymentJson = prefs[Keys.PENDING_PAYMENT_JSON] ?: "",
            lastPaymentReceipt = prefs[Keys.LAST_PAYMENT_RECEIPT] ?: ""
        )
    }

    suspend fun getCurrentPreferences(): UserPreferences = preferencesFlow.first()

    suspend fun setOfflineModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setOfflineGuestEntered(entered: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_GUEST_ENTERED] = entered }
    }

    suspend fun setPremiumPass(active: Boolean) {
        context.dataStore.edit { it[Keys.HAS_PREMIUM_PASS] = active }
    }

    suspend fun setPendingPaymentJson(json: String) {
        context.dataStore.edit { it[Keys.PENDING_PAYMENT_JSON] = json }
    }

    suspend fun setLastPaymentReceipt(summary: String) {
        context.dataStore.edit { it[Keys.LAST_PAYMENT_RECEIPT] = summary }
    }
}
