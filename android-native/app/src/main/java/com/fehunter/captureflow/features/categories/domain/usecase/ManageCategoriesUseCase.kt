package com.fehunter.captureflow.features.categories.domain.usecase

import com.fehunter.captureflow.features.captures.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.first

/**
 * Category validation and reassignment rules.
 * The repository owns the transaction that updates captures and legacy references.
 */
class ManageCategoriesUseCase(
    private val repository: CaptureRepository,
) {
    fun observeCategories() = repository.observeCategories()

    suspend fun ensureDefaultCategories() {
        DEFAULT_CATEGORIES.forEachIndexed { index, name ->
            repository.addCategory(name, if (name == COMPLETED_CATEGORY) 999 else index)
        }
    }

    suspend fun add(rawName: String) {
        val name = rawName.trim().take(MAX_CATEGORY_NAME_LENGTH)
        if (name.isBlank() || name.equals(ALL_CATEGORY, ignoreCase = true) ||
            name.equals(COMPLETED_CATEGORY, ignoreCase = true)
        ) return

        val current = repository.observeCategories().first()
        if (current.any { it.name.equals(name, ignoreCase = true) }) return
        val nextOrder = (current.filterNot { it.name == COMPLETED_CATEGORY }
            .maxOfOrNull { it.sortOrder } ?: -1) + 1
        repository.addCategory(name, nextOrder)
    }

    suspend fun rename(oldName: String, rawName: String) {
        val newName = rawName.trim().take(MAX_CATEGORY_NAME_LENGTH)
        if (oldName == COMPLETED_CATEGORY || newName.isBlank() ||
            newName.equals(ALL_CATEGORY, ignoreCase = true) ||
            newName.equals(COMPLETED_CATEGORY, ignoreCase = true)
        ) return

        val current = repository.observeCategories().first()
        if (current.none { it.name == oldName } ||
            current.any { it.name != oldName && it.name.equals(newName, ignoreCase = true) }
        ) return

        repository.renameCategory(oldName, newName)
    }

    suspend fun delete(name: String) {
        if (name == COMPLETED_CATEGORY) return
        val current = repository.observeCategories().first()
        if (current.size <= 1 || current.none { it.name == name }) return
        val fallback = current.firstOrNull {
            it.name != name && it.name != COMPLETED_CATEGORY
        }?.name ?: return
        repository.deleteCategory(name, fallback)
    }

    private companion object {
        const val MAX_CATEGORY_NAME_LENGTH = 32
        const val ALL_CATEGORY = "Todas"
        const val COMPLETED_CATEGORY = "Concluído"
        val DEFAULT_CATEGORIES = listOf(
            "Ler depois",
            "Verificar",
            "Salvar",
            "Importante",
            COMPLETED_CATEGORY,
        )
    }
}
