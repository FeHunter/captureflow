package com.fehunter.captureflow.features.backup.domain.repository

interface BackupRepository {
    suspend fun exportTo(destination: String): BackupResult
    suspend fun importFrom(source: String): BackupResult
}

data class BackupResult(val captures: Int, val categories: Int, val images: Int, val newsletterSources: Int, val newsletterArticles: Int)
