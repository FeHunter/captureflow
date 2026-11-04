package com.fehunter.captureflow.features.backup.data

import android.content.Context
import android.net.Uri
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureImportBatch
import com.fehunter.captureflow.features.captures.domain.model.CaptureMedia
import com.fehunter.captureflow.features.captures.domain.model.CaptureNotesCodec
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
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
) {
    suspend fun exportTo(destination: Uri): BackupSummary {
        val captureItems = captures.observeCaptures().first()
        val categories = captures.observeCategories().first()
        val subtasks = captures.observeSubtasks().first()
        val media = captures.observeMedia().first()
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
            .put("createdAt", System.currentTimeMillis()).put("appVersion", "0.4.0")
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

        context.contentResolver.openOutputStream(destination, "w")?.use { stream ->
            ZipOutputStream(stream).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                imageEntries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("Não foi possível criar o arquivo de backup.")
        return BackupSummary(captureItems.size, categories.size, imageEntries.size)
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
        require(manifest.optInt("version") == FORMAT_VERSION) { "Versão de backup não suportada." }

        val privateDirectory = File(context.filesDir, "captures").apply { mkdirs() }
        val createdFiles = mutableListOf<File>()
        try {
            val imageUris = entries.filterKeys { it.startsWith("images/") }.mapValues { (_, bytes) ->
                val file = File(privateDirectory, "backup-${UUID.randomUUID()}.img")
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
            val categoryJson = manifest.optJSONArray("categories") ?: JSONArray()
            for (index in 0 until categoryJson.length()) {
                val item = categoryJson.getJSONObject(index)
                val name = item.getString("name").trim()
                if (name.isNotBlank() && categoryNames.add(name)) {
                    importedCategories += CaptureCategory(name, item.optInt("sortOrder", categoryNames.size))
                }
            }
            importedCaptures.forEach { capture ->
                if (categoryNames.add(capture.category)) {
                    importedCategories += CaptureCategory(capture.category, categoryNames.size)
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

            captures.importBatch(CaptureImportBatch(importedCaptures, importedCategories, importedSubtasks, importedMedia))
            val referencedUris = buildSet {
                importedCaptures.mapNotNullTo(this) { it.imageUri }
                importedMedia.mapTo(this) { it.uri }
                importedCaptures.flatMap { CaptureNotesCodec.decode(it.notes) }.mapNotNullTo(this) { it.imageUri }
            }
            createdFiles.filterNot { Uri.fromFile(it).toString() in referencedUris }.forEach { it.delete() }
            return BackupSummary(importedCaptures.size, importedCategories.size, createdFiles.size)
        } catch (error: Exception) {
            createdFiles.forEach { it.delete() }
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
    data class BackupSummary(val captures: Int, val categories: Int, val images: Int)

    private companion object {
        const val FORMAT_NAME = "captureflow-backup"
        const val FORMAT_VERSION = 1
        const val MANIFEST_ENTRY = "manifest.json"
        const val MAX_IMAGE_BYTES = 25 * 1024 * 1024
        const val MAX_MANIFEST_BYTES = 10 * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = 250L * 1024 * 1024
        const val MAX_RECORDS = 50_000
        const val MAX_ARCHIVE_ENTRIES = 50_002
        const val MAX_MEDIA_PER_CAPTURE = 10
    }
}
