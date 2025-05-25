package com.example.rooster

import com.parse.ParseACL
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import java.security.MessageDigest
import java.util.Date
import java.util.UUID

class TransferVerificationService {
    companion object {
        private const val VERIFICATION_TIMEOUT_DAYS = 7
        private const val MIN_VERIFICATION_SCORE = 80
        private const val LOCATION_ACCURACY_THRESHOLD = 100.0 // meters
    }

    // Initiate transfer request from seller
    fun initiateTransfer(
        fowlId: String,
        buyerId: String?,
        agreedPrice: Double,
        transferLocation: String?,
        sellerDetails: BirdTransferDetails,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            // Validate seller owns the fowl
            val fowlOwnership =
                validateFowlOwnership(fowlId, ParseUser.getCurrentUser()?.objectId ?: "")
            if (!fowlOwnership) {
                onError("You do not own this fowl or it's not available for transfer")
                return
            }

            // Check if fowl has active transfer
            val activeTransfer = checkActiveTransfer(fowlId)
            if (activeTransfer != null) {
                onError("This fowl already has an active transfer request")
                return
            }

            // Generate fraud prevention data
            val fraudData = generateTransferFraudData()

            // Create transfer request
            val transferRequest =
                TransferRequest(
                    objectId = "",
                    fowlId = fowlId,
                    sellerId = ParseUser.getCurrentUser()?.objectId ?: "",
                    buyerId = buyerId,
                    status = TransferStatus.INITIATED,
                    initiatedDate = Date(),
                    completedDate = null,
                    agreedPrice = agreedPrice,
                    currency = "USD",
                    transferLocation = transferLocation,
                    transferLocationLat = null,
                    transferLocationLng = null,
                    sellerDetails = sellerDetails,
                    buyerVerification = null,
                    handoverConfirmation = null,
                    fraudPreventionData = fraudData,
                    notes = null,
                    isActive = true,
                )

            // Save to Parse
            val parseObject = TransferParser.createTransferRequest(transferRequest)
            val acl = ParseACL(ParseUser.getCurrentUser())
            acl.setPublicReadAccess(false)
            acl.setReadAccess(ParseUser.getCurrentUser(), true)
            acl.setWriteAccess(ParseUser.getCurrentUser(), true)
            buyerId?.let { acl.setReadAccess(it, true) }
            parseObject.acl = acl

            parseObject.saveInBackground { e ->
                if (e != null) {
                    onError(e.localizedMessage ?: "Failed to initiate transfer")
                } else {
                    // Send notification to buyer if specified
                    buyerId?.let {
                        sendTransferNotification(
                            parseObject.objectId,
                            it,
                            TransferNotificationType.TRANSFER_INITIATED,
                        )
                    }
                    onSuccess(parseObject.objectId)
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to initiate transfer")
        }
    }

    // Buyer verification of transfer details
    fun verifyTransferDetails(
        transferRequestId: String,
        verification: BirdVerificationDetails,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("TransferRequest")
            query.getInBackground(transferRequestId) { transferObj, e ->
                if (e != null) {
                    onError(e.localizedMessage ?: "Transfer request not found")
                    return@getInBackground
                }

                if (transferObj == null) {
                    onError("Transfer request not found")
                    return@getInBackground
                }

                // Verify buyer permissions
                val buyerId = transferObj.getString("buyerId")
                if (buyerId != ParseUser.getCurrentUser()?.objectId) {
                    onError("You are not authorized to verify this transfer")
                    return@getInBackground
                }

                // Check current status
                val currentStatus =
                    TransferStatus.valueOf(
                        transferObj.getString("status") ?: TransferStatus.INITIATED.name,
                    )
                if (currentStatus != TransferStatus.INITIATED && currentStatus != TransferStatus.PENDING_BUYER_VERIFICATION) {
                    onError("Transfer is not in verification stage")
                    return@getInBackground
                }

                // Update verification details
                transferObj.put(
                    "buyerVerification",
                    TransferParser.mapBuyerVerification(verification),
                )
                transferObj.put(
                    "status",
                    if (verification.overallMatch) {
                        TransferStatus.BUYER_VERIFIED.name
                    } else {
                        TransferStatus.DISPUTED.name
                    },
                )

                transferObj.saveInBackground { saveError ->
                    if (saveError != null) {
                        onError(saveError.localizedMessage ?: "Failed to save verification")
                    } else {
                        // Send notification to seller
                        val sellerId = transferObj.getString("sellerId")
                        sellerId?.let {
                            sendTransferNotification(
                                transferRequestId,
                                it,
                                if (verification.overallMatch) {
                                    TransferNotificationType.VERIFICATION_COMPLETED
                                } else {
                                    TransferNotificationType.DISPUTE_RAISED
                                },
                            )
                        }
                        onSuccess()
                    }
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to verify transfer")
        }
    }

    // Confirm handover (both parties must confirm)
    fun confirmHandover(
        transferRequestId: String,
        handoverConfirmation: HandoverConfirmation,
        isSellerConfirming: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("TransferRequest")
            query.getInBackground(transferRequestId) { transferObj, e ->
                if (e != null) {
                    onError(e.localizedMessage ?: "Transfer request not found")
                    return@getInBackground
                }

                if (transferObj == null) {
                    onError("Transfer request not found")
                    return@getInBackground
                }

                // Update handover confirmation
                val existingHandover = transferObj.get("handoverConfirmation") as? Map<String, Any>
                val updatedHandover =
                    if (existingHandover != null) {
                        val existing = TransferParser.parseHandoverConfirmation(existingHandover)
                        if (isSellerConfirming) {
                            existing.copy(
                                sellerConfirmedDate = Date(),
                                sellerPhotos = handoverConfirmation.sellerPhotos,
                                sellerSignature = handoverConfirmation.sellerSignature,
                                handoverLocation = handoverConfirmation.handoverLocation,
                                handoverLocationLat = handoverConfirmation.handoverLocationLat,
                                handoverLocationLng = handoverConfirmation.handoverLocationLng,
                            )
                        } else {
                            existing.copy(
                                buyerConfirmedDate = Date(),
                                buyerPhotos = handoverConfirmation.buyerPhotos,
                                buyerSignature = handoverConfirmation.buyerSignature,
                                paymentConfirmed = handoverConfirmation.paymentConfirmed,
                                paymentMethod = handoverConfirmation.paymentMethod,
                            )
                        }
                    } else {
                        if (isSellerConfirming) {
                            handoverConfirmation.copy(sellerConfirmedDate = Date())
                        } else {
                            handoverConfirmation.copy(buyerConfirmedDate = Date())
                        }
                    }

                transferObj.put(
                    "handoverConfirmation",
                    TransferParser.mapHandoverConfirmation(updatedHandover),
                )

                // Check if both parties have confirmed
                val bothConfirmed =
                    updatedHandover.sellerConfirmedDate != null &&
                        updatedHandover.buyerConfirmedDate != null

                if (bothConfirmed) {
                    transferObj.put("status", TransferStatus.COMPLETED.name)
                    transferObj.put("completedDate", Date())

                    // Create ownership record
                    createOwnershipRecord(transferObj) { success ->
                        if (success) {
                            // Update fowl ownership
                            updateFowlOwnership(transferObj)
                        }
                    }
                } else {
                    transferObj.put("status", TransferStatus.HANDOVER_CONFIRMED.name)
                }

                transferObj.saveInBackground { saveError ->
                    if (saveError != null) {
                        onError(saveError.localizedMessage ?: "Failed to confirm handover")
                    } else {
                        // Send appropriate notifications
                        if (bothConfirmed) {
                            sendCompletionNotifications(transferObj)
                        } else {
                            sendHandoverNotification(transferObj, isSellerConfirming)
                        }
                        onSuccess()
                    }
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to confirm handover")
        }
    }

    // Fetch transfer requests for current user
    fun fetchUserTransfers(
        onResult: (List<TransferRequest>) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val currentUserId = ParseUser.getCurrentUser()?.objectId ?: ""
            val query = ParseQuery.getQuery<ParseObject>("TransferRequest")

            // Get transfers where user is seller or buyer
            val sellerQuery = ParseQuery.getQuery<ParseObject>("TransferRequest")
            sellerQuery.whereEqualTo("sellerId", currentUserId)

            val buyerQuery = ParseQuery.getQuery<ParseObject>("TransferRequest")
            buyerQuery.whereEqualTo("buyerId", currentUserId)

            val mainQuery = ParseQuery.or(listOf(sellerQuery, buyerQuery))
            mainQuery.whereEqualTo("isActive", true)
            mainQuery.orderByDescending("initiatedDate")

            mainQuery.findInBackground { objects, e ->
                if (e != null) {
                    onError(e.localizedMessage ?: "Failed to fetch transfers")
                } else {
                    val transfers =
                        objects?.map { TransferParser.parseObjectToTransferRequest(it) }
                            ?: emptyList()
                    onResult(transfers)
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to fetch transfers")
        }
    }

    // Cancel transfer (only initiator can cancel before verification)
    fun cancelTransfer(
        transferRequestId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("TransferRequest")
            query.getInBackground(transferRequestId) { transferObj, e ->
                if (e != null) {
                    onError(e.localizedMessage ?: "Transfer request not found")
                    return@getInBackground
                }

                if (transferObj == null) {
                    onError("Transfer request not found")
                    return@getInBackground
                }

                // Only seller can cancel
                val sellerId = transferObj.getString("sellerId")
                if (sellerId != ParseUser.getCurrentUser()?.objectId) {
                    onError("Only the seller can cancel this transfer")
                    return@getInBackground
                }

                // Check if cancellation is allowed
                val status =
                    TransferStatus.valueOf(
                        transferObj.getString("status") ?: TransferStatus.INITIATED.name,
                    )
                if (status == TransferStatus.COMPLETED || status == TransferStatus.CANCELLED) {
                    onError("Transfer cannot be cancelled at this stage")
                    return@getInBackground
                }

                transferObj.put("status", TransferStatus.CANCELLED.name)
                transferObj.put("notes", reason)
                transferObj.put("isActive", false)

                transferObj.saveInBackground { saveError ->
                    if (saveError != null) {
                        onError(saveError.localizedMessage ?: "Failed to cancel transfer")
                    } else {
                        // Send cancellation notification
                        val buyerId = transferObj.getString("buyerId")
                        buyerId?.let {
                            sendTransferNotification(
                                transferRequestId,
                                it,
                                TransferNotificationType.TRANSFER_CANCELLED,
                            )
                        }
                        onSuccess()
                    }
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to cancel transfer")
        }
    }

    // Private helper functions

    private fun validateFowlOwnership(
        fowlId: String,
        userId: String,
    ): Boolean {
        return try {
            val query = ParseQuery.getQuery<ParseObject>("Fowl")
            val fowl = query.get(fowlId)
            val owner = fowl.getParseObject("owner")
            owner?.objectId == userId
        } catch (e: Exception) {
            false
        }
    }

    private fun checkActiveTransfer(fowlId: String): TransferRequest? {
        return try {
            val query = ParseQuery.getQuery<ParseObject>("TransferRequest")
            query.whereEqualTo("fowlId", fowlId)
            query.whereEqualTo("isActive", true)
            val result = query.first
            if (result != null) {
                TransferParser.parseObjectToTransferRequest(result)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generateTransferFraudData(): Map<String, Any> {
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "sessionId" to UUID.randomUUID().toString(),
            "deviceInfo" to "android_device",
            "appVersion" to "1.0.0",
            "ipHash" to generateHash("placeholder_ip"),
            "locationAccuracy" to LOCATION_ACCURACY_THRESHOLD,
        )
    }

    private fun generateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun generateVerificationHash(transferRequest: TransferRequest): String {
        val hashInput =
            "${transferRequest.fowlId}-${transferRequest.sellerId}-${transferRequest.buyerId}-${transferRequest.initiatedDate.time}"
        return generateHash(hashInput)
    }

    private fun createOwnershipRecord(
        transferObj: ParseObject,
        onComplete: (Boolean) -> Unit,
    ) {
        try {
            val ownershipRecord =
                OwnershipRecord(
                    objectId = "",
                    fowlId = transferObj.getString("fowlId") ?: "",
                    previousOwnerId = transferObj.getString("sellerId") ?: "",
                    newOwnerId = transferObj.getString("buyerId") ?: "",
                    transferRequestId = transferObj.objectId,
                    transferDate = Date(),
                    transferPrice = transferObj.getDouble("agreedPrice"),
                    transferLocation = transferObj.getString("transferLocation") ?: "",
                    verificationHash =
                        generateVerificationHash(
                            TransferParser.parseObjectToTransferRequest(
                                transferObj,
                            ),
                        ),
                    blockchainTxId = null,
                    isReversible = false,
                    legalDocuments = emptyList(),
                )

            val parseObject = TransferParser.createOwnershipRecord(ownershipRecord)
            val acl = ParseACL()
            acl.setPublicReadAccess(true)
            acl.setWriteAccess(ParseUser.getCurrentUser(), false)
            parseObject.acl = acl

            parseObject.saveInBackground { e ->
                onComplete(e == null)
            }
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    private fun updateFowlOwnership(transferObj: ParseObject) {
        try {
            val fowlId = transferObj.getString("fowlId") ?: return
            val newOwnerId = transferObj.getString("buyerId") ?: return

            val query = ParseQuery.getQuery<ParseObject>("Fowl")
            query.getInBackground(fowlId) { fowl, e ->
                if (e == null && fowl != null) {
                    val newOwner = ParseObject.createWithoutData("_User", newOwnerId)
                    fowl.put("owner", newOwner)
                    fowl.saveInBackground()
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the transfer
        }
    }

    private fun sendTransferNotification(
        transferRequestId: String,
        recipientId: String,
        notificationType: TransferNotificationType,
    ) {
        try {
            val notification =
                TransferNotification(
                    objectId = "",
                    recipientId = recipientId,
                    senderId = ParseUser.getCurrentUser()?.objectId ?: "",
                    transferRequestId = transferRequestId,
                    notificationType = notificationType,
                    title = getNotificationTitle(notificationType),
                    message = getNotificationMessage(notificationType),
                    actionRequired = isActionRequired(notificationType),
                    isRead = false,
                    createdDate = Date(),
                    expiryDate = getExpiryDate(notificationType),
                    metadata = emptyMap(),
                )

            val parseObject = TransferParser.createTransferNotification(notification)
            parseObject.saveInBackground()
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    private fun sendCompletionNotifications(transferObj: ParseObject) {
        val sellerId = transferObj.getString("sellerId")
        val buyerId = transferObj.getString("buyerId")
        val transferId = transferObj.objectId

        sellerId?.let {
            sendTransferNotification(transferId, it, TransferNotificationType.TRANSFER_COMPLETED)
        }
        buyerId?.let {
            sendTransferNotification(transferId, it, TransferNotificationType.TRANSFER_COMPLETED)
        }
    }

    private fun sendHandoverNotification(
        transferObj: ParseObject,
        isSellerConfirming: Boolean,
    ) {
        val sellerId = transferObj.getString("sellerId")
        val buyerId = transferObj.getString("buyerId")
        val transferId = transferObj.objectId

        if (isSellerConfirming) {
            buyerId?.let {
                sendTransferNotification(
                    transferId,
                    it,
                    TransferNotificationType.HANDOVER_CONFIRMED,
                )
            }
        } else {
            sellerId?.let {
                sendTransferNotification(
                    transferId,
                    it,
                    TransferNotificationType.HANDOVER_CONFIRMED,
                )
            }
        }
    }

    private fun getNotificationTitle(type: TransferNotificationType): String {
        return when (type) {
            TransferNotificationType.TRANSFER_INITIATED -> "New Transfer Request"
            TransferNotificationType.VERIFICATION_REQUIRED -> "Verification Required"
            TransferNotificationType.VERIFICATION_COMPLETED -> "Transfer Verified"
            TransferNotificationType.HANDOVER_SCHEDULED -> "Handover Scheduled"
            TransferNotificationType.HANDOVER_CONFIRMED -> "Handover Confirmed"
            TransferNotificationType.TRANSFER_COMPLETED -> "Transfer Completed"
            TransferNotificationType.TRANSFER_CANCELLED -> "Transfer Cancelled"
            TransferNotificationType.DISPUTE_RAISED -> "Transfer Dispute"
        }
    }

    private fun getNotificationMessage(type: TransferNotificationType): String {
        return when (type) {
            TransferNotificationType.TRANSFER_INITIATED -> "You have received a new transfer request. Please review and verify the details."
            TransferNotificationType.VERIFICATION_REQUIRED -> "Please verify the transfer details and confirm the bird information."
            TransferNotificationType.VERIFICATION_COMPLETED -> "The buyer has verified the transfer details. Ready for handover."
            TransferNotificationType.HANDOVER_SCHEDULED -> "Handover has been scheduled. Please confirm your attendance."
            TransferNotificationType.HANDOVER_CONFIRMED -> "The other party has confirmed the handover. Please complete your confirmation."
            TransferNotificationType.TRANSFER_COMPLETED -> "Transfer has been completed successfully. Ownership has been updated."
            TransferNotificationType.TRANSFER_CANCELLED -> "The transfer has been cancelled by the seller."
            TransferNotificationType.DISPUTE_RAISED -> "A dispute has been raised regarding this transfer. Please review."
        }
    }

    private fun isActionRequired(type: TransferNotificationType): Boolean {
        return when (type) {
            TransferNotificationType.TRANSFER_INITIATED,
            TransferNotificationType.VERIFICATION_REQUIRED,
            TransferNotificationType.HANDOVER_SCHEDULED,
            TransferNotificationType.HANDOVER_CONFIRMED,
            TransferNotificationType.DISPUTE_RAISED,
            -> true

            else -> false
        }
    }

    private fun getExpiryDate(type: TransferNotificationType): Date? {
        val calendar = java.util.Calendar.getInstance()
        return when (type) {
            TransferNotificationType.VERIFICATION_REQUIRED -> {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, VERIFICATION_TIMEOUT_DAYS)
                calendar.time
            }

            TransferNotificationType.HANDOVER_SCHEDULED -> {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 3)
                calendar.time
            }

            else -> null
        }
    }
}
