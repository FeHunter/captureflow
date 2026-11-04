package com.fehunter.captureflow.features.captures.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "capture_media",
    foreignKeys = [ForeignKey(
        entity = CaptureEntity::class,
        parentColumns = ["id"],
        childColumns = ["captureId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["captureId"]),
        Index(value = ["captureId", "position"], unique = true),
    ],
)
data class CaptureMediaEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val uri: String,
    val mediaType: String,
    val position: Int,
    val isCover: Boolean,
    val sourceUrl: String?,
    val createdAt: Long,
)
