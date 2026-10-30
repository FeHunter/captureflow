package com.fehunter.captureflow.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.settings.domain.usecase.GetThemeModeUseCase
import com.fehunter.captureflow.features.settings.domain.usecase.SetThemeModeUseCase

class SettingsViewModelFactory(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(getThemeMode, setThemeMode) as T
    }
}
