package com.fehunter.captureflow.data

import android.content.Intent
import android.net.Uri
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val sourceApp: String? = null,
    val sourceDetectionMethod: String? = null,
    @ColumnInfo(defaultValue = "0") val sourceVerified: Boolean = false,
)

@Entity(tableName = "checklist_tasks")
data class ChecklistTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val imageUri: String? = null,
    val description: String? = null,
    val category: String = "Verificar",
)

@Entity(
    tableName = "capture_subtasks",
    indices = [Index("captureId")],
)
data class CaptureSubtaskEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
)

@Dao
interface CaptureSubtaskDao {
    @Query("SELECT * FROM capture_subtasks WHERE captureId = :captureId ORDER BY createdAt ASC")
    fun observeForCapture(captureId: String): Flow<List<CaptureSubtaskEntity>>

    @Query("SELECT * FROM capture_subtasks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CaptureSubtaskEntity>>

    @Insert suspend fun insert(item: CaptureSubtaskEntity)

    @Query("UPDATE capture_subtasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("DELETE FROM capture_subtasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM capture_subtasks WHERE captureId = :captureId")
    suspend fun deleteForCapture(captureId: String)
}

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures ORDER BY createdAt DESC") fun observeAll(): Flow<List<CaptureEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(capture: CaptureEntity)
    @Query("UPDATE captures SET isCompleted = :completed WHERE id = :id") suspend fun setCompleted(id: String, completed: Boolean)
    @Query("UPDATE captures SET category = :category WHERE id = :id") suspend fun updateCategory(id: String, category: String)
    @Query("UPDATE captures SET imageUri = :imageUri WHERE id = :id") suspend fun updateImageUri(id: String, imageUri: String?)
    @Query("UPDATE captures SET sourceVerified = 1, sourceDetectionMethod = 'ocr_confirmed' WHERE id = :id") suspend fun confirmSourceUrl(id: String)
    @Query("UPDATE captures SET title = :title WHERE id = :id") suspend fun updateTitle(id: String, title: String)
    @Query("DELETE FROM captures WHERE id = :id") suspend fun delete(id: String)
    @Query("UPDATE captures SET content = :content, title = :title WHERE id = :id") suspend fun updateExtractedText(id: String, content: String, title: String)
    @Query("SELECT EXISTS(SELECT 1 FROM captures WHERE content = :content)") suspend fun containsContent(content: String): Boolean
    @Query("SELECT EXISTS(SELECT 1 FROM captures WHERE sourceUrl = :sourceUrl)") suspend fun containsSourceUrl(sourceUrl: String): Boolean
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_tasks ORDER BY createdAt DESC") fun observeAll(): Flow<List<ChecklistTaskEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(task: ChecklistTaskEntity)
    @Query("UPDATE checklist_tasks SET isCompleted = :completed WHERE id = :id") suspend fun setCompleted(id: String, completed: Boolean)
    @Query("DELETE FROM checklist_tasks WHERE id = :id") suspend fun delete(id: String)
    @Query("UPDATE checklist_tasks SET title = :title, description = :description WHERE id = :id") suspend fun updateDetails(id: String, title: String, description: String)
    @Query("UPDATE checklist_tasks SET category = :category WHERE id = :id") suspend fun updateCategory(id: String, category: String)
}

@Database(entities = [CaptureEntity::class, ChecklistTaskEntity::class, CaptureSubtaskEntity::class], version = 6, exportSchema = false)
abstract class CaptureDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun captureSubtaskDao(): CaptureSubtaskDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE checklist_tasks ADD COLUMN imageUri TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE checklist_tasks ADD COLUMN description TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS capture_subtasks (id TEXT NOT NULL PRIMARY KEY, captureId TEXT NOT NULL, title TEXT NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_capture_subtasks_captureId ON capture_subtasks(captureId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE checklist_tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'Tarefa'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE captures ADD COLUMN sourceApp TEXT")
                database.execSQL("ALTER TABLE captures ADD COLUMN sourceDetectionMethod TEXT")
                database.execSQL("ALTER TABLE captures ADD COLUMN sourceVerified INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE captures SET sourceVerified = CASE WHEN sourceUrl IS NULL THEN 0 ELSE 1 END")
            }
        }

        @Volatile private var instance: CaptureDatabase? = null
        fun get(context: Context): CaptureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CaptureDatabase::class.java, "captureflow.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build().also { instance = it }
        }
    }
}

data class IncomingCapture(
    val content: String,
    val imageUri: String? = null,
    val sourceUrl: String? = null,
    val sourceApp: String? = null,
    val sourceDetectionMethod: String? = null,
    val sourceVerified: Boolean = false,
)

object SourceUrlDetector {
    private val urlPattern = Regex("https?://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE)

    fun extract(raw: String): String? {
        val candidate = urlPattern.find(raw)?.value?.trimEnd('.', ',', ')', ']', '}', ';', ':') ?: return null
        val uri = runCatching { java.net.URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        return candidate.takeIf { scheme in setOf("http", "https") && !uri.host.isNullOrBlank() }
    }
}

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
            listOf("importante", "urgente", "prazo", "prioridade").any(value::contains) -> "Importante"
            listOf("verificar", "conferir", "checar", "revisar").any(value::contains) -> "Verificar"
            kind == "link" -> "Ler depois"
            else -> "Salvar"
        }
    }
    fun fromIntent(intent: Intent): IncomingCapture? {
        if (intent.action != Intent.ACTION_SEND) return null
        val type = intent.type.orEmpty()
        val extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val clipText = intent.clipData?.let { clip ->
            (0 until clip.itemCount).mapNotNull { index ->
                clip.getItemAt(index).text?.toString()
            }.joinToString("\n")
        }.orEmpty()
        val extraTextUrl = SourceUrlDetector.extract(extraText)
        val clipTextUrl = SourceUrlDetector.extract(clipText)
        val referrer = runCatching {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_REFERRER)
        }.getOrNull()?.toString() ?: intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
        val sourceApp = referrer?.let { value ->
            runCatching { Uri.parse(value) }.getOrNull()?.takeIf { it.scheme == "android-app" }?.host
        }

        return when {
            type.startsWith("image/") -> {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.let { clip ->
                    (0 until clip.itemCount).mapNotNull { index -> clip.getItemAt(index).uri }.firstOrNull()
                }
                uri?.let {
                    val sourceUrl = extraTextUrl ?: clipTextUrl
                    IncomingCapture(
                        content = extraText.ifBlank { "Imagem compartilhada" },
                        imageUri = it.toString(),
                        sourceUrl = sourceUrl,
                        sourceApp = sourceApp,
                        sourceDetectionMethod = when {
                            extraTextUrl != null -> "share_extra_text"
                            clipTextUrl != null -> "share_clipdata_text"
                            sourceApp != null -> "share_referrer_app"
                            else -> null
                        },
                        sourceVerified = sourceUrl != null,
                    )
                }
            }
            type == "text/plain" -> extraText.takeIf(String::isNotBlank)?.let {
                IncomingCapture(
                    content = it,
                    sourceUrl = extraTextUrl,
                    sourceApp = sourceApp,
                    sourceDetectionMethod = when {
                        extraTextUrl != null -> "share_extra_text"
                        sourceApp != null -> "share_referrer_app"
                        else -> null
                    },
                    sourceVerified = extraTextUrl != null,
                )
            }
            else -> null
        }
    }
    fun create(content: String, imageUri: String? = null): CaptureEntity {
        val normalized = normalize(content)
        val kind = classify(normalized, imageUri)
        val url = urlPattern.find(normalized)?.value
        return CaptureEntity(
            id = UUID.randomUUID().toString(),
            title = titleFor(normalized, kind),
            content = normalized,
            kind = kind,
            category = categoryFor(normalized, kind),
            sourceUrl = url,
            imageUri = imageUri,
            createdAt = System.currentTimeMillis(),
            sourceDetectionMethod = if (url != null) "shared_text" else null,
            sourceVerified = url != null,
        )
    }
}
