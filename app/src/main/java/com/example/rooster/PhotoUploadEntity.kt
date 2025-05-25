package com.example.rooster

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "photo_upload_requests")
data class PhotoUploadEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val uriString: String, // Changed from uri to uriString
    val fileName: String,
    val targetObjectId: String? = null,
    val targetClassName: String? = null,
    val targetField: String? = null,
    val metadataJson: String = "{}", // Changed from metadata to metadataJson
    val statusName: String = UploadStatus.PENDING.name, // Changed from status to statusName
    val progress: Int = 0,
    val retryCount: Int = 0,
    val parseFileUrl: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
