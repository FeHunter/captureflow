package com.fehunter.captureflow.features.share.presentation.screens

import android.content.Intent
import android.content.ClipData
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import java.io.File
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fehunter.captureflow.features.share.domain.model.CaptureSharePayload
import com.fehunter.captureflow.features.share.presentation.viewmodel.CaptureShareViewModel

@Composable
fun CaptureShareDialog(
    payload: CaptureSharePayload,
    viewModel: CaptureShareViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shareText = buildString {
        append(payload.title)
        append("\n\nCategoria: ")
        append(payload.category)
        if (!payload.sourceUrl.isNullOrBlank()) append("\n\n${payload.sourceUrl}")
        if (payload.content.isNotBlank()) append("\n\n${payload.content}")
        payload.notes.forEachIndexed { index, note ->
            append("\n\nAnotação ${index + 1}")
            if (note.text.isNotBlank()) append(":\n${note.text}")
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { viewModel.exportPdf(payload, it.toString()) }
    }
    val rtfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/rtf")) { uri ->
        uri?.let { viewModel.exportRtf(payload, it.toString()) }
    }

    LaunchedEffect(state.shareablePdfUri) {
        val rawUri = state.shareablePdfUri ?: return@LaunchedEffect
        runCatching {
            val sourceFile = Uri.parse(rawUri).path?.let(::File)?.canonicalFile ?: error("Arquivo PDF temporário inválido.")
            val shareDirectory = File(context.cacheDir, "share_exports").canonicalFile
            require(sourceFile.path.startsWith(shareDirectory.path + File.separator) && sourceFile.isFile) {
                "O PDF temporário não está disponível para compartilhamento."
            }
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sourceFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, payload.title)
                putExtra(Intent.EXTRA_TEXT, shareText)
                clipData = ClipData.newUri(context.contentResolver, payload.title, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Compartilhar PDF da tarefa"))
        }.onFailure { viewModel.reportShareError(it.message ?: "Não foi possível compartilhar o PDF.") }
        viewModel.consumeShareablePdf()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartilhar pensamento", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Compartilhe o título, link e suas anotações ou exporte um documento com a capa e as imagens relacionadas.")
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(shareText)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text("  Copiar texto")
                }
                Button(
                    onClick = {
                        val shareDirectory = File(context.cacheDir, "share_exports").apply { mkdirs() }
                        val expiration = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
                        shareDirectory.listFiles()?.filter { it.isFile && it.lastModified() < expiration }?.forEach(File::delete)
                        val pdfFile = File(shareDirectory, "${safeName(payload.title)}-${System.nanoTime()}.pdf")
                        viewModel.exportPdfForSharing(payload, Uri.fromFile(pdfFile).toString())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text("  Compartilhar PDF")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pdfLauncher.launch("${safeName(payload.title)}.pdf") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isBusy,
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Text(" PDF")
                    }
                    OutlinedButton(
                        onClick = { rtfLauncher.launch("${safeName(payload.title)}.rtf") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isBusy,
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Text(" RTF")
                    }
                }
                if (state.isBusy) CircularProgressIndicator()
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

private fun safeName(value: String): String =
    value.trim().replace(Regex("[^A-Za-z0-9 _-]"), "").take(48).ifBlank { "CaptureFlow-Task" }
