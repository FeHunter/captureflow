package com.fehunter.captureflow.features.captures.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fehunter.captureflow.features.newsletters.data.local.NewsletterSourceEntity
import com.fehunter.captureflow.features.newsletters.data.local.NewsletterArticleEntity
import com.fehunter.captureflow.features.newsletters.data.local.NewsletterDao

@Database(entities = [CaptureEntity::class, ChecklistTaskEntity::class, CaptureSubtaskEntity::class, CaptureCategoryEntity::class, CaptureMediaEntity::class, NewsletterSourceEntity::class, NewsletterArticleEntity::class], version = 11, exportSchema = false)
abstract class CaptureDatabase : RoomDatabase() {
    abstract fun categoryDao(): CaptureCategoryDao
    abstract fun captureDao(): CaptureDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun captureSubtaskDao(): CaptureSubtaskDao
    abstract fun captureMediaDao(): CaptureMediaDao
    abstract fun newsletterDao(): NewsletterDao
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
                database.execSQL("CREATE TABLE IF NOT EXISTS capture_subtasks (id TEXT NOT NULL PRIMARY KEY, captureId TEXT NOT NULL, title TEXT NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
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
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE captures SET category = COALESCE(NULLIF(previousCategory, ''), 'Salvar'), previousCategory = NULL WHERE category = 'Concluído'")
                database.execSQL("UPDATE captures SET previousCategory = NULL WHERE previousCategory IS NOT NULL")
                database.execSQL("DELETE FROM categories WHERE name = 'Concluído'")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS capture_media (id TEXT NOT NULL PRIMARY KEY, captureId TEXT NOT NULL, uri TEXT NOT NULL, mediaType TEXT NOT NULL, position INTEGER NOT NULL, isCover INTEGER NOT NULL, sourceUrl TEXT, createdAt INTEGER NOT NULL, FOREIGN KEY(captureId) REFERENCES captures(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_capture_media_captureId ON capture_media(captureId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_capture_media_captureId_position ON capture_media(captureId, position)")
                database.execSQL("INSERT OR IGNORE INTO capture_media (id, captureId, uri, mediaType, position, isCover, sourceUrl, createdAt) SELECT id || '-legacy-cover', id, imageUri, 'image/*', 0, 1, sourceUrl, createdAt FROM captures WHERE imageUri IS NOT NULL AND TRIM(imageUri) != ''")
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS newsletter_sources (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, feedUrl TEXT NOT NULL, siteUrl TEXT, description TEXT, imageUrl TEXT, createdAt INTEGER NOT NULL, lastFetchedAt INTEGER, etag TEXT, lastModified TEXT, lastError TEXT)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_newsletter_sources_feedUrl ON newsletter_sources(feedUrl)")
                database.execSQL("CREATE TABLE IF NOT EXISTS newsletter_articles (id TEXT NOT NULL PRIMARY KEY, sourceId TEXT NOT NULL, stableId TEXT NOT NULL, title TEXT NOT NULL, canonicalUrl TEXT NOT NULL, publishedAt INTEGER, summary TEXT, contentHtml TEXT, imageUrl TEXT, isRead INTEGER NOT NULL, savedCaptureId TEXT, fetchedAt INTEGER NOT NULL, FOREIGN KEY(sourceId) REFERENCES newsletter_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_newsletter_articles_sourceId ON newsletter_articles(sourceId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_newsletter_articles_sourceId_stableId ON newsletter_articles(sourceId, stableId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_newsletter_articles_sourceId_canonicalUrl ON newsletter_articles(sourceId, canonicalUrl)")
            }
        }

        @Volatile private var instance: CaptureDatabase? = null
        fun get(context: Context): CaptureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CaptureDatabase::class.java, "captureflow.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .build().also { instance = it }
        }
    }
}
