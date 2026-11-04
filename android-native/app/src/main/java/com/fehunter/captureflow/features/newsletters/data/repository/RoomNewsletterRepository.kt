package com.fehunter.captureflow.features.newsletters.data.repository

import com.fehunter.captureflow.features.newsletters.data.local.NewsletterDao
import com.fehunter.captureflow.features.newsletters.data.local.NewsletterArticleEntity
import com.fehunter.captureflow.features.newsletters.data.local.NewsletterSourceEntity
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterSource
import com.fehunter.captureflow.features.newsletters.domain.repository.NewsletterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNewsletterRepository(private val dao: NewsletterDao) : NewsletterRepository {
    override fun observeSources(): Flow<List<NewsletterSource>> =
        dao.observeSources().map { rows -> rows.map { it.toDomain() } }

    override fun observeArticles(sourceId: String): Flow<List<NewsletterArticle>> =
        dao.observeArticles(sourceId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findSourceByFeedUrl(feedUrl: String): NewsletterSource? =
        dao.findSourceByFeedUrl(feedUrl)?.toDomain()

    override suspend fun addSource(source: NewsletterSource) {
        dao.insertSource(source.toEntity())
    }

    override suspend fun updateSource(source: NewsletterSource) {
        dao.updateSource(
            id = source.id, title = source.title, siteUrl = source.siteUrl,
            description = source.description, imageUrl = source.imageUrl,
            lastFetchedAt = source.lastFetchedAt, etag = source.etag,
            lastModified = source.lastModified, lastError = source.lastError,
        )
    }

    override suspend fun saveArticles(articles: List<NewsletterArticle>) {
        if (articles.isNotEmpty()) dao.insertArticles(articles.map { it.toEntity() })
    }

    override suspend fun markRead(articleId: String, isRead: Boolean) = dao.markRead(articleId, isRead)
    override suspend fun markSaved(articleId: String, captureId: String?) = dao.markSaved(articleId, captureId)
    override suspend fun pruneArticles(sourceId: String, keepCount: Int) =
        dao.pruneArticles(sourceId, keepCount.coerceAtLeast(1))
    override suspend fun deleteSource(sourceId: String) = dao.deleteSource(sourceId)

    private fun NewsletterSourceEntity.toDomain() = NewsletterSource(
        id, title, feedUrl, siteUrl, description, imageUrl, createdAt,
        lastFetchedAt, etag, lastModified, lastError,
    )
    private fun NewsletterSource.toEntity() = NewsletterSourceEntity(
        id, title, feedUrl, siteUrl, description, imageUrl, createdAt,
        lastFetchedAt, etag, lastModified, lastError,
    )
    private fun NewsletterArticleEntity.toDomain() = NewsletterArticle(
        id, sourceId, stableId, title, canonicalUrl, publishedAt, summary,
        contentHtml, imageUrl, isRead, savedCaptureId, fetchedAt,
    )
    private fun NewsletterArticle.toEntity() = NewsletterArticleEntity(
        id, sourceId, stableId, title, canonicalUrl, publishedAt, summary,
        contentHtml, imageUrl, isRead, savedCaptureId, fetchedAt,
    )
}
