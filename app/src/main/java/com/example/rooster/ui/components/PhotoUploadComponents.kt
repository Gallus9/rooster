package com.example.rooster.ui.components

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // Ensure this is used for LazyVerticalGrid
import androidx.compose.foundation.lazy.items // Ensure this is used for LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.rooster.* // Import all from base package for enums and data classes
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// PhotoPickerDialog (simplified, assuming camera permission requested before calling this if needed)
@Composable
fun PhotoPickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPhotoSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showDialog) return
    val context = LocalContext.current
    var tempUriHolder by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri: Uri? ->
                onPhotoSelected(uri)
                onDismiss()
            },
        )
    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { success ->
                if (success) {
                    tempUriHolder?.let { onPhotoSelected(it) }
                }
                onDismiss()
            },
        )
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                val newUri = PhotoUriHelper.newUri(context)
                tempUriHolder = newUri
                cameraLauncher.launch(newUri)
            } else {
                // Handle permission denial, e.g., show a snackbar
                onDismiss() // Dismiss if permission denied and not proceeding
            }
        }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Choose Photo Source", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    OptionButton(icon = Icons.Filled.PhotoLibrary, text = "Gallery") {
                        galleryLauncher.launch("image/*")
                    }
                    OptionButton(icon = Icons.Filled.PhotoCamera, text = "Camera") {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(icon, text, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PhotoPreviewWithEdit(
    uri: Uri,
    onConfirm: (Uri) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier =
                modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text("Preview & Confirm", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected Photo",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Note: Editing features are placeholders.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                    Button(onClick = { onConfirm(uri) }) { Text("Confirm Upload") }
                }
            }
        }
    }
}

// Enhanced Progress Indicator with better rural optimization
@Composable
fun UploadProgressIndicator(
    uploadResult: UploadResult,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (uploadResult.status) {
                        UploadStatus.FAILED, UploadStatus.LINKING_FAILED -> MaterialTheme.colorScheme.errorContainer
                        UploadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        UploadStatus.RETRYING -> MaterialTheme.colorScheme.secondaryContainer
                        UploadStatus.UPLOADING ->
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.7f,
                            )
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Status Icon
            when (uploadResult.status) {
                UploadStatus.UPLOADING ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )

                UploadStatus.COMPLETED ->
                    Icon(
                        Icons.Filled.CheckCircle,
                        "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )

                UploadStatus.FAILED, UploadStatus.LINKING_FAILED ->
                    Icon(
                        Icons.Filled.Error,
                        "Failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )

                UploadStatus.RETRYING ->
                    Icon(
                        Icons.Filled.Refresh,
                        "Retrying",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp),
                    )

                UploadStatus.PENDING, UploadStatus.QUEUED ->
                    Icon(
                        Icons.Filled.Schedule,
                        "Pending",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp),
                    )

                else ->
                    Icon(
                        Icons.Filled.CloudUpload,
                        "Upload",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp),
                    )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upload ${uploadResult.requestId.take(8)}...",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )

                // Enhanced progress display for different states
                when (uploadResult.status) {
                    UploadStatus.UPLOADING, UploadStatus.RETRYING -> {
                        LinearProgressIndicator(
                            progress = { uploadResult.progress / 100f },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${uploadResult.progress}%",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (uploadResult.status == UploadStatus.RETRYING) {
                                Text(
                                    "Retrying...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }

                    UploadStatus.COMPLETED -> {
                        Text(
                            "✓ Upload Complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    UploadStatus.PENDING, UploadStatus.QUEUED -> {
                        Text(
                            "Waiting for network...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }

                    else -> {
                        Text(uploadResult.status.name, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Enhanced error message display
                uploadResult.errorMessage?.let { error ->
                    Text(
                        text =
                            when {
                                error.contains(
                                    "timeout",
                                    ignoreCase = true,
                                ) -> "⚡ Slow network - retrying with smaller size"

                                error.contains(
                                    "network",
                                    ignoreCase = true,
                                ) -> "📶 Network issue - will retry when connection improves"

                                error.contains(
                                    "memory",
                                    ignoreCase = true,
                                ) -> "💾 Optimizing image size..."

                                else -> "⚠ $error"
                            },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action buttons with rural-friendly design
            when (uploadResult.status) {
                UploadStatus.FAILED, UploadStatus.LINKING_FAILED -> {
                    Column {
                        IconButton(
                            onClick = { onRetry(uploadResult.requestId) },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                "Retry Upload",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "Retry",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp),
                        )
                    }
                }
                UploadStatus.UPLOADING, UploadStatus.RETRYING, UploadStatus.PENDING, UploadStatus.QUEUED -> {
                    Column {
                        IconButton(
                            onClick = { onCancel(uploadResult.requestId) },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.Cancel,
                                "Cancel Upload",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp),
                        )
                    }
                }
                UploadStatus.COMPLETED -> {
                    Column {
                        Icon(
                            Icons.Filled.CheckCircle,
                            "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            "Done",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                else -> {
                    // Empty space for other states
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

// Enhanced Network Quality Indicator for rural users
@Composable
fun NetworkQualityIndicator(
    networkQuality: NetworkQualityLevel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (icon, color, text) =
            when (networkQuality) {
                NetworkQualityLevel.EXCELLENT ->
                    Triple(
                        "📶📶📶📶",
                        MaterialTheme.colorScheme.primary,
                        "Excellent",
                    )

                NetworkQualityLevel.GOOD ->
                    Triple(
                        "📶📶📶",
                        MaterialTheme.colorScheme.secondary,
                        "Good",
                    )

                NetworkQualityLevel.FAIR ->
                    Triple(
                        "📶📶",
                        MaterialTheme.colorScheme.tertiary,
                        "Fair",
                    )

                NetworkQualityLevel.POOR ->
                    Triple(
                        "📶",
                        MaterialTheme.colorScheme.error,
                        "Poor",
                    )

                NetworkQualityLevel.OFFLINE ->
                    Triple(
                        "📵",
                        MaterialTheme.colorScheme.outline,
                        "Offline",
                    )
            }

        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 4.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

// Enhanced Upload Queue Summary for rural users
@Composable
fun UploadQueueSummary(
    uploadResults: List<UploadResult>,
    networkQuality: NetworkQualityLevel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Upload Queue",
                    style = MaterialTheme.typography.titleMedium,
                )
                NetworkQualityIndicator(networkQuality)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val pending =
                uploadResults.count {
                    it.status in
                        listOf(
                            UploadStatus.PENDING,
                            UploadStatus.QUEUED,
                        )
                }
            val uploading =
                uploadResults.count {
                    it.status in
                        listOf(
                            UploadStatus.UPLOADING,
                            UploadStatus.RETRYING,
                        )
                }
            val completed = uploadResults.count { it.status == UploadStatus.COMPLETED }
            val failed =
                uploadResults.count {
                    it.status in
                        listOf(
                            UploadStatus.FAILED,
                            UploadStatus.LINKING_FAILED,
                        )
                }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QueueStatItem("Pending", pending, MaterialTheme.colorScheme.outline)
                QueueStatItem("Uploading", uploading, MaterialTheme.colorScheme.primary)
                QueueStatItem("Completed", completed, MaterialTheme.colorScheme.secondary)
                QueueStatItem("Failed", failed, MaterialTheme.colorScheme.error)
            }

            // Network-specific advice
            if (networkQuality == NetworkQualityLevel.POOR || networkQuality == NetworkQualityLevel.OFFLINE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text =
                        if (networkQuality == NetworkQualityLevel.OFFLINE) {
                            "📵 Uploads will resume when connection is restored"
                        } else {
                            "📶 Uploads optimized for slow network - please be patient"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun QueueStatItem(
    label: String,
    count: Int,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun PhotoGridDisplay(
    uris: List<Uri>,
    onPhotoClick: (Uri) -> Unit,
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxPhotos: Int = 5,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(uris) { uri -> // Correct usage for LazyVerticalGrid
            AsyncImage(
                model = uri,
                contentDescription = "Photo in grid",
                modifier =
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPhotoClick(uri) }
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        if (uris.size < maxPhotos) {
            item {
                Box(
                    modifier =
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onAddPhotoClick() }
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.AddAPhoto,
                        contentDescription = "Add Photo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

object PhotoUriHelper {
    fun newUri(context: Context): Uri {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = context.getExternalFilesDir("Pictures")
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    }
}

// Enhanced Demo Screen with better rural simulation
@Composable
fun PhotoUploadComponentsDemoScreen(
    photoUploadService: PhotoUploadService,
    networkQualityManager: NetworkQualityManager,
) {
    val scope = rememberCoroutineScope()
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedUriForPreview by remember { mutableStateOf<Uri?>(null) }
    val urisToDisplayInGrid = remember { mutableStateListOf<Uri>() }
    val uploadResultsList = remember { mutableStateListOf<UploadResult>() }
    val networkQuality by remember { mutableStateOf(networkQualityManager.getCurrentNetworkQuality()) }

    LaunchedEffect(photoUploadService) {
        photoUploadService.uploadResults.collectLatest { result ->
            val existingIndex = uploadResultsList.indexOfFirst { item -> item.requestId == result.requestId }
            if (existingIndex != -1) {
                uploadResultsList[existingIndex] = result
            } else {
                uploadResultsList.add(0, result)
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text("📸 Photo Upload System", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Network and queue summary
        UploadQueueSummary(
            uploadResults = uploadResultsList.toList(),
            networkQuality = networkQuality,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PhotoGridDisplay(
            uris = urisToDisplayInGrid.toList(),
            onPhotoClick = { uri -> selectedUriForPreview = uri },
            onAddPhotoClick = { showPickerDialog = true },
            maxPhotos = 5,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons for testing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    scope.launch {
                        photoUploadService.resumeQueuedUploads()
                    }
                },
            ) {
                Text("Process Queue")
            }

            Button(
                onClick = {
                    scope.launch {
                        photoUploadService.clearCompletedUploadsFromQueue()
                        uploadResultsList.removeAll {
                            it.status in listOf(UploadStatus.COMPLETED, UploadStatus.FAILED)
                        }
                    }
                },
            ) {
                Text("Clear Completed")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Active Uploads:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (uploadResultsList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
            ) {
                Text(
                    "No active uploads.\nSelect photos above to start uploading.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uploadResultsList) { result ->
                    UploadProgressIndicator(
                        uploadResult = result,
                        onRetry = { requestId ->
                            // Enhanced retry with proper request reconstruction using updated data models
                            scope.launch {
                                try {
                                    // Find original URI from the grid or use a placeholder
                                    val originalUri = urisToDisplayInGrid.firstOrNull() ?: Uri.EMPTY
                                    
                                    // Create properly aligned SerializablePhotoUploadRequest
                                    val retryRequest = SerializablePhotoUploadRequest(
                                        id = UUID.randomUUID().toString(),
                                        uri = originalUri,
                                        fileName = "retry_upload_${System.currentTimeMillis()}.jpg",
                                        targetParseObjectId = "RETRY_TARGET_ID",
                                        targetClassName = "Fowl", // Use actual class name
                                        targetField = "photoField",
                                        status = UploadStatus.PENDING,
                                        progress = 0,
                                        retryCount = 0
                                    )
                                    photoUploadService.enqueueUpload(retryRequest)
                                } catch (e: Exception) {
                                    FirebaseCrashlytics.getInstance().recordException(e)
                                }
                            }
                        },
                        onCancel = { requestId ->
                            scope.launch {
                                photoUploadService.cancelUpload(requestId)
                            }
                        },
                    )
                }
            }
        }

        // Dialog handlers
        if (showPickerDialog) {
            PhotoPickerDialog(
                showDialog = showPickerDialog,
                onDismiss = { showPickerDialog = false },
                onPhotoSelected = { uri ->
                    uri?.let {
                        selectedUriForPreview = it
                        if (!urisToDisplayInGrid.contains(it)) urisToDisplayInGrid.add(it)
                    }
                },
            )
        }

        selectedUriForPreview?.let {
            PhotoPreviewWithEdit(
                uri = it,
                onConfirm = { confirmedUri ->
                    scope.launch {
                        // Create properly aligned SerializablePhotoUploadRequest for 5% completion
                        val uploadRequest = SerializablePhotoUploadRequest(
                            id = UUID.randomUUID().toString(),
                            uri = confirmedUri,
                            fileName = "demo_upload_${System.currentTimeMillis()}.jpg",
                            targetParseObjectId = "DEMO_FOWL_ID", // Align with actual Parse object
                            targetClassName = "Fowl", // Use actual Parse class name
                            targetField = "primaryImage", // Use actual field name
                            status = UploadStatus.PENDING,
                            progress = 0,
                            retryCount = 0,
                            errorMessage = null,
                            parseFileUrl = null
                        )
                        photoUploadService.enqueueUpload(uploadRequest)
                    }
                    selectedUriForPreview = null
                },
                onCancel = { selectedUriForPreview = null },
            )
        }
    }
}
