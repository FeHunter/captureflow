package com.fehunter.captureflow.features.captures.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
