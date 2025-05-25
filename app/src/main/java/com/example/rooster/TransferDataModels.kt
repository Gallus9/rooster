package com.example.rooster

import com.parse.ParseFile
import com.parse.ParseObject
import java.util.Date

// Transfer verification data models for secure bird ownership transfers

enum class TransferStatus {
    INITIATED,
    PENDING_BUYER_VERIFICATION,
    BUYER_VERIFIED,
    PENDING_HANDOVER,
    HANDOVER_CONFIRMED,
    COMPLETED,
    CANCELLED,
    DISPUTED,
}

enum class VerificationStep {
    BIRD_DETAILS,
    PHOTO_VERIFICATION,
    LOCATION_VERIFICATION,
    FINAL_CONFIRMATION,
}

data class TransferRequest(
    val objectId: String,
    val fowlId: String,
    val sellerId: String,
    val buyerId: String?,
    val status: TransferStatus,
    val initiatedDate: Date,
    val completedDate: Date?,
    val agreedPrice: Double,
    val currency: String,
    val transferLocation: String?,
    val transferLocationLat: Double?,
    val transferLocationLng: Double?,
    val sellerDetails: BirdTransferDetails,
    val buyerVerification: BirdVerificationDetails?,
    val handoverConfirmation: HandoverConfirmation?,
    val fraudPreventionData: Map<String, Any>,
    val notes: String?,
    val isActive: Boolean,
)

data class BirdTransferDetails(
    val birdName: String,
    val birdType: String,
    val age: Int,
    val color: String,
    val gender: String,
    val weight: Double?,
    val height: Double?,
    val healthStatus: String,
    val vaccinationStatus: String,
    val breedingHistory: String?,
    val specialCharacteristics: String?,
    val transferPhotos: List<ParseFile>,
    val veterinaryCertificate: ParseFile?,
    val recordedTimestamp: Date,
    val recordedLocation: String?,
)

data class BirdVerificationDetails(
    val verifiedDate: Date,
    val colorMatch: Boolean,
    val ageMatch: Boolean,
    val genderMatch: Boolean,
    val weightMatch: Boolean,
    val heightMatch: Boolean,
    val healthMatch: Boolean,
    val overallMatch: Boolean,
    val verificationPhotos: List<ParseFile>,
    val buyerNotes: String?,
    val discrepancies: List<String>,
    val verificationScore: Int,
    val verificationLocation: String?,
    val fraudCheckPassed: Boolean,
)

data class HandoverConfirmation(
    val sellerConfirmedDate: Date?,
    val buyerConfirmedDate: Date?,
    val handoverLocation: String,
    val handoverLocationLat: Double,
    val handoverLocationLng: Double,
    val sellerPhotos: List<ParseFile>,
    val buyerPhotos: List<ParseFile>,
    val sellerSignature: String?,
    val buyerSignature: String?,
    val witnessPresent: Boolean,
    val witnessName: String?,
    val witnessContact: String?,
    val paymentConfirmed: Boolean,
    val paymentMethod: String?,
    val finalNotes: String?,
)

data class OwnershipRecord(
    val objectId: String,
    val fowlId: String,
    val previousOwnerId: String,
    val newOwnerId: String,
    val transferRequestId: String,
    val transferDate: Date,
    val transferPrice: Double,
    val transferLocation: String,
    val verificationHash: String,
    val blockchainTxId: String?,
    val isReversible: Boolean,
    val legalDocuments: List<ParseFile>,
)

// Transfer notification types
enum class TransferNotificationType {
    TRANSFER_INITIATED,
    VERIFICATION_REQUIRED,
    VERIFICATION_COMPLETED,
    HANDOVER_SCHEDULED,
    HANDOVER_CONFIRMED,
    TRANSFER_COMPLETED,
    TRANSFER_CANCELLED,
    DISPUTE_RAISED,
}

data class TransferNotification(
    val objectId: String,
    val recipientId: String,
    val senderId: String,
    val transferRequestId: String,
    val notificationType: TransferNotificationType,
    val title: String,
    val message: String,
    val actionRequired: Boolean,
    val isRead: Boolean,
    val createdDate: Date,
    val expiryDate: Date?,
    val metadata: Map<String, Any>,
)

// Parse backend utility functions
object TransferParser {
    fun createTransferRequest(transfer: TransferRequest): ParseObject {
        val parseObject = ParseObject("TransferRequest")
        parseObject.put("fowlId", transfer.fowlId)
        parseObject.put("sellerId", transfer.sellerId)
        transfer.buyerId?.let { parseObject.put("buyerId", it) }
        parseObject.put("status", transfer.status.name)
        parseObject.put("initiatedDate", transfer.initiatedDate)
        transfer.completedDate?.let { parseObject.put("completedDate", it) }
        parseObject.put("agreedPrice", transfer.agreedPrice)
        parseObject.put("currency", transfer.currency)
        transfer.transferLocation?.let { parseObject.put("transferLocation", it) }
        transfer.transferLocationLat?.let { parseObject.put("transferLocationLat", it) }
        transfer.transferLocationLng?.let { parseObject.put("transferLocationLng", it) }
        parseObject.put("sellerDetails", mapSellerDetails(transfer.sellerDetails))
        transfer.buyerVerification?.let {
            parseObject.put("buyerVerification", mapBuyerVerification(it))
        }
        transfer.handoverConfirmation?.let {
            parseObject.put("handoverConfirmation", mapHandoverConfirmation(it))
        }
        parseObject.put("fraudPreventionData", transfer.fraudPreventionData)
        transfer.notes?.let { parseObject.put("notes", it) }
        parseObject.put("isActive", transfer.isActive)
        return parseObject
    }

    fun createOwnershipRecord(ownership: OwnershipRecord): ParseObject {
        val parseObject = ParseObject("OwnershipRecord")
        parseObject.put("fowlId", ownership.fowlId)
        parseObject.put("previousOwnerId", ownership.previousOwnerId)
        parseObject.put("newOwnerId", ownership.newOwnerId)
        parseObject.put("transferRequestId", ownership.transferRequestId)
        parseObject.put("transferDate", ownership.transferDate)
        parseObject.put("transferPrice", ownership.transferPrice)
        parseObject.put("transferLocation", ownership.transferLocation)
        parseObject.put("verificationHash", ownership.verificationHash)
        ownership.blockchainTxId?.let { parseObject.put("blockchainTxId", it) }
        parseObject.put("isReversible", ownership.isReversible)
        parseObject.put("legalDocuments", ownership.legalDocuments)
        return parseObject
    }

    fun createTransferNotification(notification: TransferNotification): ParseObject {
        val parseObject = ParseObject("TransferNotification")
        parseObject.put("recipientId", notification.recipientId)
        parseObject.put("senderId", notification.senderId)
        parseObject.put("transferRequestId", notification.transferRequestId)
        parseObject.put("notificationType", notification.notificationType.name)
        parseObject.put("title", notification.title)
        parseObject.put("message", notification.message)
        parseObject.put("actionRequired", notification.actionRequired)
        parseObject.put("isRead", notification.isRead)
        parseObject.put("createdDate", notification.createdDate)
        notification.expiryDate?.let { parseObject.put("expiryDate", it) }
        parseObject.put("metadata", notification.metadata)
        return parseObject
    }

    private fun mapSellerDetails(details: BirdTransferDetails): Map<String, Any> {
        val baseMap =
            mutableMapOf<String, Any>(
                "birdName" to details.birdName,
                "birdType" to details.birdType,
                "age" to details.age,
                "color" to details.color,
                "gender" to details.gender,
                "weight" to (details.weight ?: 0.0),
                "height" to (details.height ?: 0.0),
                "healthStatus" to details.healthStatus,
                "vaccinationStatus" to details.vaccinationStatus,
                "breedingHistory" to (details.breedingHistory ?: ""),
                "specialCharacteristics" to (details.specialCharacteristics ?: ""),
                "transferPhotos" to details.transferPhotos,
                "recordedTimestamp" to details.recordedTimestamp,
                "recordedLocation" to (details.recordedLocation ?: ""),
            )

        details.veterinaryCertificate?.let { baseMap["veterinaryCertificate"] = it }

        return baseMap
    }

    fun mapBuyerVerification(verification: BirdVerificationDetails): Map<String, Any> {
        return mapOf(
            "verifiedDate" to verification.verifiedDate,
            "colorMatch" to verification.colorMatch,
            "ageMatch" to verification.ageMatch,
            "genderMatch" to verification.genderMatch,
            "weightMatch" to verification.weightMatch,
            "heightMatch" to verification.heightMatch,
            "healthMatch" to verification.healthMatch,
            "overallMatch" to verification.overallMatch,
            "verificationPhotos" to verification.verificationPhotos,
            "buyerNotes" to (verification.buyerNotes ?: ""),
            "discrepancies" to verification.discrepancies,
            "verificationScore" to verification.verificationScore,
            "verificationLocation" to (verification.verificationLocation ?: ""),
            "fraudCheckPassed" to verification.fraudCheckPassed,
        )
    }

    fun mapHandoverConfirmation(handover: HandoverConfirmation): Map<String, Any> {
        val baseMap =
            mutableMapOf<String, Any>(
                "handoverLocation" to handover.handoverLocation,
                "handoverLocationLat" to handover.handoverLocationLat,
                "handoverLocationLng" to handover.handoverLocationLng,
                "sellerPhotos" to handover.sellerPhotos,
                "buyerPhotos" to handover.buyerPhotos,
                "sellerSignature" to (handover.sellerSignature ?: ""),
                "buyerSignature" to (handover.buyerSignature ?: ""),
                "witnessPresent" to handover.witnessPresent,
                "witnessName" to (handover.witnessName ?: ""),
                "witnessContact" to (handover.witnessContact ?: ""),
                "paymentConfirmed" to handover.paymentConfirmed,
                "paymentMethod" to (handover.paymentMethod ?: ""),
                "finalNotes" to (handover.finalNotes ?: ""),
            )

        handover.sellerConfirmedDate?.let { baseMap["sellerConfirmedDate"] = it }
        handover.buyerConfirmedDate?.let { baseMap["buyerConfirmedDate"] = it }

        return baseMap
    }

    fun parseObjectToTransferRequest(parseObject: ParseObject): TransferRequest {
        return TransferRequest(
            objectId = parseObject.objectId,
            fowlId = parseObject.getString("fowlId") ?: "",
            sellerId = parseObject.getString("sellerId") ?: "",
            buyerId = parseObject.getString("buyerId"),
            status =
                TransferStatus.valueOf(
                    parseObject.getString("status") ?: TransferStatus.INITIATED.name,
                ),
            initiatedDate = parseObject.getDate("initiatedDate") ?: Date(),
            completedDate = parseObject.getDate("completedDate"),
            agreedPrice = parseObject.getDouble("agreedPrice"),
            currency = parseObject.getString("currency") ?: "USD",
            transferLocation = parseObject.getString("transferLocation"),
            transferLocationLat = parseObject.getDouble("transferLocationLat").takeIf { it != 0.0 },
            transferLocationLng = parseObject.getDouble("transferLocationLng").takeIf { it != 0.0 },
            sellerDetails = parseSellerDetails(parseObject.get("sellerDetails") as? Map<String, Any>),
            buyerVerification =
                (parseObject.get("buyerVerification") as? Map<String, Any>)?.let {
                    parseBuyerVerification(it)
                },
            handoverConfirmation =
                (parseObject.get("handoverConfirmation") as? Map<String, Any>)?.let {
                    parseHandoverConfirmation(it)
                },
            fraudPreventionData =
                parseObject.get("fraudPreventionData") as? Map<String, Any>
                    ?: emptyMap(),
            notes = parseObject.getString("notes"),
            isActive = parseObject.getBoolean("isActive"),
        )
    }

    private fun parseSellerDetails(map: Map<String, Any>?): BirdTransferDetails {
        return BirdTransferDetails(
            birdName = map?.get("birdName") as? String ?: "",
            birdType = map?.get("birdType") as? String ?: "",
            age = map?.get("age") as? Int ?: 0,
            color = map?.get("color") as? String ?: "",
            gender = map?.get("gender") as? String ?: "",
            weight = map?.get("weight") as? Double,
            height = map?.get("height") as? Double,
            healthStatus = map?.get("healthStatus") as? String ?: "",
            vaccinationStatus = map?.get("vaccinationStatus") as? String ?: "",
            breedingHistory = map?.get("breedingHistory") as? String,
            specialCharacteristics = map?.get("specialCharacteristics") as? String,
            transferPhotos =
                (map?.get("transferPhotos") as? List<*>)?.filterIsInstance<ParseFile>()
                    ?: emptyList(),
            veterinaryCertificate = map?.get("veterinaryCertificate") as? ParseFile,
            recordedTimestamp = map?.get("recordedTimestamp") as? Date ?: Date(),
            recordedLocation = map?.get("recordedLocation") as? String,
        )
    }

    private fun parseBuyerVerification(map: Map<String, Any>): BirdVerificationDetails {
        return BirdVerificationDetails(
            verifiedDate = map["verifiedDate"] as? Date ?: Date(),
            colorMatch = map["colorMatch"] as? Boolean ?: false,
            ageMatch = map["ageMatch"] as? Boolean ?: false,
            genderMatch = map["genderMatch"] as? Boolean ?: false,
            weightMatch = map["weightMatch"] as? Boolean ?: false,
            heightMatch = map["heightMatch"] as? Boolean ?: false,
            healthMatch = map["healthMatch"] as? Boolean ?: false,
            overallMatch = map["overallMatch"] as? Boolean ?: false,
            verificationPhotos =
                (map["verificationPhotos"] as? List<*>)?.filterIsInstance<ParseFile>()
                    ?: emptyList(),
            buyerNotes = map["buyerNotes"] as? String,
            discrepancies =
                (map["discrepancies"] as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList(),
            verificationScore = map["verificationScore"] as? Int ?: 0,
            verificationLocation = map["verificationLocation"] as? String,
            fraudCheckPassed = map["fraudCheckPassed"] as? Boolean ?: false,
        )
    }

    fun parseHandoverConfirmation(map: Map<String, Any>): HandoverConfirmation {
        return HandoverConfirmation(
            sellerConfirmedDate = map["sellerConfirmedDate"] as? Date,
            buyerConfirmedDate = map["buyerConfirmedDate"] as? Date,
            handoverLocation = map["handoverLocation"] as? String ?: "",
            handoverLocationLat = map["handoverLocationLat"] as? Double ?: 0.0,
            handoverLocationLng = map["handoverLocationLng"] as? Double ?: 0.0,
            sellerPhotos =
                (map["sellerPhotos"] as? List<*>)?.filterIsInstance<ParseFile>()
                    ?: emptyList(),
            buyerPhotos =
                (map["buyerPhotos"] as? List<*>)?.filterIsInstance<ParseFile>()
                    ?: emptyList(),
            sellerSignature = map["sellerSignature"] as? String,
            buyerSignature = map["buyerSignature"] as? String,
            witnessPresent = map["witnessPresent"] as? Boolean ?: false,
            witnessName = map["witnessName"] as? String,
            witnessContact = map["witnessContact"] as? String,
            paymentConfirmed = map["paymentConfirmed"] as? Boolean ?: false,
            paymentMethod = map["paymentMethod"] as? String,
            finalNotes = map["finalNotes"] as? String,
        )
    }
}
