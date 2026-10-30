package com.fehunter.captureflow.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.fehunter.captureflow.features.settings.domain.usecase.GetThemeModeUseCase
import com.fehunter.captureflow.features.settings.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    getThemeMode: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
) : ViewModel() {
    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        val normalized = mode.takeIf { it in setOf("system", "light", "dark") } ?: "system"
        setThemeModeUseCase(normalized)
        _themeMode.value = normalized
    }
}
