package com.example.rooster

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoUploadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: PhotoUploadEntity)

    @Update
    suspend fun update(request: PhotoUploadEntity)

    @Delete
    suspend fun delete(request: PhotoUploadEntity)

    @Query("SELECT * FROM photo_upload_requests WHERE statusName NOT IN (:completed, :failed) ORDER BY createdAt DESC")
    suspend fun getPendingRequests(
        completed: String = UploadStatus.COMPLETED.name,
        failed: String = UploadStatus.FAILED.name,
    ): List<PhotoUploadEntity>

    @Query("SELECT * FROM photo_upload_requests ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentUploads(limit: Int = 20): Flow<List<PhotoUploadEntity>>

    @Query("SELECT * FROM photo_upload_requests WHERE id = :id")
    suspend fun getById(id: String): PhotoUploadEntity?

    @Query("DELETE FROM photo_upload_requests WHERE statusName = :completed AND updatedAt < :beforeTimestamp")
    suspend fun clearOldCompleted(
        completed: String = UploadStatus.COMPLETED.name,
        beforeTimestamp: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000, // 24 hours ago
    )

    @Query("SELECT COUNT(*) FROM photo_upload_requests WHERE statusName = :statusName")
    suspend fun getCountByStatus(statusName: String): Int
}
