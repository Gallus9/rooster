package com.example.rooster

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitiateTransferDialog(
    fowlList: List<FowlData>,
    onDismiss: () -> Unit,
    onInitiate: (String, String?, Double, String?, BirdTransferDetails) -> Unit,
) {
    var selectedFowl by remember { mutableStateOf<FowlData?>(null) }
    var buyerUsername by remember { mutableStateOf("") }
    var agreedPrice by remember { mutableStateOf("") }
    var transferLocation by remember { mutableStateOf("") }
    var showFowlDetails by remember { mutableStateOf(false) }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val photoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
        ) { uris ->
            photoUris = uris
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Initiate Transfer") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    // Fowl selection
                    Column {
                        Text("Select Fowl:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = showFowlDetails,
                            onExpandedChange = { showFowlDetails = it },
                        ) {
                            OutlinedTextField(
                                value = selectedFowl?.name ?: "Select a fowl",
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFowlDetails)
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = showFowlDetails,
                                onDismissRequest = { showFowlDetails = false },
                            ) {
                                fowlList.forEach { fowl ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(fowl.name)
                                                Text(
                                                    "${fowl.type} • Born: ${fowl.birthDate}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedFowl = fowl
                                            showFowlDetails = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = buyerUsername,
                        onValueChange = { buyerUsername = it },
                        label = { Text("Buyer Username (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = agreedPrice,
                        onValueChange = { agreedPrice = it },
                        label = { Text("Agreed Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = transferLocation,
                        onValueChange = { transferLocation = it },
                        label = { Text("Transfer Location") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Column {
                        Text("Photos (Required):", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (photoUris.isEmpty()) {
                            Button(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Photos")
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(photoUris) { uri ->
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        modifier =
                                            Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                item {
                                    Button(
                                        onClick = { photoLauncher.launch("image/*") },
                                        modifier = Modifier.size(60.dp),
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add more")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedFowl?.let { fowl ->
                        val price = agreedPrice.toDoubleOrNull() ?: 0.0
                        if (price > 0 && photoUris.isNotEmpty()) {
                            val milestoneService = MilestoneTrackingService()
                            val currentAge = milestoneService.calculateAgeInWeeks(fowl.birthDate)

                            val sellerDetails =
                                BirdTransferDetails(
                                    birdName = fowl.name,
                                    birdType = fowl.type,
                                    age = currentAge,
                                    color = "Color to be verified",
                                    gender = fowl.type,
                                    weight = null,
                                    height = null,
                                    healthStatus = "Healthy",
                                    vaccinationStatus = "Up to date",
                                    breedingHistory = null,
                                    specialCharacteristics = null,
                                    transferPhotos = emptyList(),
                                    veterinaryCertificate = null,
                                    recordedTimestamp = Date(),
                                    recordedLocation = transferLocation.takeIf { it.isNotBlank() },
                                )

                            onInitiate(
                                fowl.objectId,
                                buyerUsername.takeIf { it.isNotBlank() },
                                price,
                                transferLocation.takeIf { it.isNotBlank() },
                                sellerDetails,
                            )
                        }
                    }
                },
                enabled =
                    selectedFowl != null &&
                        agreedPrice.toDoubleOrNull() != null &&
                        (agreedPrice.toDoubleOrNull() ?: 0.0) > 0 &&
                        photoUris.isNotEmpty(),
            ) {
                Text("Initiate Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun TransferListScreen(
    transferService: TransferVerificationService,
    onTransferClick: (TransferRequest) -> Unit,
    onInitiateTransfer: () -> Unit,
) {
    var transfers by remember { mutableStateOf<List<TransferRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        transferService.fetchUserTransfers(
            onResult = {
                transfers = it
                isLoading = false
            },
            onError = {
                error = it
                isLoading = false
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Transfer Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            FloatingActionButton(
                onClick = onInitiateTransfer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Initiate Transfer")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }

            transfers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No transfers found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Tap + to initiate a new transfer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(transfers) { transfer ->
                        TransferCard(
                            transfer = transfer,
                            onClick = { onTransferClick(transfer) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransferCard(
    transfer: TransferRequest,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = transfer.sellerDetails.birdName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${transfer.sellerDetails.birdType} • ${transfer.sellerDetails.age} weeks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TransferStatusChip(status = transfer.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Price: $${transfer.agreedPrice}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50),
                )
                Text(
                    text = "Started: ${formatDate(transfer.initiatedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun TransferStatusChip(status: TransferStatus) {
    val (backgroundColor, textColor, text) =
        when (status) {
            TransferStatus.INITIATED -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "Initiated")
            TransferStatus.PENDING_BUYER_VERIFICATION ->
                Triple(
                    Color(0xFFFFF3E0),
                    Color(0xFFEF6C00),
                    "Pending Verification",
                )

            TransferStatus.BUYER_VERIFIED -> Triple(Color(0xFFE8F5E8), Color(0xFF2E7D32), "Verified")
            TransferStatus.PENDING_HANDOVER ->
                Triple(
                    Color(0xFFFFF3E0),
                    Color(0xFFEF6C00),
                    "Pending Handover",
                )

            TransferStatus.HANDOVER_CONFIRMED ->
                Triple(
                    Color(0xFFE8F5E8),
                    Color(0xFF2E7D32),
                    "Handover Confirmed",
                )

            TransferStatus.COMPLETED -> Triple(Color(0xFF4CAF50), Color.White, "Completed")
            TransferStatus.CANCELLED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Cancelled")
            TransferStatus.DISPUTED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Disputed")
        }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(4.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun TransferVerificationDialog(
    transfer: TransferRequest,
    onDismiss: () -> Unit,
    onVerify: (BirdVerificationDetails) -> Unit,
) {
    var colorMatch by remember { mutableStateOf(false) }
    var ageMatch by remember { mutableStateOf(false) }
    var genderMatch by remember { mutableStateOf(false) }
    var weightMatch by remember { mutableStateOf(false) }
    var heightMatch by remember { mutableStateOf(false) }
    var healthMatch by remember { mutableStateOf(false) }
    var buyerNotes by remember { mutableStateOf("") }
    var verificationPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val photoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
        ) { uris ->
            verificationPhotos = uris
        }

    val overallMatch =
        listOf(colorMatch, ageMatch, genderMatch, weightMatch, healthMatch).count { it } >= 4

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify Transfer Details") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                "Bird Details from Seller:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Name: ${transfer.sellerDetails.birdName}")
                            Text("Type: ${transfer.sellerDetails.birdType}")
                            Text("Age: ${transfer.sellerDetails.age} weeks")
                            Text("Color: ${transfer.sellerDetails.color}")
                            Text("Health: ${transfer.sellerDetails.healthStatus}")
                        }
                    }
                }

                item {
                    Text(
                        "Verify each detail matches the actual bird:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }

                item {
                    VerificationCheckItem(
                        label = "Color matches description",
                        checked = colorMatch,
                        onCheckedChange = { colorMatch = it },
                    )
                }

                item {
                    VerificationCheckItem(
                        label = "Age appears correct",
                        checked = ageMatch,
                        onCheckedChange = { ageMatch = it },
                    )
                }

                item {
                    VerificationCheckItem(
                        label = "Gender matches",
                        checked = genderMatch,
                        onCheckedChange = { genderMatch = it },
                    )
                }

                item {
                    VerificationCheckItem(
                        label = "Weight appears appropriate",
                        checked = weightMatch,
                        onCheckedChange = { weightMatch = it },
                    )
                }

                item {
                    VerificationCheckItem(
                        label = "Health condition matches",
                        checked = healthMatch,
                        onCheckedChange = { healthMatch = it },
                    )
                }

                item {
                    OutlinedTextField(
                        value = buyerNotes,
                        onValueChange = { buyerNotes = it },
                        label = { Text("Additional Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }

                item {
                    Column {
                        Text("Verification Photos:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (verificationPhotos.isEmpty()) {
                            Button(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Verification Photos")
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(verificationPhotos) { uri ->
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        modifier =
                                            Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                item {
                                    Button(
                                        onClick = { photoLauncher.launch("image/*") },
                                        modifier = Modifier.size(60.dp),
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add more")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (overallMatch) {
                                        Color(0xFFE8F5E8)
                                    } else {
                                        Color(0xFFFFEBEE)
                                    },
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (overallMatch) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (overallMatch) Color(0xFF2E7D32) else Color(0xFFC62828),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (overallMatch) "Verification Passed" else "Verification Failed - Consider disputing",
                                color = if (overallMatch) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val verification =
                        BirdVerificationDetails(
                            verifiedDate = Date(),
                            colorMatch = colorMatch,
                            ageMatch = ageMatch,
                            genderMatch = genderMatch,
                            weightMatch = weightMatch,
                            heightMatch = heightMatch,
                            healthMatch = healthMatch,
                            overallMatch = overallMatch,
                            verificationPhotos = emptyList(),
                            buyerNotes = buyerNotes.takeIf { it.isNotBlank() },
                            discrepancies =
                                buildList {
                                    if (!colorMatch) add("Color mismatch")
                                    if (!ageMatch) add("Age discrepancy")
                                    if (!genderMatch) add("Gender mismatch")
                                    if (!weightMatch) add("Weight concern")
                                    if (!healthMatch) add("Health concern")
                                },
                            verificationScore =
                                listOf(
                                    colorMatch,
                                    ageMatch,
                                    genderMatch,
                                    weightMatch,
                                    healthMatch,
                                ).count { it } * 20,
                            verificationLocation = null,
                            fraudCheckPassed = true,
                        )
                    onVerify(verification)
                },
                enabled = verificationPhotos.isNotEmpty(),
            ) {
                Text("Submit Verification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun VerificationCheckItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun HandoverConfirmationDialog(
    transfer: TransferRequest,
    isSellerConfirming: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (HandoverConfirmation) -> Unit,
) {
    var location by remember { mutableStateOf("") }
    var paymentConfirmed by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("") }
    var witnessPresent by remember { mutableStateOf(false) }
    var witnessName by remember { mutableStateOf("") }
    var witnessContact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val photoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
        ) { uris ->
            photos = uris
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isSellerConfirming) "Confirm Handover - Seller" else "Confirm Handover - Buyer")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Handover Location") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (!isSellerConfirming) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = paymentConfirmed,
                                onCheckedChange = { paymentConfirmed = it },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Payment completed")
                        }
                    }

                    item {
                        AnimatedVisibility(visible = paymentConfirmed) {
                            OutlinedTextField(
                                value = paymentMethod,
                                onValueChange = { paymentMethod = it },
                                label = { Text("Payment Method") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = witnessPresent,
                            onCheckedChange = { witnessPresent = it },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Witness present")
                    }
                }

                item {
                    AnimatedVisibility(visible = witnessPresent) {
                        Column {
                            OutlinedTextField(
                                value = witnessName,
                                onValueChange = { witnessName = it },
                                label = { Text("Witness Name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = witnessContact,
                                onValueChange = { witnessContact = it },
                                label = { Text("Witness Contact") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Additional Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }

                item {
                    Column {
                        Text(
                            "Handover Photos (Required):",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (photos.isEmpty()) {
                            Button(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Photos")
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(photos) { uri ->
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        modifier =
                                            Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                item {
                                    Button(
                                        onClick = { photoLauncher.launch("image/*") },
                                        modifier = Modifier.size(60.dp),
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add more")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val handoverConfirmation =
                        HandoverConfirmation(
                            sellerConfirmedDate = if (isSellerConfirming) Date() else null,
                            buyerConfirmedDate = if (!isSellerConfirming) Date() else null,
                            handoverLocation = location,
                            handoverLocationLat = 0.0,
                            handoverLocationLng = 0.0,
                            sellerPhotos = if (isSellerConfirming) emptyList() else emptyList(),
                            buyerPhotos = if (!isSellerConfirming) emptyList() else emptyList(),
                            sellerSignature = null,
                            buyerSignature = null,
                            witnessPresent = witnessPresent,
                            witnessName = witnessName.takeIf { it.isNotBlank() },
                            witnessContact = witnessContact.takeIf { it.isNotBlank() },
                            paymentConfirmed = if (!isSellerConfirming) paymentConfirmed else false,
                            paymentMethod = paymentMethod.takeIf { it.isNotBlank() },
                            finalNotes = notes.takeIf { it.isNotBlank() },
                        )
                    onConfirm(handoverConfirmation)
                },
                enabled =
                    location.isNotBlank() && photos.isNotEmpty() &&
                        (!isSellerConfirming || paymentConfirmed || !paymentConfirmed),
            ) {
                Text("Confirm Handover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun formatDate(date: Date): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(date)
}
