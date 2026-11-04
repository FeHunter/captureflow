package com.fehunter.captureflow.features.newsletters.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsletterDao {
    @Query("SELECT * FROM newsletter_sources ORDER BY title COLLATE NOCASE ASC")
    fun observeSources(): Flow<List<NewsletterSourceEntity>>

    @Query("SELECT * FROM newsletter_articles WHERE sourceId = :sourceId ORDER BY COALESCE(publishedAt, fetchedAt) DESC")
    fun observeArticles(sourceId: String): Flow<List<NewsletterArticleEntity>>

    @Query("SELECT * FROM newsletter_sources WHERE feedUrl = :feedUrl LIMIT 1")
    suspend fun findSourceByFeedUrl(feedUrl: String): NewsletterSourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: NewsletterSourceEntity)

    @Query("UPDATE newsletter_sources SET title = :title, siteUrl = :siteUrl, description = :description, imageUrl = :imageUrl, lastFetchedAt = :lastFetchedAt, etag = :etag, lastModified = :lastModified, lastError = :lastError WHERE id = :id")
    suspend fun updateSource(
        id: String, title: String, siteUrl: String?, description: String?, imageUrl: String?,
        lastFetchedAt: Long?, etag: String?, lastModified: String?, lastError: String?,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<NewsletterArticleEntity>)

    @Query("UPDATE newsletter_articles SET isRead = :isRead WHERE id = :articleId")
    suspend fun markRead(articleId: String, isRead: Boolean)

    @Query("UPDATE newsletter_articles SET savedCaptureId = :captureId WHERE id = :articleId")
    suspend fun markSaved(articleId: String, captureId: String?)

    @Query("""
        DELETE FROM newsletter_articles
        WHERE sourceId = :sourceId
          AND savedCaptureId IS NULL
          AND id NOT IN (
              SELECT id FROM newsletter_articles
              WHERE sourceId = :sourceId
              ORDER BY COALESCE(publishedAt, fetchedAt) DESC
              LIMIT :keepCount
          )
    """)
    suspend fun pruneArticles(sourceId: String, keepCount: Int)

    @Query("DELETE FROM newsletter_sources WHERE id = :sourceId")
    suspend fun deleteSource(sourceId: String)
}
