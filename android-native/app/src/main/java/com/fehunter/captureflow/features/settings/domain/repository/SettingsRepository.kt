package com.fehunter.captureflow.features.settings.domain.repository

interface SettingsRepository {
    fun getThemeMode(): String
    fun setThemeMode(mode: String)
    fun getCategorySortOrder(): String
    fun setCategorySortOrder(order: String)
    fun getCaptureSortOrder(): String
    fun setCaptureSortOrder(order: String)
    fun getImageCompressionProfile(): String
    fun setImageCompressionProfile(profile: String)
}
