package com.example.rooster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.parse.ParseException // Explicit import for ParseException
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.SaveCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resumeWithException

// Serializable DTO for UI and enqueuing new uploads
data class SerializablePhotoUploadRequest(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val fileName: String,
    val targetParseObjectId: String,
    val targetClassName: String,
    val targetField: String,
    // val metadataJson: String = "{}", // If metadata is needed later
    var status: UploadStatus = UploadStatus.PENDING,
    var progress: Int = 0,
    var retryCount: Int = 0,
    var errorMessage: String? = null,
    var parseFileUrl: String? = null,
)

// Internal representation for active processing (not strictly needed if entity is rich enough)
// For now, we will operate directly on PhotoUploadEntity within the service after conversion.

// Result DTO for UI updates
data class UploadResult(
    val requestId: String,
    val status: UploadStatus,
    val progress: Int,
    val isSuccess: Boolean,
    val errorMessage: String?,
    val fileUrl: String?,
)

class PhotoUploadService(
    private val context: Context,
    private val networkQualityManager: NetworkQualityManager, // Pass NetworkQualityManager
) {
    private val dao = App.getPhotoUploadDao() // Use public getter
    private val _uploadResults = MutableSharedFlow<UploadResult>(replay = 1)
    val uploadResults = _uploadResults.asSharedFlow()
    private val uploadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val MAX_RETRY_COUNT = 3

    init {
        // Optionally resume uploads on service initialization
        // uploadScope.launch { resumeQueuedUploads() }
    }

    // Helper to convert UI DTO to Room Entity
    private fun SerializablePhotoUploadRequest.toPhotoUploadEntity(): PhotoUploadEntity {
        return PhotoUploadEntity(
            id = this.id,
            uriString = this.uri.toString(),
            fileName = this.fileName,
            targetObjectId = this.targetParseObjectId,
            targetClassName = this.targetClassName,
            targetField = this.targetField,
            // metadataJson = this.metadataJson, // if metadata is used
            statusName = this.status.name,
            progress = this.progress,
            retryCount = this.retryCount,
            errorMessage = this.errorMessage,
            parseFileUrl = this.parseFileUrl,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    // Helper to convert Room Entity back to UI DTO (SerializablePhotoUploadRequest)
    // This is used by UploadResult or if UI needs the original request structure
    private fun PhotoUploadEntity.toSerializableRequest(): SerializablePhotoUploadRequest {
        return SerializablePhotoUploadRequest(
            id = this.id,
            uri = Uri.parse(this.uriString),
            fileName = this.fileName,
            targetParseObjectId = this.targetObjectId ?: "", // Handle nullability if entity allows
            targetClassName = this.targetClassName ?: "",
            targetField = this.targetField ?: "",
            status = UploadStatus.valueOf(this.statusName),
            progress = this.progress,
            retryCount = this.retryCount,
            errorMessage = this.errorMessage,
            parseFileUrl = this.parseFileUrl,
        )
    }

    suspend fun enqueueUpload(requestDto: SerializablePhotoUploadRequest) {
        val entity = requestDto.toPhotoUploadEntity().copy(statusName = UploadStatus.PENDING.name, progress = 0, retryCount = 0)
        dao.insert(entity)
        FirebaseCrashlytics.getInstance().log("Enqueued upload: ${entity.fileName} (ID: ${entity.id})")
        _uploadResults.tryEmit(UploadResult(entity.id, UploadStatus.PENDING, 0, false, null, null))
        processUpload(entity)
    }

    suspend fun resumeQueuedUploads() {
        val pendingEntities = dao.getPendingRequests() // Fetches non-COMPLETED/FAILED
        FirebaseCrashlytics.getInstance().log("Resuming ${pendingEntities.size} queued uploads")
        pendingEntities.forEach { entity ->
            if (!activeJobs.containsKey(entity.id)) {
                _uploadResults.tryEmit(
                    UploadResult(
                        entity.id,
                        UploadStatus.valueOf(entity.statusName),
                        entity.progress,
                        false,
                        entity.errorMessage,
                        entity.parseFileUrl,
                    ),
                )
                processUpload(entity, isResumed = true)
            }
        }
    }

    private fun processUpload(
        initialEntity: PhotoUploadEntity,
        isResumed: Boolean = false,
    ) {
        if (activeJobs.containsKey(initialEntity.id) && !isResumed) {
            FirebaseCrashlytics.getInstance().log("Upload ${initialEntity.id} already active or completed.")
            return
        }

        val job =
            uploadScope.launch {
                var currentEntity = initialEntity
                var targetParseObj: ParseObject? = null

                try {
                    if (currentEntity.targetObjectId != null && currentEntity.targetClassName != null) {
                        targetParseObj = fetchParseObject(currentEntity.targetClassName!!, currentEntity.targetObjectId!!)
                        // If targetParseObj is null here (and it's critical), might need specific error handling
                        // or let it fail during linking. For now, proceed.
                    }

                    currentEntity = currentEntity.copy(statusName = UploadStatus.UPLOADING.name, progress = 0, updatedAt = System.currentTimeMillis())
                    dao.update(currentEntity)
                    _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.UPLOADING, 0, false, null, null))

                    val imageBytes = getCompressedImageBytes(Uri.parse(currentEntity.uriString), currentEntity.fileName)
                    if (imageBytes == null) {
                        throw IOException("Failed to prepare image bytes for ${currentEntity.fileName}")
                    }

                    val parseFile = ParseFile(currentEntity.fileName, imageBytes, "image/jpeg")

                    currentEntity = currentEntity.copy(progress = 50, updatedAt = System.currentTimeMillis())
                    dao.update(currentEntity)
                    _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.UPLOADING, 50, false, null, null))

                    // Perform ParseFile save using suspendCancellableCoroutine for better cancellation
                    val saveSuccessful =
                        suspendCancellableCoroutine<Boolean> { continuation ->
                            parseFile.saveInBackground(
                                object : SaveCallback {
                                    override fun done(e: ParseException?) {
                                        if (continuation.isActive) {
                                            if (e == null) {
                                                continuation.resume(true) {}
                                            } else {
                                                continuation.resumeWithException(e)
                                            }
                                        }
                                    }
                                },
                            )
                            // Cancellation for ParseFile.saveInBackground is typically not direct.
                            // The job itself being cancelled will prevent further processing.
                            // If Parse SDK offers a specific cancel for ParseFile upload, use it here.
                            // For now, relying on job cancellation to stop subsequent steps.
                        }

                    if (!saveSuccessful) { // Should not happen if exception is thrown by resumeWithException
                        throw RuntimeException("ParseFile save operation did not throw but returned false/null implicitly.")
                    }

                    currentEntity =
                        currentEntity.copy(
                            statusName = UploadStatus.COMPLETED.name,
                            progress = 100,
                            parseFileUrl = parseFile.url,
                            errorMessage = null, // Clear previous errors
                            updatedAt = System.currentTimeMillis(),
                        )
                    dao.update(currentEntity)
                    _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.COMPLETED, 100, true, null, parseFile.url))

                    if (targetParseObj != null && currentEntity.targetField != null) {
                        linkToTargetObject(parseFile, targetParseObj, currentEntity.targetField!!, currentEntity.id)
                    } else {
                        // If no target or field, consider it fully processed after upload
                        dao.delete(currentEntity)
                        FirebaseCrashlytics.getInstance().log(
                            "Upload ${currentEntity.id} completed (no target linking) and removed from queue.",
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        currentEntity = currentEntity.copy(statusName = UploadStatus.CANCELLED.name, progress = 0, updatedAt = System.currentTimeMillis())
                        dao.update(currentEntity) // Or delete if cancelled items should be kept
                        _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.CANCELLED, 0, false, "Cancelled by user", null))
                        FirebaseCrashlytics.getInstance().log("Upload ${currentEntity.id} cancelled by user.")
                        // Decide if cancelled items should be deleted or kept with CANCELLED status
                        dao.delete(currentEntity) // Example: remove cancelled from queue
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        handleUploadFailure(currentEntity, e.message ?: "Unknown upload error", e)
                    }
                } finally {
                    activeJobs.remove(initialEntity.id)
                }
            }
        activeJobs[initialEntity.id] = job
    }

    private suspend fun handleUploadFailure(
        entity: PhotoUploadEntity,
        errorMsg: String,
        exception: Exception,
    ) {
        var currentEntity =
            entity.copy(
                retryCount = entity.retryCount + 1,
                errorMessage = errorMsg,
                updatedAt = System.currentTimeMillis(),
            )

        if (currentEntity.retryCount <= MAX_RETRY_COUNT) {
            currentEntity = currentEntity.copy(statusName = UploadStatus.RETRYING.name)
            dao.update(currentEntity)
            _uploadResults.tryEmit(
                UploadResult(currentEntity.id, UploadStatus.RETRYING, currentEntity.progress, false, errorMsg, currentEntity.parseFileUrl),
            )
            val delayTime = 1000L * (2 shl (currentEntity.retryCount - 1)) // Exponential backoff: 2s, 4s, 8s
            FirebaseCrashlytics.getInstance().log(
                "Retrying upload ${currentEntity.id} (attempt ${currentEntity.retryCount}) in ${delayTime}ms. Error: $errorMsg",
            )
            delay(delayTime)
            processUpload(currentEntity, isResumed = true)
        } else {
            currentEntity = currentEntity.copy(statusName = UploadStatus.FAILED.name)
            dao.update(currentEntity)
            _uploadResults.tryEmit(
                UploadResult(currentEntity.id, UploadStatus.FAILED, currentEntity.progress, false, errorMsg, currentEntity.parseFileUrl),
            )
            FirebaseCrashlytics.getInstance().log(
                "Upload ${currentEntity.id} failed permanently after ${currentEntity.retryCount} retries. Error: $errorMsg",
            )
            // Failed uploads are kept in DB with FAILED status until cleared by clearCompletedUploadsFromQueue
        }
    }

    private suspend fun linkToTargetObject(
        parseFile: ParseFile,
        targetObject: ParseObject,
        targetField: String,
        originalRequestId: String,
    ) {
        var entityToUpdate: PhotoUploadEntity? = null
        try {
            targetObject.put(targetField, parseFile)
            val linkSuccessful =
                suspendCancellableCoroutine<Boolean> { continuation ->
                    targetObject.saveInBackground(
                        object : SaveCallback {
                            override fun done(e: ParseException?) {
                                if (continuation.isActive) {
                                    if (e == null) {
                                        continuation.resume(true) {}
                                    } else {
                                        continuation.resumeWithException(e)
                                    }
                                }
                            }
                        },
                    )
                }

            if (linkSuccessful) {
                FirebaseCrashlytics.getInstance().log(
                    "Successfully linked ParseFile ${parseFile.name} to ${targetObject.className}/${targetObject.objectId}#$targetField",
                )
                // Upload and linking successful, remove from queue
                entityToUpdate = dao.getById(originalRequestId)
                entityToUpdate?.let { dao.delete(it) }
                FirebaseCrashlytics.getInstance().log("Upload $originalRequestId fully completed and removed from queue.")
            } else {
                throw RuntimeException("ParseObject save for linking did not throw but returned false/null implicitly.")
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(
                RuntimeException(
                    "Linking failed for ${parseFile.name} to ${targetObject.className}/${targetObject.objectId}#$targetField: ${e.message}",
                    e,
                ),
            )
            entityToUpdate = dao.getById(originalRequestId)
            entityToUpdate?.let {
                val updatedEntity =
                    it.copy(
                        statusName = UploadStatus.LINKING_FAILED.name,
                        errorMessage = "Linking failed: ${e.message}",
                        updatedAt = System.currentTimeMillis(),
                    )
                dao.update(updatedEntity)
                _uploadResults.tryEmit(
                    UploadResult(
                        updatedEntity.id,
                        UploadStatus.LINKING_FAILED,
                        100,
                        false,
                        updatedEntity.errorMessage,
                        updatedEntity.parseFileUrl,
                    ),
                )
            }
        }
    }

    private suspend fun fetchParseObject(
        className: String,
        objectId: String,
    ): ParseObject? {
        return try {
            withContext(Dispatchers.IO) {
                ParseQuery.getQuery<ParseObject>(className).get(objectId)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().log("Failed to fetch ParseObject $className/$objectId: ${e.message}")
            null
        }
    }

    suspend fun cancelUpload(requestId: String) {
        activeJobs[requestId]?.cancel(CancellationException("User cancelled upload $requestId"))
        // The job's cancellation handler (in processUpload) will update DB status to CANCELLED and remove.
        FirebaseCrashlytics.getInstance().log("Attempted to cancel upload: $requestId")
    }

    suspend fun clearCompletedUploadsFromQueue() {
        dao.clearOldCompleted(UploadStatus.COMPLETED.name)
        dao.clearOldCompleted(
            UploadStatus.FAILED.name,
            System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
        ) // Clear FAILED older than 7 days
        dao.clearOldCompleted(UploadStatus.CANCELLED.name)
        FirebaseCrashlytics.getInstance().log("Cleared old completed, failed, and cancelled uploads from the local queue.")
    }

    private fun getCompressedImageBytes(
        uri: Uri,
        fileName: String,
    ): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                if (originalBitmap == null) {
                    FirebaseCrashlytics.getInstance().log("Failed to decode bitmap from URI for $fileName")
                    return null
                }

                val compressionConfig = this.networkQualityManager.getOptimalCompressionLevel()
                val quality = compressionConfig.quality
                val maxDimension = compressionConfig.maxDimension

                val resizedBitmap =
                    if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                        val ratio = minOf(maxDimension.toFloat() / originalBitmap.width, maxDimension.toFloat() / originalBitmap.height)
                        Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * ratio).toInt(),
                            (originalBitmap.height * ratio).toInt(),
                            true,
                        )
                    } else {
                        originalBitmap
                    }

                ByteArrayOutputStream().use { baos ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    if (resizedBitmap != originalBitmap) resizedBitmap.recycle()
                    originalBitmap.recycle()
                    baos.toByteArray()
                }
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(RuntimeException("Image compression failed for $fileName: ${e.message}", e))
            null
        }
    }

    fun shutdown() {
        uploadScope.cancel("PhotoUploadService is shutting down.")
        FirebaseCrashlytics.getInstance().log("PhotoUploadService shutdown.")
    }
}
