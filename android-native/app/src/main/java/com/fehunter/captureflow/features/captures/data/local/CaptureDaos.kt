package com.fehunter.captureflow.features.captures.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1") suspend fun findById(id: String): CaptureEntity?
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
