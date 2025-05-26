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

    // New methods for enhanced Room functionality - 5% completion tasks

    @Query("UPDATE photo_upload_requests SET retryCount = retryCount + 1, statusName = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementRetryCount(
        id: String,
        status: String = UploadStatus.RETRYING.name,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("SELECT * FROM photo_upload_requests WHERE statusName = :retrying ORDER BY updatedAt ASC")
    suspend fun getRetryingRequests(retrying: String = UploadStatus.RETRYING.name): List<PhotoUploadEntity>

    @Query("SELECT * FROM photo_upload_requests WHERE retryCount < :maxRetries AND statusName IN (:failed, :retrying)")
    suspend fun getRetriableRequests(
        maxRetries: Int = 3,
        failed: String = UploadStatus.FAILED.name,
        retrying: String = UploadStatus.RETRYING.name,
    ): List<PhotoUploadEntity>

    @Query("UPDATE photo_upload_requests SET statusName = :status, errorMessage = :errorMessage, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: String,
        errorMessage: String? = null,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE photo_upload_requests SET progress = :progress, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateProgress(
        id: String,
        progress: Int,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE photo_upload_requests SET parseFileUrl = :url, statusName = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateWithParseFileUrl(
        id: String,
        url: String,
        status: String = UploadStatus.COMPLETED.name,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM photo_upload_requests WHERE statusName IN (:completed, :failed, :cancelled)")
    suspend fun clearCompleted(
        completed: String = UploadStatus.COMPLETED.name,
        failed: String = UploadStatus.FAILED.name,
        cancelled: String = UploadStatus.CANCELLED.name,
    )

    // Batch operations for offline sync
    @Query("SELECT * FROM photo_upload_requests WHERE statusName = :pending ORDER BY createdAt ASC LIMIT :batchSize")
    suspend fun getPendingBatch(
        pending: String = UploadStatus.PENDING.name,
        batchSize: Int = 5,
    ): List<PhotoUploadEntity>

    @Transaction
    suspend fun bulkUpdateStatus(entities: List<PhotoUploadEntity>) {
        entities.forEach { update(it) }
    }

    // Network quality aware queries
    @Query("SELECT * FROM photo_upload_requests WHERE statusName = :pending AND retryCount = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getFreshPendingRequests(
        pending: String = UploadStatus.PENDING.name,
        limit: Int = 10,
    ): List<PhotoUploadEntity>

    @Query("SELECT COUNT(*) FROM photo_upload_requests WHERE statusName = :uploading")
    suspend fun getActiveUploadCount(uploading: String = UploadStatus.UPLOADING.name): Int
}
