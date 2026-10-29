package com.fehunter.captureflow.data

import android.content.Intent
import android.net.Uri
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val kind: String,
    val category: String,
    val sourceUrl: String?,
    val imageUri: String?,
    val createdAt: Long,
    val isCompleted: Boolean = false,
)

@Entity(tableName = "checklist_tasks")
data class ChecklistTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
)

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures ORDER BY createdAt DESC") fun observeAll(): Flow<List<CaptureEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(capture: CaptureEntity)
    @Query("UPDATE captures SET isCompleted = :completed WHERE id = :id") suspend fun setCompleted(id: String, completed: Boolean)
    @Query("DELETE FROM captures WHERE id = :id") suspend fun delete(id: String)
    @Query("UPDATE captures SET content = :content, title = :title WHERE id = :id") suspend fun updateExtractedText(id: String, content: String, title: String)
    @Query("SELECT EXISTS(SELECT 1 FROM captures WHERE content = :content)") suspend fun containsContent(content: String): Boolean
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_tasks ORDER BY createdAt DESC") fun observeAll(): Flow<List<ChecklistTaskEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(task: ChecklistTaskEntity)
    @Query("UPDATE checklist_tasks SET isCompleted = :completed WHERE id = :id") suspend fun setCompleted(id: String, completed: Boolean)
    @Query("DELETE FROM checklist_tasks WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [CaptureEntity::class, ChecklistTaskEntity::class], version = 1, exportSchema = false)
abstract class CaptureDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun checklistDao(): ChecklistDao
    companion object {
        @Volatile private var instance: CaptureDatabase? = null
        fun get(context: Context): CaptureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CaptureDatabase::class.java, "captureflow.db")
                .build().also { instance = it }
        }
    }
}

data class IncomingCapture(val content: String, val imageUri: String? = null)

object CaptureClassifier {
    private val urlPattern = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
    fun normalize(raw: String) = raw.trim()
    fun classify(content: String, imageUri: String?): String = when {
        imageUri != null -> "image"
        urlPattern.containsMatchIn(content) -> "link"
        else -> "text"
    }
    fun titleFor(content: String, kind: String): String {
        if (kind == "image") return "Imagem compartilhada"
        val url = urlPattern.find(content)?.value
        if (content.isBlank() && url != null) return Uri.parse(url).host?.removePrefix("www.") ?: "Link salvo"
        val clean = content.replace(urlPattern, "").trim()
        return (clean.ifBlank { content }).take(72).ifBlank { "Nova referência" }
    }
    fun categoryFor(content: String, kind: String): String {
        val value = content.lowercase()
        return when {
            kind == "link" -> "Ler depois"
            listOf("checklist", "preciso", "lembrar", "comprar", "fazer", "[ ]").any(value::contains) -> "Tarefas"
            listOf("ideia", "inspir", "design", "projeto").any(value::contains) -> "Ideias"
            else -> "Referência"
        }
    }
    fun fromIntent(intent: Intent): IncomingCapture? {
        if (intent.action != Intent.ACTION_SEND) return null
        val type = intent.type.orEmpty()
        return when {
            type.startsWith("image/") -> {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                uri?.let { IncomingCapture("Imagem compartilhada", it.toString()) }
            }
            type == "text/plain" -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                ?.takeIf(String::isNotBlank)?.let(::IncomingCapture)
            else -> null
        }
    }
    fun create(content: String, imageUri: String? = null): CaptureEntity {
        val normalized = normalize(content)
        val kind = classify(normalized, imageUri)
        val url = urlPattern.find(normalized)?.value
        return CaptureEntity(UUID.randomUUID().toString(), titleFor(normalized, kind), normalized, kind,
            categoryFor(normalized, kind), url, imageUri, System.currentTimeMillis())
    }
}
