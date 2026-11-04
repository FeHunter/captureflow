package com.fehunter.captureflow.features.newsletters.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fehunter.captureflow.core.ui.theme.CaptureFlowBackdrop
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.presentation.viewmodel.NewslettersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var openedArticle by remember { mutableStateOf<NewsletterArticle?>(null) }

    if (openedArticle != null) {
        NewsletterArticleReader(article = openedArticle!!, onBack = { openedArticle = null })
    } else Box(Modifier.fillMaxSize()) {
        CaptureFlowBackdrop(Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                top = 12.dp,
                end = 18.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar à biblioteca")
                        }
                        Spacer(Modifier.weight(1f))
                        if (selectedSource != null) {
                            IconButton(
                                onClick = { showDeleteSourceConfirmation = true },
                                enabled = !isLoading,
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Remover newsletter selecionada",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Icon(
                                Icons.Default.RssFeed,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp).size(25.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "SUA LEITURA, EM UM SÓ LUGAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                            )
                            Text(
                                "Newsletters",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    Text(
                        "Acompanhe publicações dos seus sites e autores favoritos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Adicionar uma fonte",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            "Insira o endereço de um feed RSS ou Atom. Também tentamos encontrar o feed a partir do site.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = feedUrl,
                            onValueChange = { feedUrl = it.take(2_000) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Endereço do site ou feed") },
                            placeholder = { Text("https://exemplo.com/feed.xml") },
                        )
                        Button(
                            onClick = {
                                viewModel.addFeed(feedUrl.trim())
                                feedUrl = ""
                            },
                            enabled = feedUrl.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Adicionar newsletter")
                        }
                    }
                }
            }

            if (sources.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Suas fontes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${sources.size} ${if (sources.size == 1) "fonte cadastrada" else "fontes cadastradas"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FilledTonalButton(
                                onClick = viewModel::refreshAll,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Atualizar todas")
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sources, key = { it.id }) { source ->
                                val selected = selectedSourceId == source.id
                                Surface(
                                    onClick = { viewModel.selectSource(source.id) },
                                    modifier = Modifier.widthIn(min = 172.dp, max = 230.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                                    ),
                                    tonalElevation = if (selected) 2.dp else 0.dp,
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.RssFeed,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp),
                                                tint = if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Spacer(Modifier.size(7.dp))
                                            Text(
                                                source.title,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Text(
                                            source.siteUrl?.let { runCatching { java.net.URI(it).host }.getOrNull() }
                                                ?: "Feed RSS / Atom",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (source.lastError != null) {
                                            Text("Falha na atualização", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedSource != null) {
                item {
                    val updatedAt = selectedSource.lastFetchedAt?.let {
                        SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(it))
                    }
                    val hasError = selectedSource.lastError != null
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (hasError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (hasError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (hasError) "Não foi possível atualizar esta fonte"
                                    else "Status da fonte",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    selectedSource.lastError ?: updatedAt?.let { "Atualizada em $it" }
                                    ?: "Esta fonte ainda não foi sincronizada.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = viewModel::refreshSelected,
                                enabled = !isLoading,
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Atualizar fonte")
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            "Sincronizando publicações…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(CircleShape),
                    )
                }
            }

            if (message != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            message.orEmpty(),
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (sources.isEmpty()) {
                item {
                    EmptyNewslettersState(
                        title = "Comece seguindo uma publicação",
                        description = "Adicione o feed RSS ou Atom de um site para reunir os novos artigos aqui.",
                    )
                }
            } else if (articles.isEmpty() && !isLoading) {
                item {
                    EmptyNewslettersState(
                        title = "Nenhuma publicação por aqui",
                        description = "Esta fonte ainda não tem artigos disponíveis. Você pode tentar atualizar o feed.",
                    )
                }
            } else if (articles.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Publicações",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${articles.size} artigos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(articles, key = { it.id }) { article ->
                    NewsletterArticleCard(
                        article = article,
                        onOpen = { openedArticle = article; viewModel.markRead(article.id, true) },
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
                TextButton(
                    onClick = {
                        viewModel.deleteSource(selectedSource.id)
                        showDeleteSourceConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remover fonte") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSourceConfirmation = false }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }
}

@Composable
private fun EmptyNewslettersState(
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Default.RssFeed,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NewsletterArticleCard(
    article: NewsletterArticle,
    onOpen: () -> Unit,
    onReadChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val publishedAt = article.publishedAt?.let {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (article.isRead) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        if (article.isRead) "LIDO" else "NÃO LIDO",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (article.isRead) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.weight(1f))
                publishedAt?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (article.isRead) FontWeight.Medium else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            article.summary?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("Ler no app", maxLines = 1)
                }
                TextButton(
                    onClick = { onReadChange(!article.isRead) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(if (article.isRead) "Não lido" else "Marcar lido", maxLines = 1)
                }
                TextButton(
                    onClick = onSave,
                    enabled = article.savedCaptureId == null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(if (article.savedCaptureId == null) "Salvar" else "Salvo", maxLines = 1)
                }
            }
        }
    }
}
