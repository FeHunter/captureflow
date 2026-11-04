package com.fehunter.captureflow.features.newsletters.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureFactory
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import com.fehunter.captureflow.features.newsletters.data.feed.NewsletterFeedFetcher
import com.fehunter.captureflow.features.newsletters.data.feed.NewsletterFetchResult
import com.fehunter.captureflow.features.newsletters.data.feed.RssAtomFeedParser
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterSource
import com.fehunter.captureflow.features.newsletters.domain.usecase.NewsletterUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URI
import java.util.UUID

class NewslettersViewModel(
    private val newsletterUseCases: NewsletterUseCases,
    private val captureUseCases: CaptureUseCases,
    private val fetcher: NewsletterFeedFetcher,
    private val parser: RssAtomFeedParser,
) : ViewModel() {
    val sources = newsletterUseCases.observeSources().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    private val _selectedSourceId = MutableStateFlow<String?>(null)
    val selectedSourceId = _selectedSourceId.asStateFlow()
    val articles = _selectedSourceId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else newsletterUseCases.observeArticles(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        viewModelScope.launch {
            sources.collect { items ->
                if (_selectedSourceId.value == null && items.isNotEmpty()) {
                    _selectedSourceId.value = items.first().id
                }
            }
        }
    }

    fun selectSource(sourceId: String) {
        _selectedSourceId.value = sourceId
        _message.value = null
    }

    fun addFeed(rawUrl: String) {
        if (_isLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _message.value = null
            try {
                val feedUrl = normalizeFeedUrl(rawUrl)
                val existing = newsletterUseCases.findSourceByFeedUrl(feedUrl)
                if (existing != null) {
                    _selectedSourceId.value = existing.id
                    refresh(existing)
                    return@launch
                }
                val result = fetcher.fetch(feedUrl)
                if (result !is NewsletterFetchResult.Success) {
                    throw IllegalStateException("O feed não retornou conteúdo atualizado.")
                }
                val parsed = parser.parse(result.xml, result.finalUrl)
                if (parsed.articles.isEmpty()) throw IllegalArgumentException("Não encontramos artigos válidos neste feed.")
                val now = System.currentTimeMillis()
                val source = NewsletterSource(
                    id = UUID.randomUUID().toString(),
                    title = parsed.title?.takeIf(String::isNotBlank) ?: URI(result.finalUrl).host.orEmpty(),
                    feedUrl = feedUrl,
                    siteUrl = parsed.siteUrl,
                    description = parsed.description,
                    imageUrl = null,
                    createdAt = now,
                    lastFetchedAt = now,
                    etag = result.etag,
                    lastModified = result.lastModified,
                    lastError = null,
                )
                newsletterUseCases.addSource(source)
                newsletterUseCases.saveArticles(parsed.articles.map { item ->
                    NewsletterArticle(
                        id = UUID.randomUUID().toString(), sourceId = source.id,
                        stableId = item.stableId, title = item.title, canonicalUrl = item.canonicalUrl,
                        publishedAt = item.publishedAt, summary = item.summary, contentHtml = item.contentHtml,
                        imageUrl = item.imageUrl, isRead = false, savedCaptureId = null, fetchedAt = now,
                    )
                })
                newsletterUseCases.pruneArticles(source.id, RETENTION_PER_SOURCE)
                _selectedSourceId.value = source.id
                _message.value = "Newsletter adicionada."
            } catch (error: Exception) {
                _message.value = error.localizedMessage ?: "Não foi possível adicionar este feed."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshSelected() {
        val sourceId = _selectedSourceId.value ?: return
        val source = sources.value.firstOrNull { it.id == sourceId } ?: return
        if (_isLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _message.value = null
            try {
                refresh(source)
                _message.value = "Feed atualizado."
            } catch (error: Exception) {
                _message.value = error.localizedMessage ?: "Não foi possível atualizar o feed."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refresh(source: NewsletterSource) {
        when (val result = fetcher.fetch(source.feedUrl, source.etag, source.lastModified)) {
            NewsletterFetchResult.NotModified -> newsletterUseCases.updateSource(
                source.copy(lastFetchedAt = System.currentTimeMillis(), lastError = null),
            )
            is NewsletterFetchResult.Success -> {
                val parsed = parser.parse(result.xml, result.finalUrl)
                val now = System.currentTimeMillis()
                newsletterUseCases.updateSource(
                    source.copy(
                        title = parsed.title?.takeIf(String::isNotBlank) ?: source.title,
                        siteUrl = parsed.siteUrl ?: source.siteUrl,
                        description = parsed.description ?: source.description,
                        lastFetchedAt = now,
                        etag = result.etag ?: source.etag,
                        lastModified = result.lastModified ?: source.lastModified,
                        lastError = null,
                    ),
                )
                newsletterUseCases.saveArticles(parsed.articles.map { item ->
                    NewsletterArticle(
                        id = UUID.randomUUID().toString(), sourceId = source.id,
                        stableId = item.stableId, title = item.title, canonicalUrl = item.canonicalUrl,
                        publishedAt = item.publishedAt, summary = item.summary, contentHtml = item.contentHtml,
                        imageUrl = item.imageUrl, isRead = false, savedCaptureId = null, fetchedAt = now,
                    )
                })
                newsletterUseCases.pruneArticles(source.id, RETENTION_PER_SOURCE)
            }
        }
    }

    fun markRead(articleId: String, isRead: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { newsletterUseCases.markRead(articleId, isRead) }
    }

    fun saveArticleToLibrary(article: NewsletterArticle) {
        viewModelScope.launch(Dispatchers.IO) {
            if (captureUseCases.findBySourceUrl(article.canonicalUrl)) {
                _message.value = "Este artigo já está salvo na Biblioteca."
                return@launch
            }
            val capture = CaptureFactory.create(article.canonicalUrl).copy(
                title = article.title.take(160),
                content = article.summary.orEmpty(),
                category = "Ler depois",
                sourceUrl = article.canonicalUrl,
            )
            captureUseCases.save(capture)
            newsletterUseCases.markSaved(article.id, capture.id)
            _message.value = "Artigo salvo na Biblioteca."
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            newsletterUseCases.deleteSource(sourceId)
            if (_selectedSourceId.value == sourceId) _selectedSourceId.value = null
        }
    }

    private fun normalizeFeedUrl(raw: String): String {
        val value = raw.trim()
        require(value.isNotBlank()) { "Informe o endereço do feed." }
        val uri = URI(value)
        require(uri.scheme?.lowercase() in setOf("http", "https")) { "Use um endereço HTTP ou HTTPS." }
        require(!uri.host.isNullOrBlank()) { "Endereço de feed inválido." }
        return uri.normalize().toString()
    }

    private companion object { const val RETENTION_PER_SOURCE = 100 }
}
