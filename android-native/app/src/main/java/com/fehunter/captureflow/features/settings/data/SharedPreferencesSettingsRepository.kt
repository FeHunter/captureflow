package com.fehunter.captureflow.features.settings.data

import android.content.Context
import com.fehunter.captureflow.features.settings.domain.repository.SettingsRepository

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "captureflow_preferences",
        Context.MODE_PRIVATE,
    )

    override fun getThemeMode(): String =
        preferences.getString("theme_mode", "system") ?: "system"

    override fun setThemeMode(mode: String) {
        val normalized = mode.takeIf { it in setOf("system", "light", "dark") } ?: "system"
        preferences.edit().putString("theme_mode", normalized).apply()
    }
}
