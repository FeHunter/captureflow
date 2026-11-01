package com.fehunter.captureflow.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.fehunter.captureflow.features.settings.domain.usecase.GetThemeModeUseCase
import com.fehunter.captureflow.features.settings.domain.usecase.SetThemeModeUseCase
import com.fehunter.captureflow.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    getThemeMode: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val preferencesRepository: SettingsRepository,
) : ViewModel() {
    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode = _themeMode.asStateFlow()

    private val _categorySortOrder = MutableStateFlow(preferencesRepository.getCategorySortOrder())
    val categorySortOrder = _categorySortOrder.asStateFlow()
    private val _captureSortOrder = MutableStateFlow(preferencesRepository.getCaptureSortOrder())
    val captureSortOrder = _captureSortOrder.asStateFlow()
    private val _imageCompressionProfile = MutableStateFlow(preferencesRepository.getImageCompressionProfile())
    val imageCompressionProfile = _imageCompressionProfile.asStateFlow()

    fun setCategorySortOrder(order: String) {
        val normalized = order.takeIf { it in setOf("manual", "newest", "oldest", "name_asc", "name_desc") } ?: "manual"
        preferencesRepository.setCategorySortOrder(normalized)
        _categorySortOrder.value = normalized
    }

    fun setCaptureSortOrder(order: String) {
        val normalized = order.takeIf { it in setOf("newest", "oldest", "title_asc", "title_desc") } ?: "newest"
        preferencesRepository.setCaptureSortOrder(normalized)
        _captureSortOrder.value = normalized
    }

    fun setImageCompressionProfile(profile: String) {
        val normalized = profile.takeIf { it in setOf("MEDIUM", "HIGH") } ?: "MEDIUM"
        preferencesRepository.setImageCompressionProfile(normalized)
        _imageCompressionProfile.value = normalized
    }

    fun setThemeMode(mode: String) {
        val normalized = mode.takeIf { it in setOf("system", "light", "dark") } ?: "system"
        setThemeModeUseCase(normalized)
        _themeMode.value = normalized
    }
}
