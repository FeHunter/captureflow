package com.fehunter.captureflow.features.newsletters.domain.repository

import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterSource
import kotlinx.coroutines.flow.Flow

interface NewsletterRepository {
    fun observeSources(): Flow<List<NewsletterSource>>
    fun observeArticles(sourceId: String): Flow<List<NewsletterArticle>>
    suspend fun findSourceByFeedUrl(feedUrl: String): NewsletterSource?
    suspend fun addSource(source: NewsletterSource)
    suspend fun updateSource(source: NewsletterSource)
    suspend fun saveArticles(articles: List<NewsletterArticle>)
    suspend fun markRead(articleId: String, isRead: Boolean)
    suspend fun markSaved(articleId: String, captureId: String?)
    suspend fun pruneArticles(sourceId: String, keepCount: Int)
    suspend fun deleteSource(sourceId: String)
}
