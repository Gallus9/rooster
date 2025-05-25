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

@Composable
fun UploadProgressIndicator(
    uploadResult: UploadResult, // Changed to use UploadResult DTO
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
            // Simplified: Removed AsyncImage for brevity, UI should show filename or generic icon
            // If URI is needed, it must be part of UploadResult or fetched via requestId
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upload ID: ${uploadResult.requestId.take(8)}...", // Display requestId
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                if (uploadResult.status == UploadStatus.UPLOADING || uploadResult.status == UploadStatus.RETRYING) {
                    LinearProgressIndicator(
                        progress = { uploadResult.progress / 100f }, // Updated for new M3 API
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                    )
                    Text("${uploadResult.progress}%", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(uploadResult.status.name, style = MaterialTheme.typography.bodySmall)
                }
                uploadResult.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            when (uploadResult.status) {
                UploadStatus.FAILED, UploadStatus.LINKING_FAILED -> {
                    IconButton(onClick = { onRetry(uploadResult.requestId) }) {
                        Icon(Icons.Filled.Refresh, "Retry")
                    }
                }
                UploadStatus.UPLOADING, UploadStatus.RETRYING, UploadStatus.PENDING, UploadStatus.QUEUED -> {
                    IconButton(onClick = { onCancel(uploadResult.requestId) }) {
                        Icon(Icons.Filled.Cancel, "Cancel")
                    }
                }
                UploadStatus.COMPLETED -> {
                    Icon(Icons.Filled.CheckCircle, "Completed", tint = MaterialTheme.colorScheme.primary)
                }
                else -> {}
            }
        }
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
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
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

@Composable
fun PhotoUploadComponentsDemoScreen(photoUploadService: PhotoUploadService, networkQualityManager: NetworkQualityManager /* Added */) {
    val scope = rememberCoroutineScope()
    var showPickerDialog by remember { mutableStateOf(false) }
    var selectedUriForPreview by remember { mutableStateOf<Uri?>(null) }
    val urisToDisplayInGrid = remember { mutableStateListOf<Uri>() }
    val uploadResultsList = remember { mutableStateListOf<UploadResult>() }

    LaunchedEffect(photoUploadService) {
        photoUploadService.uploadResults.collectLatest { result ->
            val existingIndex = uploadResultsList.indexOfFirst { item -> item.requestId == result.requestId }
            if (existingIndex != -1) {
                uploadResultsList[existingIndex] = result
            } else {
                uploadResultsList.add(0, result)
            }
            if (result.isSuccess && result.status == UploadStatus.COMPLETED) {
                // Find the original URI from a displayed list if needed, for now just log
                // urisToDisplayInGrid.removeIf { it.toString() == result.requestId } // This logic is flawed, requestId is not URI
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text("Photo Upload Demo", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        PhotoGridDisplay(
            uris = urisToDisplayInGrid.toList(),
            onPhotoClick = { uri -> selectedUriForPreview = uri },
            onAddPhotoClick = { showPickerDialog = true },
            maxPhotos = 5,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Upload Queue/Progress:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (uploadResultsList.isEmpty()) {
            Text("No active or recent uploads.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uploadResultsList) { result ->
                    UploadProgressIndicator(
                        uploadResult = result,
                        onRetry = { requestId ->
                            // To retry, we need original request details.
                            // This demo would need to store SerializablePhotoUploadRequest s or reconstruct them.
                            // For simplicity, this part is a TODO for full retry implementation.
                            println("Retry requested for $requestId - full re-enqueue logic needed.")
                            // Example: find original DTO by ID, then call enqueueUpload
                        },
                        onCancel = { requestId -> scope.launch { photoUploadService.cancelUpload(requestId) } },
                    )
                }
            }
        }

        if (showPickerDialog) {
            PhotoPickerDialog(
                showDialog = showPickerDialog,
                onDismiss = { showPickerDialog = false },
                onPhotoSelected = { uri ->
                    uri?.let {
                        selectedUriForPreview = it
                        if (!urisToDisplayInGrid.contains(it)) urisToDisplayInGrid.add(it) // Add to grid for display
                    }
                },
            )
        }

        selectedUriForPreview?.let {
            PhotoPreviewWithEdit(
                uri = it,
                onConfirm = { confirmedUri ->
                    scope.launch {
                        // Construct SerializablePhotoUploadRequest for the service
                        val requestDto =
                            SerializablePhotoUploadRequest(
                                id = UUID.randomUUID().toString(), // Generate new ID for each enqueue
                                uri = confirmedUri,
                                fileName = "demo_upload_${System.currentTimeMillis()}.jpg",
                                // These need to be actual IDs and class names for linking to work
                                targetParseObjectId = "DUMMY_TARGET_ID",
                                targetClassName = "DUMMY_CLASS_NAME",
                                targetField = "photoField",
                            )
                        photoUploadService.enqueueUpload(requestDto)
                    }
                    selectedUriForPreview = null
                },
                onCancel = { selectedUriForPreview = null },
            )
        }
    }
}
