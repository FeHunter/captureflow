package com.fehunter.captureflow.features.captures.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureMediaDao {
    @Query("SELECT * FROM capture_media ORDER BY captureId ASC, position ASC")
    fun observeAll(): Flow<List<CaptureMediaEntity>>
    @Query("SELECT * FROM capture_media WHERE captureId = :captureId ORDER BY position ASC")
    suspend fun findForCapture(captureId: String): List<CaptureMediaEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<CaptureMediaEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertImported(items: List<CaptureMediaEntity>)
    @Query("DELETE FROM capture_media WHERE captureId = :captureId") suspend fun deleteForCapture(captureId: String)
    @Query("UPDATE capture_media SET isCover = 0 WHERE captureId = :captureId") suspend fun clearCover(captureId: String)
    @Query("UPDATE capture_media SET isCover = 1 WHERE id = :mediaId AND captureId = :captureId") suspend fun setCover(captureId: String, mediaId: String)
}
