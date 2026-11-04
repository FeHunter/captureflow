package com.fehunter.captureflow.features.newsletters.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.presentation.viewmodel.NewslettersViewModel

@Composable
fun NewslettersScreen(
    viewModel: NewslettersViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val sources by viewModel.sources.collectAsState()
    val selectedSourceId by viewModel.selectedSourceId.collectAsState()
    val selectedSource = sources.firstOrNull { it.id == selectedSourceId }
    val articles by viewModel.articles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    var feedUrl by remember { mutableStateOf("") }
    var showDeleteSourceConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("‹ Biblioteca") }
            Text("Newsletters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            selectedSource?.let {
                TextButton(onClick = { showDeleteSourceConfirmation = true }) { Text("Remover") }
            }
            TextButton(onClick = viewModel::refreshSelected, enabled = selectedSourceId != null && !isLoading) {
                Text("Atualizar")
            }
            TextButton(onClick = viewModel::refreshAll, enabled = sources.isNotEmpty() && !isLoading) {
                Text("Todas")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Adicionar fonte RSS ou Atom", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = feedUrl,
                    onValueChange = { feedUrl = it.take(2_000) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("URL do feed") },
                    placeholder = { Text("https://exemplo.com/feed.xml") },
                )
                Button(
                    onClick = { viewModel.addFeed(feedUrl); feedUrl = "" },
                    enabled = feedUrl.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Adicionar newsletter") }
            }
        }

        selectedSource?.let { source ->
            val updatedAt = source.lastFetchedAt?.let {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
            }
            Text(
                text = source.lastError ?: updatedAt?.let { "Última atualização: $it" } ?: "Ainda não atualizado",
                style = MaterialTheme.typography.bodySmall,
                color = if (source.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (sources.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources, key = { it.id }) { source ->
                    FilterChip(
                        selected = selectedSourceId == source.id,
                        onClick = { viewModel.selectSource(source.id) },
                        label = { Text(source.title, maxLines = 1) },
                    )
                }
            }
        }

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("Sincronizando feed…", style = MaterialTheme.typography.bodySmall)
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

        if (articles.isEmpty()) {
            Text(
                if (sources.isEmpty()) "Adicione uma fonte RSS ou Atom para começar."
                else "Nenhum artigo nesta fonte. Tente atualizar o feed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(articles, key = { it.id }) { article ->
                    NewsletterArticleCard(
                        article = article,
                        onReadChange = { viewModel.markRead(article.id, it) },
                        onSave = { viewModel.saveArticleToLibrary(article) },
                    )
                }
            }
        }
    }

    if (showDeleteSourceConfirmation && selectedSource != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSourceConfirmation = false },
            title = { Text("Remover newsletter?") },
            text = {
                Text("A fonte e os artigos armazenados serão removidos. Referências já salvas na Biblioteca permanecerão.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSource(selectedSource.id)
                    showDeleteSourceConfirmation = false
                }) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSourceConfirmation = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun NewsletterArticleCard(
    article: NewsletterArticle,
    onReadChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            article.summary?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { runCatching { uriHandler.openUri(article.canonicalUrl) } }) {
                    Text("Abrir original")
                }
                TextButton(onClick = { onReadChange(!article.isRead) }) {
                    Text(if (article.isRead) "Marcar não lido" else "Marcar lido")
                }
                TextButton(onClick = onSave, enabled = article.savedCaptureId == null) {
                    Text(if (article.savedCaptureId == null) "Salvar na Biblioteca" else "Salvo")
                }
            }
        }
    }
}
