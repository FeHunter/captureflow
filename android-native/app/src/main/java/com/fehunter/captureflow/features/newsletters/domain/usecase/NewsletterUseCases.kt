package com.fehunter.captureflow.features.newsletters.domain.usecase

import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterSource
import com.fehunter.captureflow.features.newsletters.domain.repository.NewsletterRepository
import kotlinx.coroutines.flow.Flow

class NewsletterUseCases(private val repository: NewsletterRepository) {
    fun observeSources(): Flow<List<NewsletterSource>> = repository.observeSources()
    fun observeArticles(sourceId: String): Flow<List<NewsletterArticle>> = repository.observeArticles(sourceId)
    suspend fun findSourceByFeedUrl(feedUrl: String) = repository.findSourceByFeedUrl(feedUrl)
    suspend fun addSource(source: NewsletterSource) = repository.addSource(source)
    suspend fun updateSource(source: NewsletterSource) = repository.updateSource(source)
    suspend fun saveArticles(articles: List<NewsletterArticle>) = repository.saveArticles(articles)
    suspend fun markRead(articleId: String, isRead: Boolean) = repository.markRead(articleId, isRead)
    suspend fun markSaved(articleId: String, captureId: String?) = repository.markSaved(articleId, captureId)
    suspend fun pruneArticles(sourceId: String, keepCount: Int = DEFAULT_RETENTION) =
        repository.pruneArticles(sourceId, keepCount)
    suspend fun deleteSource(sourceId: String) = repository.deleteSource(sourceId)

    private companion object { const val DEFAULT_RETENTION = 100 }
}
