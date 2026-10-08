package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_DEFAULT_REMINDER_MINUTES = "default_reminder_minutes"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SELECTED_MODEL = "selected_gemini_model"
        const val DEFAULT_MODEL = "gemini-3.1-flash-lite-preview"
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, model.trim()).apply()
    }

    fun getApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
        if (customKey.isNotEmpty()) {
            return customKey
        }
        // Fallback to BuildConfig if configured and not placeholder
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun getCustomApiKeyOnly(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun getDefaultReminderMinutes(): Int {
        return prefs.getInt(KEY_DEFAULT_REMINDER_MINUTES, 15)
    }

    fun setDefaultReminderMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DEFAULT_REMINDER_MINUTES, minutes).apply()
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun saveUserProfile(profile: com.example.data.model.UserProfile) {
        prefs.edit()
            .putString("user_uid", profile.uid)
            .putString("user_name", profile.displayName)
            .putString("user_email", profile.email)
            .putString("user_photo", profile.photoUrl ?: "")
            .putBoolean("user_is_google", profile.isGoogleLinked)
            .putLong("user_last_sync", profile.lastSyncTime ?: 0L)
            .apply()
    }

    fun getUserProfile(): com.example.data.model.UserProfile? {
        val email = prefs.getString("user_email", "") ?: ""
        if (email.isBlank()) return null
        val uid = prefs.getString("user_uid", "user_${System.currentTimeMillis()}") ?: ""
        val name = prefs.getString("user_name", "Sinh viên") ?: "Sinh viên"
        val photo = prefs.getString("user_photo", "") ?: ""
        val isGoogle = prefs.getBoolean("user_is_google", true)
        val lastSync = prefs.getLong("user_last_sync", 0L)
        return com.example.data.model.UserProfile(
            uid = uid,
            displayName = name,
            email = email,
            photoUrl = photo.ifBlank { null },
            isGoogleLinked = isGoogle,
            lastSyncTime = if (lastSync > 0) lastSync else null
        )
    }

    fun updateLastCalendarSyncTime(time: Long) {
        prefs.edit().putLong("user_last_sync", time).apply()
    }

    fun clearUserProfile() {
        prefs.edit()
            .remove("user_uid")
            .remove("user_name")
            .remove("user_email")
            .remove("user_photo")
            .remove("user_is_google")
            .remove("user_last_sync")
            .apply()
    }
}
