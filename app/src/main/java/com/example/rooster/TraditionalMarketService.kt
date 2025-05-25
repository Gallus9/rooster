package com.example.rooster

import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TraditionalMarketService {
    // Regional Market Calendar Functions
    fun fetchTraditionalMarkets(
        region: String = "",
        onResult: (List<TraditionalMarket>) -> Unit,
        onError: (String?) -> Unit,
        setLoading: (Boolean) -> Unit,
    ) {
        setLoading(true)
        try {
            val query = ParseQuery.getQuery<ParseObject>("TraditionalMarket")
            query.whereEqualTo("isActive", true)
            if (region.isNotEmpty()) {
                query.whereEqualTo("region", region)
            }
            query.orderByAscending("name")
            query.findInBackground { objects, e ->
                setLoading(false)
                if (e != null) {
                    onError(e.localizedMessage)
                } else {
                    val markets =
                        objects?.map { TraditionalMarket.fromParseObject(it) } ?: emptyList()
                    onResult(markets)
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            onError(e.localizedMessage)
        }
    }

    fun fetchMarketCalendar(
        marketId: String = "",
        startDate: Date,
        endDate: Date,
        onResult: (List<MarketCalendarEntry>) -> Unit,
        onError: (String?) -> Unit,
        setLoading: (Boolean) -> Unit,
    ) {
        setLoading(true)
        try {
            val query = ParseQuery.getQuery<ParseObject>("TraditionalMarket")
            query.whereEqualTo("isActive", true)
            if (marketId.isNotEmpty()) {
                query.whereEqualTo("objectId", marketId)
            }

            query.findInBackground { markets, e ->
                if (e != null) {
                    setLoading(false)
                    onError(e.localizedMessage)
                    return@findInBackground
                }

                val calendarEntries = mutableListOf<MarketCalendarEntry>()
                val calendar = Calendar.getInstance()
                val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

                markets?.forEach { marketObj ->
                    val market = TraditionalMarket.fromParseObject(marketObj)

                    // Generate calendar entries based on market type and schedule
                    when (market.marketType) {
                        MarketType.WEEKLY -> {
                            // Generate weekly entries
                            calendar.time = startDate
                            while (calendar.time.before(endDate) || calendar.time == endDate) {
                                val dayOfWeek = dayFormatter.format(calendar.time)
                                if (market.marketDays.contains(dayOfWeek)) {
                                    calendarEntries.add(
                                        MarketCalendarEntry(
                                            marketId = market.objectId,
                                            marketName = market.name,
                                            date = Date(calendar.timeInMillis),
                                            dayOfWeek = dayOfWeek,
                                            location = market.location,
                                            marketType = market.marketType,
                                            specialties = market.specialties,
                                        ),
                                    )
                                }
                                calendar.add(Calendar.DAY_OF_MONTH, 1)
                            }
                        }

                        MarketType.FESTIVAL_SPECIAL -> {
                            // Add specific festival dates
                            market.marketDates.forEach { festivalDate ->
                                if (!festivalDate.before(startDate) && !festivalDate.after(endDate)) {
                                    calendarEntries.add(
                                        MarketCalendarEntry(
                                            marketId = market.objectId,
                                            marketName = market.name,
                                            date = festivalDate,
                                            dayOfWeek = dayFormatter.format(festivalDate),
                                            location = market.location,
                                            marketType = market.marketType,
                                            specialties = market.specialties,
                                            culturalEvents = listOf(market.culturalSignificance),
                                        ),
                                    )
                                }
                            }
                        }

                        else -> {
                            // Handle other market types (daily, monthly, etc.)
                            calendar.time = startDate
                            val interval =
                                when (market.marketType) {
                                    MarketType.DAILY -> 1
                                    MarketType.BI_WEEKLY -> 14
                                    MarketType.MONTHLY -> 30
                                    else -> 7
                                }

                            while (calendar.time.before(endDate) || calendar.time == endDate) {
                                calendarEntries.add(
                                    MarketCalendarEntry(
                                        marketId = market.objectId,
                                        marketName = market.name,
                                        date = Date(calendar.timeInMillis),
                                        dayOfWeek = dayFormatter.format(calendar.time),
                                        location = market.location,
                                        marketType = market.marketType,
                                        specialties = market.specialties,
                                    ),
                                )
                                calendar.add(Calendar.DAY_OF_MONTH, interval)
                            }
                        }
                    }
                }

                setLoading(false)
                onResult(calendarEntries.sortedBy { it.date })
            }
        } catch (e: Exception) {
            setLoading(false)
            onError(e.localizedMessage)
        }
    }

    // Pre-Market Order System Functions
    fun createPreMarketOrder(
        order: PreMarketOrder,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit,
    ) {
        try {
            val parseObject = ParseObject("PreMarketOrder")
            parseObject.put("seller", ParseUser.getCurrentUser())
            parseObject.put("marketId", order.marketId)
            parseObject.put("marketDate", order.marketDate)
            parseObject.put("fowlType", order.fowlType)
            parseObject.put("breed", order.breed)
            parseObject.put("quantity", order.quantity)
            parseObject.put("pricePerBird", order.pricePerBird)
            parseObject.put("totalPrice", order.totalPrice)
            parseObject.put("description", order.description)
            parseObject.put("reservationDeadline", order.reservationDeadline)
            parseObject.put("status", order.status.name)
            parseObject.put("minOrderQuantity", order.minOrderQuantity)
            parseObject.put("specialInstructions", order.specialInstructions)
            parseObject.put("deliveryMethod", order.deliveryMethod.name)
            parseObject.put("culturalContext", order.culturalContext)

            parseObject.saveInBackground { e ->
                if (e == null) {
                    onSuccess(parseObject.objectId)
                } else {
                    onError(
                        (e as? ParseException)?.localizedMessage
                            ?: "Failed to create pre-market order",
                    )
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage)
        }
    }

    fun fetchPreMarketOrders(
        marketId: String = "",
        onResult: (List<PreMarketOrder>) -> Unit,
        onError: (String?) -> Unit,
        setLoading: (Boolean) -> Unit,
    ) {
        setLoading(true)
        try {
            val query = ParseQuery.getQuery<ParseObject>("PreMarketOrder")
            if (marketId.isNotEmpty()) {
                query.whereEqualTo("marketId", marketId)
            }
            query.whereEqualTo("status", PreOrderStatus.OPEN.name)
            query.orderByDescending("createdAt")
            query.findInBackground { objects, e ->
                setLoading(false)
                if (e != null) {
                    onError(e.localizedMessage)
                } else {
                    val orders = objects?.map { PreMarketOrder.fromParseObject(it) } ?: emptyList()
                    onResult(orders)
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            onError(e.localizedMessage)
        }
    }

    // Group Buying Coordination Functions
    fun createGroupBuyingRequest(
        request: GroupBuyingRequest,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit,
    ) {
        try {
            val parseObject = ParseObject("GroupBuyingRequest")
            parseObject.put("organizer", ParseUser.getCurrentUser())
            parseObject.put("title", request.title)
            parseObject.put("description", request.description)
            parseObject.put("fowlType", request.fowlType)
            parseObject.put("breed", request.breed)
            parseObject.put("targetQuantity", request.targetQuantity)
            parseObject.put("maxPricePerBird", request.maxPricePerBird)
            parseObject.put("minParticipants", request.minParticipants)
            parseObject.put("maxParticipants", request.maxParticipants)
            parseObject.put("deadline", request.deadline)
            parseObject.put("marketId", request.marketId)
            parseObject.put("marketDate", request.marketDate)
            parseObject.put("status", request.status.name)
            parseObject.put("isPublic", request.isPublic)
            parseObject.put("culturalPurpose", request.culturalPurpose)
            parseObject.put("region", request.region)

            parseObject.saveInBackground { e ->
                if (e == null) {
                    onSuccess(parseObject.objectId)
                } else {
                    onError(
                        (e as? ParseException)?.localizedMessage
                            ?: "Failed to create group buying request",
                    )
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage)
        }
    }

    fun fetchGroupBuyingRequests(
        region: String = "",
        onResult: (List<GroupBuyingRequest>) -> Unit,
        onError: (String?) -> Unit,
        setLoading: (Boolean) -> Unit,
    ) {
        setLoading(true)
        try {
            val query = ParseQuery.getQuery<ParseObject>("GroupBuyingRequest")
            query.whereEqualTo("isPublic", true)
            query.whereEqualTo("status", GroupBuyStatus.ORGANIZING.name)
            if (region.isNotEmpty()) {
                query.whereEqualTo("region", region)
            }
            query.orderByDescending("createdAt")
            query.findInBackground { objects, e ->
                setLoading(false)
                if (e != null) {
                    onError(e.localizedMessage)
                } else {
                    val requests =
                        objects?.map { GroupBuyingRequest.fromParseObject(it) } ?: emptyList()
                    onResult(requests)
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            onError(e.localizedMessage)
        }
    }

    // Market Trend Analysis Functions
    fun fetchMarketTrends(
        marketId: String,
        fowlType: String = "",
        breed: String = "",
        days: Int = 30,
        onResult: (List<MarketTrend>) -> Unit,
        onError: (String?) -> Unit,
        setLoading: (Boolean) -> Unit,
    ) {
        setLoading(true)
        try {
            val query = ParseQuery.getQuery<ParseObject>("MarketTrend")
            query.whereEqualTo("marketId", marketId)
            if (fowlType.isNotEmpty()) {
                query.whereEqualTo("fowlType", fowlType)
            }
            if (breed.isNotEmpty()) {
                query.whereEqualTo("breed", breed)
            }

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -days)
            query.whereGreaterThanOrEqualTo("marketDate", calendar.time)
            query.orderByDescending("marketDate")

            query.findInBackground { objects, e ->
                setLoading(false)
                if (e != null) {
                    onError(e.localizedMessage)
                } else {
                    val trends = objects?.map { MarketTrend.fromParseObject(it) } ?: emptyList()
                    onResult(trends)
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            onError(e.localizedMessage)
        }
    }

    fun generatePricePrediction(
        marketId: String,
        fowlType: String,
        breed: String,
        targetDate: Date,
        onResult: (PricePrediction) -> Unit,
        onError: (String?) -> Unit,
    ) {
        // Fetch historical data for price prediction
        fetchMarketTrends(
            marketId = marketId,
            fowlType = fowlType,
            breed = breed,
            days = 90,
            onResult = { trends ->
                if (trends.isNotEmpty()) {
                    val averagePrice = trends.map { it.averagePrice }.average()
                    val priceVariance = trends.map { it.priceVariance }.average()
                    val seasonal = calculateSeasonalAdjustment(targetDate)
                    val festival = calculateFestivalAdjustment(targetDate)

                    val predictedPrice = averagePrice * (1 + seasonal + festival)
                    val confidence = calculateConfidence(trends.size, priceVariance)
                    val range =
                        Pair(
                            predictedPrice * (1 - priceVariance / 100),
                            predictedPrice * (1 + priceVariance / 100),
                        )

                    val prediction =
                        PricePrediction(
                            fowlType = fowlType,
                            breed = breed,
                            marketId = marketId,
                            targetDate = targetDate,
                            predictedPrice = predictedPrice,
                            confidence = confidence,
                            priceRange = range,
                            influencingFactors = getInfluencingFactors(trends),
                            seasonalAdjustment = seasonal,
                            festivalAdjustment = festival,
                            demandForecast = predictDemand(trends, targetDate),
                            recommendedAction = getRecommendedAction(predictedPrice, averagePrice),
                        )

                    onResult(prediction)
                } else {
                    onError("Insufficient historical data for price prediction")
                }
            },
            onError = onError,
            setLoading = { },
        )
    }

    // Digital Marketplace Backup Functions
    fun createDigitalMarketEvent(
        originalMarketId: String,
        originalMarketDate: Date,
        cancellationReason: String,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit,
    ) {
        try {
            val parseObject = ParseObject("DigitalMarketEvent")
            parseObject.put("originalMarketId", originalMarketId)
            parseObject.put("originalMarketDate", originalMarketDate)
            parseObject.put("cancellationReason", cancellationReason)

            val calendar = Calendar.getInstance()
            calendar.time = originalMarketDate
            calendar.add(Calendar.HOUR_OF_DAY, 2) // Start digital event 2 hours after original time
            parseObject.put("digitalEventDate", calendar.time)

            parseObject.put("eventDuration", 24)
            parseObject.put("status", DigitalEventStatus.SCHEDULED.name)
            parseObject.put("isEmergencyEvent", true)

            parseObject.saveInBackground { e ->
                if (e == null) {
                    onSuccess(parseObject.objectId)
                } else {
                    onError(
                        (e as? ParseException)?.localizedMessage
                            ?: "Failed to create digital market event",
                    )
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage)
        }
    }

    fun fetchActiveDigitalMarketEvents(
        onResult: (List<DigitalMarketEvent>) -> Unit,
        onError: (String?) -> Unit,
        setLoading: (Boolean) -> Unit,
    ) {
        setLoading(true)
        try {
            val query = ParseQuery.getQuery<ParseObject>("DigitalMarketEvent")
            query.whereEqualTo("status", DigitalEventStatus.ACTIVE.name)
            query.orderByDescending("digitalEventDate")
            query.findInBackground { objects, e ->
                setLoading(false)
                if (e != null) {
                    onError(e.localizedMessage)
                } else {
                    val events =
                        objects?.map { DigitalMarketEvent.fromParseObject(it) } ?: emptyList()
                    onResult(events)
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            onError(e.localizedMessage)
        }
    }

    // Helper functions for price prediction
    private fun calculateSeasonalAdjustment(date: Date): Double {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return when (calendar.get(Calendar.MONTH)) {
            Calendar.JANUARY -> 0.15 // Festival season
            Calendar.APRIL -> 0.10 // Summer season
            Calendar.OCTOBER, Calendar.NOVEMBER -> 0.20 // Festival season
            else -> 0.0
        }
    }

    private fun calculateFestivalAdjustment(date: Date): Double {
        // Check if date is near major festivals
        val calendar = Calendar.getInstance()
        calendar.time = date
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return when {
            month == Calendar.JANUARY && day in 10..20 -> 0.25 // Sankranti
            month == Calendar.MARCH && day in 15..25 -> 0.15 // Holi
            month == Calendar.OCTOBER && day in 15..25 -> 0.20 // Dussehra
            month == Calendar.NOVEMBER && day in 1..15 -> 0.20 // Diwali
            else -> 0.0
        }
    }

    private fun calculateConfidence(
        dataPoints: Int,
        variance: Double,
    ): Double {
        val baseConfidence = minOf(dataPoints / 30.0, 1.0) // More data points = higher confidence
        val varianceAdjustment =
            maxOf(0.0, 1.0 - variance / 50.0) // Lower variance = higher confidence
        return (baseConfidence * 0.7 + varianceAdjustment * 0.3) * 100
    }

    private fun getInfluencingFactors(trends: List<MarketTrend>): List<String> {
        val factors = mutableListOf<String>()
        val recent = trends.take(5)

        if (recent.any { it.festivalImpact.isNotEmpty() }) {
            factors.add("Festival Season")
        }
        if (recent.any { it.weatherImpact.contains("rain", ignoreCase = true) }) {
            factors.add("Weather Conditions")
        }
        if (recent.map { it.demandLevel }
                .any { it == DemandLevel.HIGH || it == DemandLevel.VERY_HIGH }
        ) {
            factors.add("High Demand")
        }
        if (recent.map { it.supplierCount }.average() < 10) {
            factors.add("Limited Supply")
        }

        return factors
    }

    private fun predictDemand(
        trends: List<MarketTrend>,
        targetDate: Date,
    ): DemandLevel {
        val recentDemand = trends.take(5).map { it.demandLevel }
        val averageDemand =
            recentDemand.map {
                when (it) {
                    DemandLevel.VERY_LOW -> 1
                    DemandLevel.LOW -> 2
                    DemandLevel.MODERATE -> 3
                    DemandLevel.HIGH -> 4
                    DemandLevel.VERY_HIGH -> 5
                    DemandLevel.EXCEPTIONAL -> 6
                }
            }.average()

        val festivalAdjustment = if (calculateFestivalAdjustment(targetDate) > 0.1) 1 else 0
        val finalDemand = (averageDemand + festivalAdjustment).toInt()

        return when (finalDemand) {
            1 -> DemandLevel.VERY_LOW
            2 -> DemandLevel.LOW
            3 -> DemandLevel.MODERATE
            4 -> DemandLevel.HIGH
            5 -> DemandLevel.VERY_HIGH
            else -> DemandLevel.EXCEPTIONAL
        }
    }

    private fun getRecommendedAction(
        predictedPrice: Double,
        historicalAverage: Double,
    ): String {
        val priceRatio = predictedPrice / historicalAverage
        return when {
            priceRatio > 1.2 -> "Excellent selling opportunity - Prices above average"
            priceRatio > 1.1 -> "Good time to sell - Prices slightly above average"
            priceRatio < 0.9 -> "Consider waiting - Prices below average"
            priceRatio < 0.8 -> "Hold if possible - Prices significantly below average"
            else -> "Moderate pricing - Standard market conditions"
        }
    }
}
