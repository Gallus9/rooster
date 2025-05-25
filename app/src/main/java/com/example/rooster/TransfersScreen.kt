package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser

@Composable
fun TransfersScreen() {
    val transferService = remember { TransferVerificationService() }

    var transfers by remember { mutableStateOf<List<TransferRequest>>(emptyList()) }
    var fowlList by remember { mutableStateOf<List<FowlData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showInitiateDialog by remember { mutableStateOf(false) }
    var selectedTransfer by remember { mutableStateOf<TransferRequest?>(null) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var showHandoverDialog by remember { mutableStateOf(false) }
    var isSellerConfirming by remember { mutableStateOf(false) }

    // Fetch transfers and fowl list on startup
    LaunchedEffect(Unit) {
        isLoading = true

        // Fetch user's transfers
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

        // Fetch user's fowl for initiating transfers
        fowlList = fetchUserFowl()
    }

    // Main screen content
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header
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
                onClick = { showInitiateDialog = true },
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Loading transfers...")
                    }
                }
            }
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            "Error Loading Transfers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            error ?: "Unknown error occurred",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                error = null
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
                            },
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            transfers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "No transfers found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Tap + to initiate your first transfer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { showInitiateDialog = true },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Initiate Transfer")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(transfers) { transfer ->
                        EnhancedTransferCard(
                            transfer = transfer,
                            currentUserId = ParseUser.getCurrentUser()?.objectId ?: "",
                            onActionRequired = { action, isSeller ->
                                selectedTransfer = transfer
                                when (action) {
                                    "verify" -> showVerificationDialog = true
                                    "handover" -> {
                                        isSellerConfirming = isSeller
                                        showHandoverDialog = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showInitiateDialog) {
        InitiateTransferDialog(
            fowlList = fowlList,
            onDismiss = { showInitiateDialog = false },
            onInitiate = { fowlId, buyerId, price, location, sellerDetails ->
                transferService.initiateTransfer(
                    fowlId = fowlId,
                    buyerId = buyerId,
                    agreedPrice = price,
                    transferLocation = location,
                    sellerDetails = sellerDetails,
                    onSuccess = { transferId ->
                        showInitiateDialog = false
                        // Refresh transfers
                        transferService.fetchUserTransfers(
                            onResult = { transfers = it },
                            onError = { error = it },
                        )
                    },
                    onError = { errorMsg ->
                        error = errorMsg
                    },
                )
            },
        )
    }

    selectedTransfer?.let { transfer ->
        if (showVerificationDialog) {
            TransferVerificationDialog(
                transfer = transfer,
                onDismiss = {
                    showVerificationDialog = false
                    selectedTransfer = null
                },
                onVerify = { verification ->
                    transferService.verifyTransferDetails(
                        transferRequestId = transfer.objectId,
                        verification = verification,
                        onSuccess = {
                            showVerificationDialog = false
                            selectedTransfer = null
                            // Refresh transfers
                            transferService.fetchUserTransfers(
                                onResult = { transfers = it },
                                onError = { error = it },
                            )
                        },
                        onError = { errorMsg ->
                            error = errorMsg
                            showVerificationDialog = false
                            selectedTransfer = null
                        },
                    )
                },
            )
        }

        if (showHandoverDialog) {
            HandoverConfirmationDialog(
                transfer = transfer,
                isSellerConfirming = isSellerConfirming,
                onDismiss = {
                    showHandoverDialog = false
                    selectedTransfer = null
                },
                onConfirm = { handoverConfirmation ->
                    transferService.confirmHandover(
                        transferRequestId = transfer.objectId,
                        handoverConfirmation = handoverConfirmation,
                        isSellerConfirming = isSellerConfirming,
                        onSuccess = {
                            showHandoverDialog = false
                            selectedTransfer = null
                            // Refresh transfers
                            transferService.fetchUserTransfers(
                                onResult = { transfers = it },
                                onError = { error = it },
                            )
                        },
                        onError = { errorMsg ->
                            error = errorMsg
                            showHandoverDialog = false
                            selectedTransfer = null
                        },
                    )
                },
            )
        }
    }
}

@Composable
fun EnhancedTransferCard(
    transfer: TransferRequest,
    currentUserId: String,
    onActionRequired: (String, Boolean) -> Unit,
) {
    val isSeller = transfer.sellerId == currentUserId
    val isBuyer = transfer.buyerId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header with bird info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transfer.sellerDetails.birdName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${transfer.sellerDetails.birdType} • ${transfer.sellerDetails.age} weeks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSeller) "You are the seller" else "You are the buyer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }

                TransferStatusChip(status = transfer.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price and dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Price: $${transfer.agreedPrice}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Started: ${formatDate(transfer.initiatedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    transfer.completedDate?.let { date ->
                        Text(
                            text = "Completed: ${formatDate(date)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Action buttons based on status and user role
            Spacer(modifier = Modifier.height(12.dp))

            when (transfer.status) {
                TransferStatus.INITIATED -> {
                    if (isBuyer) {
                        Button(
                            onClick = { onActionRequired("verify", false) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Transfer Details")
                        }
                    }
                }

                TransferStatus.BUYER_VERIFIED -> {
                    if (isSeller) {
                        Button(
                            onClick = { onActionRequired("handover", true) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Handshake, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Handover")
                        }
                    }
                }

                TransferStatus.HANDOVER_CONFIRMED -> {
                    if ((isSeller && transfer.handoverConfirmation?.sellerConfirmedDate == null) ||
                        (isBuyer && transfer.handoverConfirmation?.buyerConfirmedDate == null)
                    ) {
                        Button(
                            onClick = { onActionRequired("handover", isSeller) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Handshake, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Complete Handover")
                        }
                    }
                }

                TransferStatus.COMPLETED -> {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E8),
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Transfer completed successfully",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                TransferStatus.DISPUTED -> {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE),
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Transfer is disputed - Contact support",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                TransferStatus.CANCELLED -> {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE),
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Transfer was cancelled",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Status: ${transfer.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Transfer location if available
            transfer.transferLocation?.let { location ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun fetchUserFowl(): List<FowlData> {
    return try {
        val query = ParseQuery.getQuery<ParseObject>("Fowl")
        query.whereEqualTo("owner", ParseUser.getCurrentUser())
        query.orderByDescending("createdAt")
        val result = query.find()
        result.map { fowl ->
            FowlData(
                objectId = fowl.objectId,
                name = fowl.getString("name") ?: "Unknown",
                type = fowl.getString("type") ?: "Unknown",
                birthDate = fowl.getString("birthDate") ?: "Unknown",
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun formatDate(date: java.util.Date): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(date)
}

@Preview(showBackground = true)
@Composable
fun TransfersScreenPreview() {
    TransfersScreen()
}
