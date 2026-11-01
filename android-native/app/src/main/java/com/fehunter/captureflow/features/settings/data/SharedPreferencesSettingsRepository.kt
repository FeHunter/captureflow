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

    override fun getCategorySortOrder(): String =
        preferences.getString("category_sort_order", "manual") ?: "manual"

    override fun setCategorySortOrder(order: String) {
        val normalized = order.takeIf { it in setOf("manual", "name_asc", "name_desc") } ?: "manual"
        preferences.edit().putString("category_sort_order", normalized).apply()
    }

    override fun getCaptureSortOrder(): String =
        preferences.getString("capture_sort_order", "newest") ?: "newest"

    override fun setCaptureSortOrder(order: String) {
        val normalized = order.takeIf { it in setOf("newest", "oldest", "title_asc", "title_desc") } ?: "newest"
        preferences.edit().putString("capture_sort_order", normalized).apply()
    }

    override fun getImageCompressionProfile(): String =
        preferences.getString("image_compression_profile", "MEDIUM") ?: "MEDIUM"

    override fun setImageCompressionProfile(profile: String) {
        val normalized = profile.takeIf { it in setOf("MEDIUM", "HIGH") } ?: "MEDIUM"
        preferences.edit().putString("image_compression_profile", normalized).apply()
    }
}
