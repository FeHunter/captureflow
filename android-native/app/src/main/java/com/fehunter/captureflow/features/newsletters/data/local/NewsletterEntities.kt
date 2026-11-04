package com.fehunter.captureflow.features.newsletters.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "newsletter_sources",
    indices = [Index(value = ["feedUrl"], unique = true)],
)
data class NewsletterSourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val feedUrl: String,
    val siteUrl: String?,
    val description: String?,
    val imageUrl: String?,
    val createdAt: Long,
    val lastFetchedAt: Long?,
    val etag: String?,
    val lastModified: String?,
    val lastError: String?,
)

@Entity(
    tableName = "newsletter_articles",
    foreignKeys = [ForeignKey(
        entity = NewsletterSourceEntity::class,
        parentColumns = ["id"],
        childColumns = ["sourceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["sourceId", "stableId"], unique = true),
        Index(value = ["sourceId", "canonicalUrl"], unique = true),
    ],
)
data class NewsletterArticleEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val stableId: String,
    val title: String,
    val canonicalUrl: String,
    val publishedAt: Long?,
    val summary: String?,
    val contentHtml: String?,
    val imageUrl: String?,
    val isRead: Boolean,
    val savedCaptureId: String?,
    val fetchedAt: Long,
)
