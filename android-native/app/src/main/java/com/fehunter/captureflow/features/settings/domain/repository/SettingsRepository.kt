package com.fehunter.captureflow.features.settings.domain.repository

interface SettingsRepository {
    fun getThemeMode(): String
    fun setThemeMode(mode: String)
}
