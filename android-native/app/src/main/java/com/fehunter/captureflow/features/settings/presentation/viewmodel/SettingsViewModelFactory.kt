package com.fehunter.captureflow.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.settings.domain.usecase.GetThemeModeUseCase
import com.fehunter.captureflow.features.settings.domain.usecase.SetThemeModeUseCase
import com.fehunter.captureflow.features.settings.domain.repository.SettingsRepository

class SettingsViewModelFactory(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val preferencesRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(getThemeMode, setThemeMode, preferencesRepository) as T
    }
}
