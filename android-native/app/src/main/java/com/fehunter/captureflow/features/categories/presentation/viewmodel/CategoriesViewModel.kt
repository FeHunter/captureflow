package com.fehunter.captureflow.features.categories.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.features.categories.domain.usecase.ManageCategoriesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val manageCategories: ManageCategoriesUseCase,
) : ViewModel() {
    init {
        viewModelScope.launch { manageCategories.ensureDefaultCategories() }
    }

    val categories = manageCategories.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(rawName: String) {
        viewModelScope.launch { manageCategories.add(rawName) }
    }

    fun renameCategory(oldName: String, rawName: String) {
        viewModelScope.launch { manageCategories.rename(oldName, rawName) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch { manageCategories.delete(name) }
    }
}
