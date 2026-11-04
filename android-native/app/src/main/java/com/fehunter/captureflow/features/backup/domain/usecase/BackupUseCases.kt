package com.fehunter.captureflow.features.backup.domain.usecase

import com.fehunter.captureflow.features.backup.domain.repository.BackupRepository

class BackupUseCases(private val repository: BackupRepository) {
    suspend fun export(destination: String) = repository.exportTo(destination)
    suspend fun import(source: String) = repository.importFrom(source)
}
