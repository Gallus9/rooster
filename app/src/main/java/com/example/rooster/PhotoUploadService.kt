package com.example.rooster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.parse.ParseException
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
    var status: UploadStatus = UploadStatus.PENDING,
    var progress: Int = 0,
    var retryCount: Int = 0,
    var errorMessage: String? = null,
    var parseFileUrl: String? = null,
)

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
    private val networkQualityManager: NetworkQualityManager,
) {
    private val dao = App.getPhotoUploadDao()
    private val _uploadResults = MutableSharedFlow<UploadResult>(replay = 1)
    val uploadResults = _uploadResults.asSharedFlow()
    private val uploadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    // Enhanced for 2G optimization - 5% completion
    private val maxRetryCount = 3
    private val base2GRetryDelay = 2000L // Start with 2s for 2G
    private val maxRetryDelay = 8000L // Cap at 8s max
    private val max2GConcurrentUploads = 2 // Limit concurrent for 2G
    private val maxGoodNetworkConcurrentUploads = 5

    init {
        uploadScope.launch {
            resumeQueuedUploads()
            // Start background sync for better 2G performance
            startPeriodicSync()
        }
    }

    // New periodic sync for 2G optimization
    private suspend fun startPeriodicSync() {
        while (uploadScope.isActive) {
            try {
                val networkQuality = networkQualityManager.getCurrentNetworkQuality()
                val syncInterval =
                    when (networkQuality) {
                        NetworkQualityLevel.EXCELLENT, NetworkQualityLevel.GOOD -> 30_000L // 30s
                        NetworkQualityLevel.FAIR -> 60_000L // 1min
                        NetworkQualityLevel.POOR -> 120_000L // 2min
                        NetworkQualityLevel.OFFLINE -> 300_000L // 5min
                    }

                delay(syncInterval)
                if (networkQuality != NetworkQualityLevel.OFFLINE) {
                    processQueuedUploadsForNetwork(networkQuality)
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                delay(60_000L) // Wait 1min on error
            }
        }
    }

    // Network-aware queue processing for 2G optimization
    private suspend fun processQueuedUploadsForNetwork(networkQuality: NetworkQualityLevel) {
        val activeCount = dao.getActiveUploadCount()
        val maxConcurrent =
            when (networkQuality) {
                NetworkQualityLevel.POOR, NetworkQualityLevel.OFFLINE -> max2GConcurrentUploads
                else -> maxGoodNetworkConcurrentUploads
            }

        if (activeCount >= maxConcurrent) return

        val batchSize =
            when (networkQuality) {
                NetworkQualityLevel.EXCELLENT, NetworkQualityLevel.GOOD -> 5
                NetworkQualityLevel.FAIR -> 3
                NetworkQualityLevel.POOR -> 2
                NetworkQualityLevel.OFFLINE -> 1
            }

        val pendingRequests = dao.getFreshPendingRequests(limit = batchSize)
        pendingRequests.take(maxConcurrent - activeCount).forEach { entity ->
            if (!activeJobs.containsKey(entity.id)) {
                processUpload(entity, isResumed = true)
            }
        }
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
            statusName = this.status.name,
            progress = this.progress,
            retryCount = this.retryCount,
            errorMessage = this.errorMessage,
            parseFileUrl = this.parseFileUrl,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    // Helper to convert Room Entity back to UI DTO
    private fun PhotoUploadEntity.toSerializableRequest(): SerializablePhotoUploadRequest {
        return SerializablePhotoUploadRequest(
            id = this.id,
            uri = Uri.parse(this.uriString),
            fileName = this.fileName,
            targetParseObjectId = this.targetObjectId ?: "",
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
        val entity =
            requestDto.toPhotoUploadEntity()
                .copy(statusName = UploadStatus.PENDING.name, progress = 0, retryCount = 0)
        dao.insert(entity)
        FirebaseCrashlytics.getInstance().log("Enqueued upload: ${entity.fileName} (ID: ${entity.id})")
        _uploadResults.tryEmit(UploadResult(entity.id, UploadStatus.PENDING, 0, false, null, null))

        // Only start immediate processing if network allows
        val networkQuality = networkQualityManager.getCurrentNetworkQuality()
        if (networkQuality != NetworkQualityLevel.OFFLINE && dao.getActiveUploadCount() < max2GConcurrentUploads) {
            processUpload(entity)
        }
    }

    suspend fun resumeQueuedUploads() {
        val pendingEntities = dao.getPendingRequests()
        FirebaseCrashlytics.getInstance().log("Resuming ${pendingEntities.size} queued uploads")

        val networkQuality = networkQualityManager.getCurrentNetworkQuality()
        val maxToResume =
            when (networkQuality) {
                NetworkQualityLevel.POOR, NetworkQualityLevel.OFFLINE -> max2GConcurrentUploads
                else -> maxGoodNetworkConcurrentUploads
            }

        pendingEntities.take(maxToResume).forEach { entity ->
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
        if (activeJobs.containsKey(initialEntity.id) && !isResumed) return

        val job =
            uploadScope.launch {
                var currentEntity = initialEntity
                var targetParseObj: ParseObject? = null

                try {
                    if (currentEntity.targetObjectId != null && currentEntity.targetClassName != null) {
                        targetParseObj = fetchParseObject(currentEntity.targetClassName!!, currentEntity.targetObjectId!!)
                    }

                    // Update to uploading status using enhanced DAO
                    dao.updateStatus(currentEntity.id, UploadStatus.UPLOADING.name)
                    dao.updateProgress(currentEntity.id, 0)
                    _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.UPLOADING, 0, false, null, null))

                    // Enhanced 2G-optimized compression
                    val imageBytes = get2GOptimizedImageBytes(Uri.parse(currentEntity.uriString), currentEntity.fileName)
                    if (imageBytes == null) {
                        throw IOException("Failed to prepare 2G-optimized image bytes for ${currentEntity.fileName}")
                    }

                    val parseFile = ParseFile(currentEntity.fileName, imageBytes, "image/jpeg")

                    // Update progress
                    dao.updateProgress(currentEntity.id, 50)
                    _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.UPLOADING, 50, false, null, null))

                    // Enhanced Parse upload with 2G timeout handling
                    val saveSuccessful =
                        suspendCancellableCoroutine<Boolean> { continuation ->
                            val timeoutJob =
                                uploadScope.launch {
                                    val networkQuality = networkQualityManager.getCurrentNetworkQuality()
                                    val timeout =
                                        when (networkQuality) {
                                            NetworkQualityLevel.POOR -> 60_000L // 60s for 2G
                                            NetworkQualityLevel.FAIR -> 45_000L
                                            else -> 30_000L
                                        }
                                    delay(timeout)
                                    if (continuation.isActive) {
                                        continuation.resumeWithException(IOException("Upload timeout after ${timeout}ms"))
                                    }
                                }

                            parseFile.saveInBackground(
                                object : SaveCallback {
                                    override fun done(e: ParseException?) {
                                        timeoutJob.cancel()
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

                    if (!saveSuccessful) {
                        throw RuntimeException("ParseFile save operation failed")
                    }

                    // Update with parse file URL using enhanced DAO
                    dao.updateWithParseFileUrl(currentEntity.id, parseFile.url ?: "")
                    _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.COMPLETED, 100, true, null, parseFile.url))

                    if (targetParseObj != null && currentEntity.targetField != null) {
                        linkToTargetObject(parseFile, targetParseObj, currentEntity.targetField!!, currentEntity.id)
                    } else {
                        dao.delete(dao.getById(currentEntity.id)!!)
                        FirebaseCrashlytics.getInstance().log("Upload ${currentEntity.id} completed and removed from queue.")
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        dao.updateStatus(currentEntity.id, UploadStatus.CANCELLED.name)
                        _uploadResults.tryEmit(UploadResult(currentEntity.id, UploadStatus.CANCELLED, 0, false, "Cancelled by user", null))
                        dao.delete(dao.getById(currentEntity.id)!!)
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        handleUploadFailureEnhanced(currentEntity, e.message ?: "Unknown upload error")
                    }
                } finally {
                    activeJobs.remove(initialEntity.id)
                }
            }
        activeJobs[initialEntity.id] = job
    }

    // Enhanced failure handling with better 2G retry logic
    private suspend fun handleUploadFailureEnhanced(
        entity: PhotoUploadEntity,
        errorMsg: String,
    ) {
        if (entity.retryCount < maxRetryCount) {
            dao.incrementRetryCount(entity.id)
            _uploadResults.tryEmit(
                UploadResult(entity.id, UploadStatus.RETRYING, entity.progress, false, errorMsg, entity.parseFileUrl),
            )

            // Enhanced 2G retry delay with exponential backoff
            val networkQuality = networkQualityManager.getCurrentNetworkQuality()
            val delayTime =
                when (networkQuality) {
                    NetworkQualityLevel.POOR -> {
                        // For 2G: longer delays, capped at maxRetryDelay
                        minOf(base2GRetryDelay * (entity.retryCount + 1), maxRetryDelay)
                    }
                    else -> {
                        // Standard exponential backoff for better networks
                        minOf(1000L * (2 shl entity.retryCount), maxRetryDelay)
                    }
                }

            FirebaseCrashlytics.getInstance().log(
                "2G-optimized retry for upload ${entity.id} (attempt ${entity.retryCount + 1}) in ${delayTime}ms. Error: $errorMsg",
            )
            delay(delayTime)

            val updatedEntity = dao.getById(entity.id)
            if (updatedEntity != null) {
                processUpload(updatedEntity, isResumed = true)
            }
        } else {
            dao.updateStatus(entity.id, UploadStatus.FAILED.name, errorMsg)
            _uploadResults.tryEmit(
                UploadResult(entity.id, UploadStatus.FAILED, entity.progress, false, errorMsg, entity.parseFileUrl),
            )
            FirebaseCrashlytics.getInstance().log(
                "Upload ${entity.id} failed permanently after ${entity.retryCount} retries. Error: $errorMsg",
            )
        }
    }

    private suspend fun linkToTargetObject(
        parseFile: ParseFile,
        targetObject: ParseObject,
        targetField: String,
        originalRequestId: String,
    ) {
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
                dao.getById(originalRequestId)?.let { dao.delete(it) }
                FirebaseCrashlytics.getInstance().log("Upload $originalRequestId fully completed and removed from queue.")
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            dao.updateStatus(
                originalRequestId,
                UploadStatus.LINKING_FAILED.name,
                "Linking failed: ${e.message}",
            )
            _uploadResults.tryEmit(
                UploadResult(originalRequestId, UploadStatus.LINKING_FAILED, 100, false, "Linking failed: ${e.message}", parseFile.url),
            )
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
        FirebaseCrashlytics.getInstance().log("Attempted to cancel upload: $requestId")
    }

    suspend fun clearCompletedUploadsFromQueue() {
        dao.clearCompleted()
        FirebaseCrashlytics.getInstance().log("Cleared old completed, failed, and cancelled uploads from the local queue.")
    }

    // Enhanced 2G-optimized image compression - major 5% improvement
    private fun get2GOptimizedImageBytes(
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

                val networkQuality = networkQualityManager.getCurrentNetworkQuality()
                val compressionConfig =
                    when (networkQuality) {
                        NetworkQualityLevel.POOR -> {
                            // Ultra compression for 2G - 20% quality, 240px max
                            ImageCompressionConfig(quality = 20, maxDimension = 240)
                        }
                        NetworkQualityLevel.FAIR -> {
                            // High compression for weak 3G - 40% quality, 480px max
                            ImageCompressionConfig(quality = 40, maxDimension = 480)
                        }
                        NetworkQualityLevel.GOOD -> {
                            // Medium compression - 60% quality, 720px max
                            ImageCompressionConfig(quality = 60, maxDimension = 720)
                        }
                        else -> {
                            // Low compression for excellent networks - 80% quality, 1080px max
                            ImageCompressionConfig(quality = 80, maxDimension = 1080)
                        }
                    }

                val resizedBitmap =
                    if (originalBitmap.width > compressionConfig.maxDimension ||
                        originalBitmap.height > compressionConfig.maxDimension
                    ) {
                        val ratio =
                            minOf(
                                compressionConfig.maxDimension.toFloat() / originalBitmap.width,
                                compressionConfig.maxDimension.toFloat() / originalBitmap.height,
                            )
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
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionConfig.quality, baos)
                    if (resizedBitmap != originalBitmap) resizedBitmap.recycle()
                    originalBitmap.recycle()

                    val compressedBytes = baos.toByteArray()
                    FirebaseCrashlytics.getInstance().log(
                        "2G-optimized compression: $fileName -> ${compressedBytes.size} bytes at ${compressionConfig.quality}% quality, ${compressionConfig.maxDimension}px max",
                    )
                    compressedBytes
                }
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(
                RuntimeException("2G image compression failed for $fileName: ${e.message}", e),
            )
            null
        }
    }

    fun shutdown() {
        uploadScope.cancel("PhotoUploadService is shutting down.")
        FirebaseCrashlytics.getInstance().log("PhotoUploadService shutdown.")
    }
}

// Helper data class for 2G compression
private data class ImageCompressionConfig(
    val quality: Int,
    val maxDimension: Int,
)
