package com.fehunter.captureflow.features.categories.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.categories.domain.usecase.ManageCategoriesUseCase

class CategoriesViewModelFactory(
    private val manageCategories: ManageCategoriesUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return CategoriesViewModel(manageCategories) as T
    }
}
