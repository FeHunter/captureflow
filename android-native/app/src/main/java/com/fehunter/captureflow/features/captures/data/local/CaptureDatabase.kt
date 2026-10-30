package com.fehunter.captureflow.features.captures.data.local

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
    val previousCategory: String? = null,
    val notes: String? = null,
)

@Entity(tableName = "categories")
data class CaptureCategoryEntity(
    @PrimaryKey val name: String,
    val sortOrder: Int,
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
interface CaptureCategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CaptureCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CaptureCategoryEntity)

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName")
    suspend fun rename(oldName: String, newName: String)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun delete(name: String)
}

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
    @Query("UPDATE captures SET isCompleted = 1, previousCategory = CASE WHEN category != 'Concluído' THEN category ELSE previousCategory END, category = 'Concluído' WHERE id = :id") suspend fun markCompleted(id: String)
    @Query("UPDATE captures SET isCompleted = 0, category = COALESCE(previousCategory, 'Salvar'), previousCategory = NULL WHERE id = :id") suspend fun markPending(id: String)
    @Query("UPDATE captures SET category = :category WHERE id = :id") suspend fun updateCategory(id: String, category: String)
    @Query("UPDATE captures SET category = :newName WHERE category = :oldName") suspend fun renameCategoryReferences(oldName: String, newName: String)
    @Query("UPDATE captures SET previousCategory = :newName WHERE previousCategory = :oldName") suspend fun renamePreviousCategoryReferences(oldName: String, newName: String)
    @Query("UPDATE captures SET imageUri = :imageUri WHERE id = :id") suspend fun updateImageUri(id: String, imageUri: String?)
    @Query("UPDATE captures SET title = :title WHERE id = :id") suspend fun updateTitle(id: String, title: String)
    @Query("UPDATE captures SET notes = :notes WHERE id = :id") suspend fun updateNotes(id: String, notes: String?)
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
    @Query("UPDATE checklist_tasks SET category = :newName WHERE category = :oldName") suspend fun renameCategoryReferences(oldName: String, newName: String)
}

@Database(entities = [CaptureEntity::class, ChecklistTaskEntity::class, CaptureSubtaskEntity::class, CaptureCategoryEntity::class], version = 8, exportSchema = false)
abstract class CaptureDatabase : RoomDatabase() {
    abstract fun categoryDao(): CaptureCategoryDao
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
                database.execSQL("CREATE TABLE IF NOT EXISTS categories (name TEXT NOT NULL PRIMARY KEY, sortOrder INTEGER NOT NULL)")
                database.execSQL("INSERT OR IGNORE INTO categories (name, sortOrder) VALUES ('Ler depois', 0), ('Verificar', 1), ('Salvar', 2), ('Importante', 3)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE captures ADD COLUMN previousCategory TEXT")
                database.execSQL("INSERT OR IGNORE INTO categories (name, sortOrder) VALUES ('Concluído', 999)")
                database.execSQL("UPDATE captures SET previousCategory = category, category = 'Concluído' WHERE isCompleted = 1")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE captures ADD COLUMN notes TEXT")
            }
        }

        @Volatile private var instance: CaptureDatabase? = null
        fun get(context: Context): CaptureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CaptureDatabase::class.java, "captureflow.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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
            listOf("importante", "urgente", "prazo", "prioridade").any(value::contains) -> "Importante"
            listOf("verificar", "conferir", "checar", "revisar").any(value::contains) -> "Verificar"
            kind == "link" -> "Ler depois"
            else -> "Salvar"
        }
    }
    fun fromIntent(intent: Intent): IncomingCapture? {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return null
        val type = intent.type.orEmpty().lowercase()
        if (type.startsWith("image/")) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            return uri?.let { IncomingCapture("Imagem compartilhada", it.toString()) }
        }

        // Different apps (including video/social apps) use text/plain, text/html,
        // text/uri-list or another text/* MIME type for the same shared URL.
        if (!type.startsWith("text/") && type != "application/x-www-form-urlencoded") return null

        val sharedText = runCatching {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        }.getOrNull()
        val clipText = intent.clipData?.let { clip ->
            (0 until clip.itemCount).mapNotNull { index ->
                val item = clip.getItemAt(index)
                item.text?.toString()?.takeIf(String::isNotBlank)
                    ?: item.uri?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            }.joinToString("\n").takeIf(String::isNotBlank)
        }
        val dataUrl = intent.data?.takeIf {
            it.scheme.equals("http", true) || it.scheme.equals("https", true)
        }?.toString()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf(String::isNotBlank)

        val content = sequenceOf(sharedText, clipText, dataUrl, subject)
            .filterNotNull()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString("\n")
        return content.takeIf(String::isNotBlank)?.let(::IncomingCapture)
    }
    fun create(content: String, imageUri: String? = null): CaptureEntity {
        val normalized = normalize(content)
        val kind = classify(normalized, imageUri)
        val url = urlPattern.find(normalized)?.value
        return CaptureEntity(UUID.randomUUID().toString(), titleFor(normalized, kind), normalized, kind,
            categoryFor(normalized, kind), url, imageUri, System.currentTimeMillis())
    }
}
