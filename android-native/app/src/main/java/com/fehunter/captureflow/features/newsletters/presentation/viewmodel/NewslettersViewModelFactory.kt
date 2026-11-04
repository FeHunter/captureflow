package com.fehunter.captureflow.features.newsletters.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import com.fehunter.captureflow.features.newsletters.data.feed.NewsletterFeedFetcher
import com.fehunter.captureflow.features.newsletters.data.feed.RssAtomFeedParser
import com.fehunter.captureflow.features.newsletters.domain.usecase.NewsletterUseCases

class NewslettersViewModelFactory(
    private val newsletterUseCases: NewsletterUseCases,
    private val captureUseCases: CaptureUseCases,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewslettersViewModel::class.java)) {
            return NewslettersViewModel(
                newsletterUseCases = newsletterUseCases,
                captureUseCases = captureUseCases,
                fetcher = NewsletterFeedFetcher(),
                parser = RssAtomFeedParser(),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
