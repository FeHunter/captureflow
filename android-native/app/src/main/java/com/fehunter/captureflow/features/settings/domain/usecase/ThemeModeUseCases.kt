package com.fehunter.captureflow.features.settings.domain.usecase

import com.fehunter.captureflow.features.settings.domain.repository.SettingsRepository

class GetThemeModeUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): String = repository.getThemeMode()
}

class SetThemeModeUseCase(private val repository: SettingsRepository) {
    operator fun invoke(mode: String) = repository.setThemeMode(mode)
}
