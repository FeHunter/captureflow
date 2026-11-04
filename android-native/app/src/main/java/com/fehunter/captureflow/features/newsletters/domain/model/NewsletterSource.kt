package com.fehunter.captureflow.features.newsletters.domain.model

data class NewsletterSource(
    val id: String,
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

data class NewsletterArticle(
    val id: String,
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
