package com.fehunter.captureflow.features.captures.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
