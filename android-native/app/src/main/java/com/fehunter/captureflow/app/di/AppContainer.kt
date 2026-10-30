package com.fehunter.captureflow.app.di

import android.app.Application
import com.fehunter.captureflow.features.captures.data.local.CaptureDatabase
import com.fehunter.captureflow.features.captures.data.repository.RoomCaptureRepository
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModelFactory
import com.fehunter.captureflow.features.imageimport.data.metadata.LinkMetadataService
import com.fehunter.captureflow.features.imageimport.data.ImageImportGatewayImpl
import com.fehunter.captureflow.features.imageimport.data.ocr.OcrService
import com.fehunter.captureflow.features.imageimport.presentation.viewmodel.ImageImportViewModelFactory
import com.fehunter.captureflow.features.categories.presentation.viewmodel.CategoriesViewModelFactory
import com.fehunter.captureflow.features.categories.domain.usecase.ManageCategoriesUseCase
import com.fehunter.captureflow.features.dashboard.domain.usecase.ObserveDashboardSummaryUseCase
import com.fehunter.captureflow.features.dashboard.presentation.viewmodel.DashboardViewModelFactory
import com.fehunter.captureflow.features.settings.data.SharedPreferencesSettingsRepository
import com.fehunter.captureflow.features.settings.domain.usecase.GetThemeModeUseCase
import com.fehunter.captureflow.features.settings.domain.usecase.SetThemeModeUseCase
import com.fehunter.captureflow.features.settings.presentation.viewmodel.SettingsViewModelFactory
import com.fehunter.captureflow.features.imageimport.data.storage.ImageImportStorageService
import com.fehunter.captureflow.core.storage.LocalPrivateImageStorage

/**
 * Manual composition root for the current single-module Android application.
 * Feature implementations are wired here rather than constructed by the UI layer.
 */
class AppContainer(application: Application) {
    private val database = CaptureDatabase.get(application)

    /** Feature-owned data boundary for domain use cases and ViewModels. */
    private val captureRepository = RoomCaptureRepository(database)
    val captureUseCases = CaptureUseCases(captureRepository)
    private val privateImageStorage = LocalPrivateImageStorage(application)
    private val imageImportStorage = ImageImportStorageService(application)
    private val imageImportGateway = ImageImportGatewayImpl(
        context = application,
        metadataService = LinkMetadataService(),
        ocrService = OcrService(),
        storageService = imageImportStorage,
    )

    private val settingsRepository = SharedPreferencesSettingsRepository(application)
    val settingsViewModelFactory = SettingsViewModelFactory(
        getThemeMode = GetThemeModeUseCase(settingsRepository),
        setThemeMode = SetThemeModeUseCase(settingsRepository),
    )

    val categoriesViewModelFactory = CategoriesViewModelFactory(ManageCategoriesUseCase(captureRepository))
    val dashboardViewModelFactory = DashboardViewModelFactory(ObserveDashboardSummaryUseCase(captureUseCases))

    val imageImportViewModelFactory = ImageImportViewModelFactory(
        captureUseCases = captureUseCases,
        gateway = imageImportGateway,
    )

    val captureViewModelFactory = CaptureViewModelFactory(
        captureUseCases = captureUseCases,
        privateImageStorage = privateImageStorage,
    )
}
