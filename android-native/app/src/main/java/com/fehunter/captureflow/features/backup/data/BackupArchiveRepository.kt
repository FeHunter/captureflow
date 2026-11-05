package com.fehunter.captureflow.features.backup.data

import android.net.Uri
import com.fehunter.captureflow.features.backup.domain.repository.BackupRepository
import com.fehunter.captureflow.features.backup.domain.repository.BackupResult

class BackupArchiveRepository(private val manager: BackupArchiveManager) : BackupRepository {
    override suspend fun exportTo(destination: String): BackupResult =
        manager.exportTo(Uri.parse(destination)).toDomain()

    override suspend fun importFrom(source: String): BackupResult =
        manager.importFrom(Uri.parse(source)).toDomain()

    private fun BackupArchiveManager.BackupSummary.toDomain() =
        BackupResult(captures, categories, images, newsletterSources, newsletterArticles)
}
