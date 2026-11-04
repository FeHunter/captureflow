package com.fehunter.captureflow.features.newsletters.domain.model

data class NewsletterArticleDraft(
    val stableId: String,
    val title: String,
    val canonicalUrl: String,
    val publishedAt: Long?,
    val summary: String?,
    val contentHtml: String?,
    val imageUrl: String?,
)

data class ParsedNewsletterFeed(
    val title: String?,
    val description: String?,
    val siteUrl: String?,
    val articles: List<NewsletterArticleDraft>,
)
