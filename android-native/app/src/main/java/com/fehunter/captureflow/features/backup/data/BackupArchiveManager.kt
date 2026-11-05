package com.fehunter.captureflow.features.backup.data

import android.content.Context
import android.net.Uri
import com.fehunter.captureflow.BuildConfig
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureImportBatch
import com.fehunter.captureflow.features.captures.domain.model.CaptureMedia
import com.fehunter.captureflow.features.captures.domain.model.CaptureNotesCodec
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterSource
import com.fehunter.captureflow.features.newsletters.domain.usecase.NewsletterUseCases
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Versioned ZIP backup. Import is additive and never overwrites local records. */
class BackupArchiveManager(
    private val context: Context,
    private val captures: CaptureUseCases,
    private val newsletters: NewsletterUseCases,
) {
    suspend fun exportTo(destination: Uri): BackupSummary {
        val captureItems = captures.observeCaptures().first()
        val categories = captures.observeCategories().first()
        val subtasks = captures.observeSubtasks().first()
        val media = captures.observeMedia().first()
        val newsletterSources = newsletters.observeSources().first()
        val newsletterArticles = newsletterSources.flatMap { source -> newsletters.observeArticles(source.id).first() }
        require(newsletterSources.size <= MAX_RECORDS && newsletterArticles.size <= MAX_RECORDS) {
            "O backup contém fontes ou artigos Newsletter demais."
        }
        val imageEntries = linkedMapOf<String, ByteArray>()

        fun imageReference(rawUri: String?): String? {
            if (rawUri.isNullOrBlank()) return null
            val bytes = context.contentResolver.openInputStream(Uri.parse(rawUri))?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_IMAGE_BYTES) { "Uma imagem excede o limite de 25 MB." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: throw IllegalStateException("Não foi possível ler uma imagem do backup.")
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") { "%02x".format(it) }
            val entry = "images/$digest.bin"
            if (entry !in imageEntries) {
                val currentBytes = imageEntries.values.sumOf { it.size.toLong() }
                require(currentBytes + bytes.size <= MAX_ARCHIVE_BYTES) { "O backup excede o limite de 250 MB." }
                imageEntries[entry] = bytes
            }
            return entry
        }

        val manifest = JSONObject().put("format", FORMAT_NAME).put("version", FORMAT_VERSION)
            .put("createdAt", System.currentTimeMillis()).put("appVersion", BuildConfig.VERSION_NAME)
        val categoryArray = JSONArray()
        categories.forEach { category ->
            categoryArray.put(JSONObject().put("name", category.name).put("sortOrder", category.sortOrder))
        }
        manifest.put("categories", categoryArray)

        val captureArray = JSONArray()
        captureItems.forEach { capture ->
            val noteImages = JSONArray()
            CaptureNotesCodec.decode(capture.notes).forEach { note ->
                imageReference(note.imageUri)?.let { ref ->
                    noteImages.put(JSONObject().put("noteId", note.id).put("image", ref))
                }
            }
            captureArray.put(JSONObject()
                .put("id", capture.id).put("title", capture.title).put("content", capture.content)
                .put("kind", capture.kind).put("category", capture.category)
                .put("sourceUrl", capture.sourceUrl ?: JSONObject.NULL)
                .put("image", imageReference(capture.imageUri) ?: JSONObject.NULL)
                .put("createdAt", capture.createdAt).put("isCompleted", capture.isCompleted)
                .put("previousCategory", capture.previousCategory ?: JSONObject.NULL)
                .put("notes", capture.notes ?: JSONObject.NULL).put("noteImages", noteImages))
        }
        manifest.put("captures", captureArray)

        val subtaskArray = JSONArray()
        subtasks.forEach { item ->
            subtaskArray.put(JSONObject().put("id", item.id).put("captureId", item.captureId)
                .put("title", item.title).put("isCompleted", item.isCompleted).put("createdAt", item.createdAt))
        }
        manifest.put("subtasks", subtaskArray)

        val mediaArray = JSONArray()
        media.forEach { item ->
            mediaArray.put(JSONObject().put("id", item.id).put("captureId", item.captureId)
                .put("image", imageReference(item.uri)).put("mediaType", item.mediaType)
                .put("position", item.position).put("isCover", item.isCover)
                .put("sourceUrl", item.sourceUrl ?: JSONObject.NULL).put("createdAt", item.createdAt))
        }
        manifest.put("media", mediaArray)

        val newsletterSourceArray = JSONArray()
        newsletterSources.forEach { source ->
            newsletterSourceArray.put(JSONObject()
                .put("id", source.id).put("title", source.title).put("feedUrl", source.feedUrl)
                .put("siteUrl", source.siteUrl ?: JSONObject.NULL)
                .put("description", source.description ?: JSONObject.NULL)
                .put("imageUrl", source.imageUrl ?: JSONObject.NULL)
                .put("createdAt", source.createdAt).put("lastFetchedAt", source.lastFetchedAt ?: JSONObject.NULL)
                .put("etag", source.etag ?: JSONObject.NULL).put("lastModified", source.lastModified ?: JSONObject.NULL)
                .put("lastError", source.lastError ?: JSONObject.NULL))
        }
        manifest.put("newsletterSources", newsletterSourceArray)

        val newsletterArticleArray = JSONArray()
        newsletterArticles.forEach { article ->
            newsletterArticleArray.put(JSONObject()
                .put("id", article.id).put("sourceId", article.sourceId).put("stableId", article.stableId)
                .put("title", article.title).put("canonicalUrl", article.canonicalUrl)
                .put("publishedAt", article.publishedAt ?: JSONObject.NULL)
                .put("summary", article.summary ?: JSONObject.NULL)
                .put("contentHtml", article.contentHtml ?: JSONObject.NULL)
                .put("imageUrl", article.imageUrl ?: JSONObject.NULL).put("isRead", article.isRead)
                .put("savedCaptureId", article.savedCaptureId ?: JSONObject.NULL).put("fetchedAt", article.fetchedAt))
        }
        manifest.put("newsletterArticles", newsletterArticleArray)

        context.contentResolver.openOutputStream(destination, "w")?.use { stream ->
            ZipOutputStream(stream).use { zip ->
                val manifestBytes = manifest.toString().toByteArray(Charsets.UTF_8)
                require(manifestBytes.size <= MAX_MANIFEST_BYTES) { "O manifesto excede o limite de 10 MB." }
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifestBytes)
                zip.closeEntry()
                imageEntries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("Não foi possível criar o arquivo de backup.")
        return BackupSummary(captureItems.size, categories.size, imageEntries.size, newsletterSources.size, newsletterArticles.size)
    }

    suspend fun importFrom(source: Uri): BackupSummary {
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        context.contentResolver.openInputStream(source)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(entries.size < MAX_ARCHIVE_ENTRIES) { "O backup contém arquivos demais." }
                    require(!entry.isDirectory && !entry.name.startsWith("/") &&
                        !entry.name.contains("..") && '\\' !in entry.name) {
                        "O backup contém um caminho inválido."
                    }
                    require(entry.name == MANIFEST_ENTRY || entry.name.startsWith("images/")) {
                        "O backup contém um arquivo não reconhecido."
                    }
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var entryBytes = 0
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        require(entryBytes <= if (entry.name == MANIFEST_ENTRY) MAX_MANIFEST_BYTES else MAX_IMAGE_BYTES) {
                            "Um arquivo do backup excede o limite permitido."
                        }
                        require(totalBytes <= MAX_ARCHIVE_BYTES) { "O backup excede o limite de 250 MB." }
                        output.write(buffer, 0, count)
                    }
                    require(entries.put(entry.name, output.toByteArray()) == null) {
                        "O backup contém entradas duplicadas."
                    }
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("Não foi possível abrir o arquivo selecionado.")

        val manifestBytes = entries[MANIFEST_ENTRY] ?: throw IllegalArgumentException("Manifesto do backup não encontrado.")
        val manifest = runCatching { JSONObject(manifestBytes.toString(Charsets.UTF_8)) }
            .getOrElse { throw IllegalArgumentException("O manifesto do backup está inválido.") }
        require(manifest.optString("format") == FORMAT_NAME) { "Este arquivo não é um backup do CaptureFlow." }
        val backupVersion = manifest.optInt("version")
        require(backupVersion in MIN_SUPPORTED_VERSION..FORMAT_VERSION) { "Versão de backup não suportada." }

        val privateDirectory = File(context.filesDir, "captures").apply { mkdirs() }
        val createdFiles = mutableListOf<File>()
        var capturesCommitted = false
        var retainedImageUris = emptySet<String>()
        try {
            val imageUris = entries.filterKeys { it.startsWith("images/") }.mapValues { (_, bytes) ->
                val extension = when {
                    bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(0x52, 0x49, 0x46, 0x46)) &&
                        bytes.copyOfRange(8, 12).contentEquals(byteArrayOf(0x57, 0x45, 0x42, 0x50)) -> "webp"
                    bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(
                        byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a),
                    ) -> "png"
                    bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() -> "jpg"
                    bytes.size >= 6 && (bytes.copyOfRange(0, 6).toString(Charsets.US_ASCII) in setOf("GIF87a", "GIF89a")) -> "gif"
                    else -> "img"
                }
                val file = File(privateDirectory, "backup-${UUID.randomUUID()}.$extension")
                file.writeBytes(bytes)
                createdFiles += file
                Uri.fromFile(file).toString()
            }
            fun restoredImage(value: Any?): String? {
                if (value == null || value == JSONObject.NULL) return null
                val path = value as? String ?: throw IllegalArgumentException("Referência de imagem inválida.")
                require(path.startsWith("images/") && !path.contains("..")) { "Caminho de imagem inválido." }
                return imageUris[path] ?: throw IllegalArgumentException("Imagem ausente no arquivo de backup.")
            }

            val existingCaptures = captures.observeCaptures().first()
            val existingCategories = captures.observeCategories().first()
            val existingSubtasks = captures.observeSubtasks().first()
            val existingMedia = captures.observeMedia().first()
            val usedIds = existingCaptures.map { it.id }.toMutableSet()
            val existingKeys = existingCaptures.map(::duplicateKey).toMutableSet()
            val importedCaptures = mutableListOf<Capture>()
            val captureIdMap = mutableMapOf<String, String>()
            val importedKeys = mutableSetOf<String>()
            val captureJson = manifest.getJSONArray("captures")
            require(captureJson.length() <= MAX_RECORDS) { "O backup contém referências demais." }
            val seenCaptureIds = mutableSetOf<String>()

            for (index in 0 until captureJson.length()) {
                val item = captureJson.getJSONObject(index)
                val oldId = item.getString("id")
                require(seenCaptureIds.add(oldId)) { "O manifesto contém IDs de referência duplicados." }
                val notesRaw = item.optString("notes").takeUnless { it == "null" }
                val noteImages = item.optJSONArray("noteImages") ?: JSONArray()
                val noteImageMap = buildMap<String, String?> {
                    for (noteIndex in 0 until noteImages.length()) {
                        val noteImage = noteImages.getJSONObject(noteIndex)
                        put(noteImage.getString("noteId"), restoredImage(noteImage.get("image")))
                    }
                }
                val restoredNotes = if (notesRaw.isNullOrBlank() || !notesRaw.startsWith("CAPTUREFLOW_NOTES_V1")) {
                    notesRaw
                } else {
                    CaptureNotesCodec.encode(CaptureNotesCodec.decode(notesRaw).map { note ->
                        note.copy(imageUri = noteImageMap[note.id] ?: note.imageUri)
                    })
                }
                val original = Capture(
                    id = oldId, title = item.getString("title"), content = item.optString("content"),
                    kind = item.optString("kind", "text"), category = item.optString("category", "Salvar"),
                    sourceUrl = item.optString("sourceUrl").takeUnless { it == "null" || it.isBlank() },
                    imageUri = restoredImage(item.opt("image")), createdAt = item.getLong("createdAt"),
                    isCompleted = item.optBoolean("isCompleted"),
                    previousCategory = item.optString("previousCategory").takeUnless { it == "null" || it.isBlank() },
                    notes = restoredNotes,
                )
                val key = duplicateKey(original)
                if (key in existingKeys || key in importedKeys) {
                    captureIdMap[oldId] = existingCaptures.firstOrNull { duplicateKey(it) == key }?.id
                        ?: importedCaptures.first { duplicateKey(it) == key }.id
                    continue
                }
                val newId = if (oldId in usedIds) UUID.randomUUID().toString() else oldId
                usedIds += newId
                captureIdMap[oldId] = newId
                importedCaptures += original.copy(id = newId)
                importedKeys += key
            }

            val categoryNames = existingCategories.map { it.name }.toMutableSet()
            val importedCategories = mutableListOf<CaptureCategory>()
            var nextCategoryOrder = (existingCategories.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val categoryJson = manifest.optJSONArray("categories") ?: JSONArray()
            val orderedCategories = (0 until categoryJson.length())
                .map { categoryJson.getJSONObject(it) }
                .sortedBy { it.optInt("sortOrder", Int.MAX_VALUE) }
            orderedCategories.forEach { item ->
                val name = item.getString("name").trim()
                if (name.isNotBlank() && categoryNames.add(name)) {
                    importedCategories += CaptureCategory(name, nextCategoryOrder++)
                }
            }
            importedCaptures.forEach { capture ->
                if (categoryNames.add(capture.category)) {
                    importedCategories += CaptureCategory(capture.category, nextCategoryOrder++)
                }
            }

            val existingSubtaskIds = existingSubtasks.map { it.id }.toMutableSet()
            val importedSubtasks = mutableListOf<CaptureSubtask>()
            val importedCaptureIds = importedCaptures.map { it.id }.toSet()
            val subtaskJson = manifest.optJSONArray("subtasks") ?: JSONArray()
            require(subtaskJson.length() <= MAX_RECORDS) { "O backup contém subtarefas demais." }
            for (index in 0 until subtaskJson.length()) {
                val item = subtaskJson.getJSONObject(index)
                val mappedCapture = captureIdMap[item.getString("captureId")] ?: continue
                if (mappedCapture !in importedCaptureIds) continue
                val oldId = item.getString("id")
                val id = if (oldId in existingSubtaskIds) UUID.randomUUID().toString() else oldId
                existingSubtaskIds += id
                importedSubtasks += CaptureSubtask(id, mappedCapture, item.getString("title"),
                    item.optBoolean("isCompleted"), item.getLong("createdAt"))
            }

            val existingMediaIds = existingMedia.map { it.id }.toMutableSet()
            val importedMedia = mutableListOf<CaptureMedia>()
            val mediaJson = manifest.optJSONArray("media") ?: JSONArray()
            require(mediaJson.length() <= MAX_RECORDS) { "O backup contém mídias demais." }
            val mediaCountByCapture = (0 until mediaJson.length())
                .map { mediaJson.getJSONObject(it).getString("captureId") }.groupingBy { it }.eachCount()
            require(mediaCountByCapture.values.all { it <= MAX_MEDIA_PER_CAPTURE }) {
                "Uma referência do backup excede o limite de imagens permitido."
            }
            for (index in 0 until mediaJson.length()) {
                val item = mediaJson.getJSONObject(index)
                val mappedCapture = captureIdMap[item.getString("captureId")] ?: continue
                if (mappedCapture !in importedCaptureIds) continue
                val uri = restoredImage(item.opt("image")) ?: continue
                val oldId = item.getString("id")
                val id = if (oldId in existingMediaIds) UUID.randomUUID().toString() else oldId
                existingMediaIds += id
                importedMedia += CaptureMedia(id, mappedCapture, uri,
                    item.optString("mediaType", "image/*"), item.optInt("position"),
                    item.optBoolean("isCover"), item.optString("sourceUrl").takeUnless { it == "null" },
                    item.optLong("createdAt"))
            }

            // Compute references before committing the Capture batch so a later Newsletter
            // restore failure never deletes files already referenced by committed captures.
            retainedImageUris = buildSet {
                importedCaptures.mapNotNullTo(this) { it.imageUri }
                importedMedia.mapTo(this) { it.uri }
                importedCaptures.flatMap { CaptureNotesCodec.decode(it.notes) }.mapNotNullTo(this) { it.imageUri }
            }
            captures.importBatch(CaptureImportBatch(importedCaptures, importedCategories, importedSubtasks, importedMedia))
            capturesCommitted = true

            // v1 archives have no newsletter arrays. v2 restores feeds and articles additively.
            val currentNewsletterSources = newsletters.observeSources().first()
            val sourceIdMap = mutableMapOf<String, String>()
            val sourceIdByFeedUrl = currentNewsletterSources.associate { it.feedUrl to it.id }.toMutableMap()
            val usedSourceIds = currentNewsletterSources.map { it.id }.toMutableSet()
            val newsletterSourceJson = manifest.optJSONArray("newsletterSources") ?: JSONArray()
            require(newsletterSourceJson.length() <= MAX_RECORDS) { "O backup contém fontes Newsletter demais." }
            for (index in 0 until newsletterSourceJson.length()) {
                val item = newsletterSourceJson.getJSONObject(index)
                val oldId = item.getString("id")
                val feedUrl = item.getString("feedUrl")
                val existingId = sourceIdByFeedUrl[feedUrl]
                val targetId = existingId ?: if (oldId in usedSourceIds) UUID.randomUUID().toString() else oldId
                sourceIdMap[oldId] = targetId
                if (existingId == null) {
                    usedSourceIds += targetId
                    sourceIdByFeedUrl[feedUrl] = targetId
                    newsletters.addSource(NewsletterSource(
                        id = targetId,
                        title = item.optString("title").ifBlank { feedUrl },
                        feedUrl = feedUrl,
                        siteUrl = item.optString("siteUrl").takeUnless { it == "null" },
                        description = item.optString("description").takeUnless { it == "null" },
                        imageUrl = item.optString("imageUrl").takeUnless { it == "null" },
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        lastFetchedAt = item.optLong("lastFetchedAt").takeIf { !item.isNull("lastFetchedAt") },
                        etag = item.optString("etag").takeUnless { it == "null" },
                        lastModified = item.optString("lastModified").takeUnless { it == "null" },
                        lastError = item.optString("lastError").takeUnless { it == "null" },
                    ))
                }
            }

            val knownCaptureIds = existingCaptures.map { it.id }.toSet() + importedCaptures.map { it.id }
            val newsletterArticleJson = manifest.optJSONArray("newsletterArticles") ?: JSONArray()
            require(newsletterArticleJson.length() <= MAX_RECORDS) { "O backup contém artigos demais." }
            val importedArticles = mutableListOf<NewsletterArticle>()
            for (index in 0 until newsletterArticleJson.length()) {
                val item = newsletterArticleJson.getJSONObject(index)
                val sourceId = sourceIdMap[item.getString("sourceId")] ?: continue
                val savedCaptureRaw = item.optString("savedCaptureId").takeUnless { it == "null" || it.isBlank() }
                val savedCaptureId = savedCaptureRaw?.let { captureIdMap[it] ?: it.takeIf(knownCaptureIds::contains) }
                importedArticles += NewsletterArticle(
                    id = UUID.randomUUID().toString(),
                    sourceId = sourceId,
                    stableId = item.getString("stableId"),
                    title = item.getString("title"),
                    canonicalUrl = item.getString("canonicalUrl"),
                    publishedAt = item.optLong("publishedAt").takeIf { !item.isNull("publishedAt") },
                    summary = item.optString("summary").takeUnless { it == "null" },
                    contentHtml = item.optString("contentHtml").takeUnless { it == "null" },
                    imageUrl = item.optString("imageUrl").takeUnless { it == "null" },
                    isRead = item.optBoolean("isRead"),
                    savedCaptureId = savedCaptureId,
                    fetchedAt = item.optLong("fetchedAt", System.currentTimeMillis()),
                )
            }
            newsletters.saveArticles(importedArticles)

            val retainedFiles = createdFiles.filter { Uri.fromFile(it).toString() in retainedImageUris }
            createdFiles.filterNot { it in retainedFiles }.forEach { it.delete() }
            return BackupSummary(importedCaptures.size, importedCategories.size, retainedFiles.size, newsletterSourceJson.length(), importedArticles.size)
        } catch (error: Exception) {
            createdFiles.filterNot { capturesCommitted && Uri.fromFile(it).toString() in retainedImageUris }
                .forEach { it.delete() }
            throw error
        }
    }

    private fun duplicateKey(item: Capture): String {
        val url = item.sourceUrl.orEmpty().trim().lowercase().removeSuffix("/")
        val notes = CaptureNotesCodec.decode(item.notes).joinToString("|") {
            "${it.text.trim()}:${!it.imageUri.isNullOrBlank()}"
        }
        val identity = "${item.title.trim().lowercase()}|${item.content.trim()}|${item.kind}|${item.category}|${item.isCompleted}|$notes"
        return if (url.isNotBlank()) "$url|$identity" else identity
    }
    data class BackupSummary(val captures: Int, val categories: Int, val images: Int, val newsletterSources: Int, val newsletterArticles: Int)

    private companion object {
        const val FORMAT_NAME = "captureflow-backup"
        const val FORMAT_VERSION = 2
        const val MIN_SUPPORTED_VERSION = 1
        const val MANIFEST_ENTRY = "manifest.json"
        const val MAX_IMAGE_BYTES = 25 * 1024 * 1024
        const val MAX_MANIFEST_BYTES = 10 * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = 250L * 1024 * 1024
        const val MAX_RECORDS = 50_000
        const val MAX_ARCHIVE_ENTRIES = 50_002
        const val MAX_MEDIA_PER_CAPTURE = 10
    }
}
