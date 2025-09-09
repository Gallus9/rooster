package com.example.rooster

import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseUser
import java.util.Date

// Traditional Market Calendar
data class TraditionalMarket(
    val objectId: String = "",
    val name: String = "",
    val location: String = "",
    val address: String = "",
    val marketType: MarketType = MarketType.WEEKLY,
    val marketDays: List<String> = emptyList(), // Days of week for weekly markets
    val marketDates: List<Date> = emptyList(), // Specific dates for festival markets
    val startTime: String = "",
    val endTime: String = "",
    val specialties: List<String> = emptyList(), // Types of fowl commonly sold
    val contactInfo: String = "",
    val isActive: Boolean = true,
    val culturalSignificance: String = "",
    val region: String = "",
    val language: String = "Telugu",
) {
    companion object {
        fun fromParseObject(parseObject: ParseObject): TraditionalMarket {
            return TraditionalMarket(
                objectId = parseObject.objectId ?: "",
                name = parseObject.getString("name") ?: "",
                location = parseObject.getString("location") ?: "",
                address = parseObject.getString("address") ?: "",
                marketType = MarketType.valueOf(parseObject.getString("marketType") ?: "WEEKLY"),
                marketDays = parseObject.getList<String>("marketDays") ?: emptyList(),
                marketDates = parseObject.getList<Date>("marketDates") ?: emptyList(),
                startTime = parseObject.getString("startTime") ?: "",
                endTime = parseObject.getString("endTime") ?: "",
                specialties = parseObject.getList<String>("specialties") ?: emptyList(),
                contactInfo = parseObject.getString("contactInfo") ?: "",
                isActive = parseObject.getBoolean("isActive"),
                culturalSignificance = parseObject.getString("culturalSignificance") ?: "",
                region = parseObject.getString("region") ?: "",
                language = parseObject.getString("language") ?: "Telugu",
            )
        }
    }

    fun toParseObject(): ParseObject {
        val parseObject = ParseObject("TraditionalMarket")
        parseObject.put("name", name)
        parseObject.put("location", location)
        parseObject.put("address", address)
        parseObject.put("marketType", marketType.name)
        parseObject.put("marketDays", marketDays)
        parseObject.put("marketDates", marketDates)
        parseObject.put("startTime", startTime)
        parseObject.put("endTime", endTime)
        parseObject.put("specialties", specialties)
        parseObject.put("contactInfo", contactInfo)
        parseObject.put("isActive", isActive)
        parseObject.put("culturalSignificance", culturalSignificance)
        parseObject.put("region", region)
        parseObject.put("language", language)
        return parseObject
    }
}

// Pre-Market Order System
data class PreMarketOrder(
    val objectId: String = "",
    val seller: ParseUser? = null,
    val marketId: String = "",
    val marketDate: Date = Date(),
    val fowlType: String = "",
    val breed: String = "",
    val quantity: Int = 0,
    val pricePerBird: Double = 0.0,
    val totalPrice: Double = 0.0,
    val description: String = "",
    val images: List<ParseFile> = emptyList(),
    val reservationDeadline: Date = Date(),
    val status: PreOrderStatus = PreOrderStatus.OPEN,
    val reservedQuantity: Int = 0,
    val minOrderQuantity: Int = 1,
    val specialInstructions: String = "",
    val deliveryMethod: DeliveryMethod = DeliveryMethod.PICKUP_AT_MARKET,
    val culturalContext: String = "", // Festival or occasion context
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) {
    companion object {
        fun fromParseObject(parseObject: ParseObject): PreMarketOrder {
            return PreMarketOrder(
                objectId = parseObject.objectId ?: "",
                seller = parseObject.getParseUser("seller"),
                marketId = parseObject.getString("marketId") ?: "",
                marketDate = parseObject.getDate("marketDate") ?: Date(),
                fowlType = parseObject.getString("fowlType") ?: "",
                breed = parseObject.getString("breed") ?: "",
                quantity = parseObject.getInt("quantity"),
                pricePerBird = parseObject.getDouble("pricePerBird"),
                totalPrice = parseObject.getDouble("totalPrice"),
                description = parseObject.getString("description") ?: "",
                images = parseObject.getList<ParseFile>("images") ?: emptyList(),
                reservationDeadline = parseObject.getDate("reservationDeadline") ?: Date(),
                status = PreOrderStatus.valueOf(parseObject.getString("status") ?: "OPEN"),
                reservedQuantity = parseObject.getInt("reservedQuantity"),
                minOrderQuantity = parseObject.getInt("minOrderQuantity"),
                specialInstructions = parseObject.getString("specialInstructions") ?: "",
                deliveryMethod =
                    DeliveryMethod.valueOf(
                        parseObject.getString("deliveryMethod") ?: "PICKUP_AT_MARKET",
                    ),
                culturalContext = parseObject.getString("culturalContext") ?: "",
                createdAt = parseObject.createdAt ?: Date(),
                updatedAt = parseObject.updatedAt ?: Date(),
            )
        }
    }
}

// Group Buying Coordination
data class GroupBuyingRequest(
    val objectId: String = "",
    val organizer: ParseUser? = null,
    val title: String = "",
    val description: String = "",
    val fowlType: String = "",
    val breed: String = "",
    val targetQuantity: Int = 0,
    val maxPricePerBird: Double = 0.0,
    val currentParticipants: Int = 0,
    val minParticipants: Int = 0,
    val maxParticipants: Int = 0,
    val deadline: Date = Date(),
    val marketId: String = "",
    val marketDate: Date = Date(),
    val status: GroupBuyStatus = GroupBuyStatus.ORGANIZING,
    val participants: List<ParseUser> = emptyList(),
    val participantQuantities: Map<String, Int> = emptyMap(),
    val totalCommittedQuantity: Int = 0,
    val isPublic: Boolean = true,
    val culturalPurpose: String = "", // Festival, competition, etc.
    val region: String = "",
    val createdAt: Date = Date(),
) {
    companion object {
        fun fromParseObject(parseObject: ParseObject): GroupBuyingRequest {
            return GroupBuyingRequest(
                objectId = parseObject.objectId ?: "",
                organizer = parseObject.getParseUser("organizer"),
                title = parseObject.getString("title") ?: "",
                description = parseObject.getString("description") ?: "",
                fowlType = parseObject.getString("fowlType") ?: "",
                breed = parseObject.getString("breed") ?: "",
                targetQuantity = parseObject.getInt("targetQuantity"),
                maxPricePerBird = parseObject.getDouble("maxPricePerBird"),
                currentParticipants = parseObject.getInt("currentParticipants"),
                minParticipants = parseObject.getInt("minParticipants"),
                maxParticipants = parseObject.getInt("maxParticipants"),
                deadline = parseObject.getDate("deadline") ?: Date(),
                marketId = parseObject.getString("marketId") ?: "",
                marketDate = parseObject.getDate("marketDate") ?: Date(),
                status = GroupBuyStatus.valueOf(parseObject.getString("status") ?: "ORGANIZING"),
                participants = parseObject.getList<ParseUser>("participants") ?: emptyList(),
                participantQuantities = emptyMap(), // Will be handled separately in actual implementation
                totalCommittedQuantity = parseObject.getInt("totalCommittedQuantity"),
                isPublic = parseObject.getBoolean("isPublic"),
                culturalPurpose = parseObject.getString("culturalPurpose") ?: "",
                region = parseObject.getString("region") ?: "",
                createdAt = parseObject.createdAt ?: Date(),
            )
        }
    }
}

// Market Analytics and Trends
data class MarketTrend(
    val objectId: String = "",
    val marketId: String = "",
    val marketDate: Date = Date(),
    val fowlType: String = "",
    val breed: String = "",
    val averagePrice: Double = 0.0,
    val highestPrice: Double = 0.0,
    val lowestPrice: Double = 0.0,
    val totalSold: Int = 0,
    val demandLevel: DemandLevel = DemandLevel.MODERATE,
    val priceVariance: Double = 0.0,
    val popularBreeds: List<String> = emptyList(),
    val seasonalFactors: List<String> = emptyList(),
    val festivalImpact: String = "",
    val weatherImpact: String = "",
    val transportCosts: Double = 0.0,
    val marketEfficiency: Double = 0.0,
    val supplierCount: Int = 0,
    val buyerCount: Int = 0,
    val region: String = "",
) {
    companion object {
        fun fromParseObject(parseObject: ParseObject): MarketTrend {
            return MarketTrend(
                objectId = parseObject.objectId ?: "",
                marketId = parseObject.getString("marketId") ?: "",
                marketDate = parseObject.getDate("marketDate") ?: Date(),
                fowlType = parseObject.getString("fowlType") ?: "",
                breed = parseObject.getString("breed") ?: "",
                averagePrice = parseObject.getDouble("averagePrice"),
                highestPrice = parseObject.getDouble("highestPrice"),
                lowestPrice = parseObject.getDouble("lowestPrice"),
                totalSold = parseObject.getInt("totalSold"),
                demandLevel =
                    DemandLevel.valueOf(
                        parseObject.getString("demandLevel") ?: "MODERATE",
                    ),
                priceVariance = parseObject.getDouble("priceVariance"),
                popularBreeds = parseObject.getList<String>("popularBreeds") ?: emptyList(),
                seasonalFactors = parseObject.getList<String>("seasonalFactors") ?: emptyList(),
                festivalImpact = parseObject.getString("festivalImpact") ?: "",
                weatherImpact = parseObject.getString("weatherImpact") ?: "",
                transportCosts = parseObject.getDouble("transportCosts"),
                marketEfficiency = parseObject.getDouble("marketEfficiency"),
                supplierCount = parseObject.getInt("supplierCount"),
                buyerCount = parseObject.getInt("buyerCount"),
                region = parseObject.getString("region") ?: "",
            )
        }
    }
}

// Digital Marketplace Backup for Cancelled Markets
data class DigitalMarketEvent(
    val objectId: String = "",
    val originalMarketId: String = "",
    val originalMarketDate: Date = Date(),
    val cancellationReason: String = "",
    val digitalEventDate: Date = Date(),
    val eventDuration: Int = 24, // Hours
    val participatingFarmers: List<ParseUser> = emptyList(),
    val status: DigitalEventStatus = DigitalEventStatus.SCHEDULED,
    val totalListings: Int = 0,
    val totalTransactions: Int = 0,
    val specialPromotions: List<String> = emptyList(),
    val deliveryOptions: List<DeliveryMethod> = emptyList(),
    val culturalNotes: String = "",
    val isEmergencyEvent: Boolean = false,
    val region: String = "",
) {
    companion object {
        fun fromParseObject(parseObject: ParseObject): DigitalMarketEvent {
            return DigitalMarketEvent(
                objectId = parseObject.objectId ?: "",
                originalMarketId = parseObject.getString("originalMarketId") ?: "",
                originalMarketDate = parseObject.getDate("originalMarketDate") ?: Date(),
                cancellationReason = parseObject.getString("cancellationReason") ?: "",
                digitalEventDate = parseObject.getDate("digitalEventDate") ?: Date(),
                eventDuration = parseObject.getInt("eventDuration"),
                participatingFarmers =
                    parseObject.getList<ParseUser>("participatingFarmers")
                        ?: emptyList(),
                status = DigitalEventStatus.valueOf(parseObject.getString("status") ?: "SCHEDULED"),
                totalListings = parseObject.getInt("totalListings"),
                totalTransactions = parseObject.getInt("totalTransactions"),
                specialPromotions = parseObject.getList<String>("specialPromotions") ?: emptyList(),
                deliveryOptions =
                    DeliveryMethod.values().filter {
                        parseObject.getList<String>("deliveryOptions")?.contains(it.name) == true
                    },
                culturalNotes = parseObject.getString("culturalNotes") ?: "",
                isEmergencyEvent = parseObject.getBoolean("isEmergencyEvent"),
                region = parseObject.getString("region") ?: "",
            )
        }
    }
}

// Enums for different statuses and types
enum class MarketType {
    DAILY,
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    FESTIVAL_SPECIAL,
    SEASONAL,
}

enum class PreOrderStatus {
    OPEN,
    PARTIALLY_RESERVED,
    FULLY_RESERVED,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
}

enum class GroupBuyStatus {
    ORGANIZING,
    ACTIVE,
    MINIMUM_REACHED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class DeliveryMethod {
    PICKUP_AT_MARKET,
    HOME_DELIVERY,
    CENTRAL_PICKUP_POINT,
    FARMER_DELIVERY,
}

enum class DemandLevel {
    VERY_LOW,
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
    EXCEPTIONAL,
}

enum class DigitalEventStatus {
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    POSTPONED,
}

// Market Calendar Entry for easy display
data class MarketCalendarEntry(
    val marketId: String = "",
    val marketName: String = "",
    val date: Date = Date(),
    val dayOfWeek: String = "",
    val location: String = "",
    val marketType: MarketType = MarketType.WEEKLY,
    val specialties: List<String> = emptyList(),
    val expectedCrowds: DemandLevel = DemandLevel.MODERATE,
    val weatherForecast: String = "",
    val culturalEvents: List<String> = emptyList(),
    val transportRoutes: List<String> = emptyList(),
    val isHoliday: Boolean = false,
    val holidayName: String = "",
)

// Price Prediction Model
data class PricePrediction(
    val fowlType: String = "",
    val breed: String = "",
    val marketId: String = "",
    val targetDate: Date = Date(),
    val predictedPrice: Double = 0.0,
    val confidence: Double = 0.0,
    val priceRange: Pair<Double, Double> = Pair(0.0, 0.0),
    val influencingFactors: List<String> = emptyList(),
    val seasonalAdjustment: Double = 0.0,
    val festivalAdjustment: Double = 0.0,
    val demandForecast: DemandLevel = DemandLevel.MODERATE,
    val recommendedAction: String = "",
)
