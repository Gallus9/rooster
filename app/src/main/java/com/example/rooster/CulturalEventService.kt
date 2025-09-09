package com.example.rooster

import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class CulturalEventService {
    // Festival Management
    suspend fun getFestivals(language: String = "en"): Result<List<Festival>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("Festival")
                query.whereEqualTo("isActive", true)
                query.orderByAscending("date")

                val results = query.find()
                val festivals =
                    results.mapNotNull { parseObject ->
                        parseFestivalFromParseObject(parseObject, language)
                    }
                Result.success(festivals)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getUpcomingFestivals(language: String = "en"): Result<List<Festival>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("Festival")
                query.whereEqualTo("isActive", true)
                query.whereGreaterThanOrEqualTo("date", Date())
                query.orderByAscending("date")

                val results = query.find()
                val festivals =
                    results.mapNotNull { parseObject ->
                        parseFestivalFromParseObject(parseObject, language)
                    }
                Result.success(festivals)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getFestivalsByMonth(
        month: Int,
        year: Int,
        language: String = "en",
    ): Result<List<Festival>> =
        withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, 1, 0, 0, 0)
                val startDate = calendar.time

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endDate = calendar.time

                val query = ParseQuery.getQuery<ParseObject>("Festival")
                query.whereEqualTo("isActive", true)
                query.whereGreaterThanOrEqualTo("date", startDate)
                query.whereLessThanOrEqualTo("date", endDate)
                query.orderByAscending("date")

                val results = query.find()
                val festivals =
                    results.mapNotNull { parseObject ->
                        parseFestivalFromParseObject(parseObject, language)
                    }
                Result.success(festivals)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Group Ordering System
    suspend fun getActiveGroupOrders(festivalId: String? = null): Result<List<GroupOrder>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("GroupOrder")
                query.whereEqualTo("status", GroupOrderStatus.OPEN.name)
                query.whereGreaterThan("deadline", Date())

                if (festivalId != null) {
                    query.whereEqualTo("festivalId", festivalId)
                }

                query.orderByAscending("deadline")

                val results = query.find()
                val groupOrders =
                    results.mapNotNull { parseObject ->
                        parseGroupOrderFromParseObject(parseObject)
                    }
                Result.success(groupOrders)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun createGroupOrder(groupOrder: GroupOrder): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = groupOrder.toParseObject()
                parseObject.save()
                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun joinGroupOrder(
        orderId: String,
        quantity: Int,
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val query = ParseQuery.getQuery<ParseObject>("GroupOrder")
                val groupOrder = query.get(orderId)

                val currentQuantity = groupOrder.getInt("currentQuantity")
                val targetQuantity = groupOrder.getInt("targetQuantity")
                val unitPrice = groupOrder.getDouble("unitPrice")

                if (currentQuantity + quantity > targetQuantity) {
                    return@withContext Result.failure(Exception("Quantity exceeds target"))
                }

                // Create participant entry
                val participant = ParseObject("GroupOrderParticipant")
                participant.put("groupOrderId", orderId)
                participant.put("userId", currentUser.objectId)
                participant.put("userName", currentUser.username ?: "")
                participant.put("quantity", quantity)
                participant.put("totalAmount", quantity * unitPrice)
                participant.put("joinedAt", Date())
                participant.put("paymentStatus", PaymentStatus.PENDING.name)
                participant.save()

                // Update group order
                groupOrder.put("currentQuantity", currentQuantity + quantity)
                if (currentQuantity + quantity >= targetQuantity) {
                    groupOrder.put("status", GroupOrderStatus.TARGET_REACHED.name)
                }
                groupOrder.save()

                Result.success(true)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Competition Management
    suspend fun getFestivalCompetitions(
        festivalId: String,
        language: String = "en",
    ): Result<List<FestivalCompetition>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("FestivalCompetition")
                query.whereEqualTo("festivalId", festivalId)
                query.orderByAscending("startDate")

                val results = query.find()
                val competitions =
                    results.mapNotNull { parseObject ->
                        parseCompetitionFromParseObject(parseObject, language)
                    }
                Result.success(competitions)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun registerForCompetition(
        competitionId: String,
        fowlId: String,
        culturalPresentation: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                // Check if already registered
                val existingQuery = ParseQuery.getQuery<ParseObject>("CompetitionEntry")
                existingQuery.whereEqualTo("competitionId", competitionId)
                existingQuery.whereEqualTo("participantId", currentUser.objectId)

                if (existingQuery.find().isNotEmpty()) {
                    return@withContext Result.failure(Exception("Already registered for this competition"))
                }

                // Get fowl details
                val fowlQuery = ParseQuery.getQuery<ParseObject>("Fowl")
                val fowl = fowlQuery.get(fowlId)

                val entry = ParseObject("CompetitionEntry")
                entry.put("competitionId", competitionId)
                entry.put("participantId", currentUser.objectId)
                entry.put("participantName", currentUser.username ?: "")
                entry.put("fowlId", fowlId)
                entry.put("roosterName", fowl.getString("name") ?: "")
                entry.put("registrationDate", Date())
                entry.put("status", EntryStatus.REGISTERED.name)
                entry.put("culturalPresentation", culturalPresentation)
                entry.save()

                // Update competition participant count
                val competitionQuery = ParseQuery.getQuery<ParseObject>("FestivalCompetition")
                val competition = competitionQuery.get(competitionId)
                competition.increment("currentParticipants")
                competition.save()

                Result.success(entry.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getCompetitionLeaderboard(competitionId: String): Result<FestivalLeaderboard> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("CompetitionEntry")
                query.whereEqualTo("competitionId", competitionId)
                query.whereEqualTo("status", EntryStatus.COMPLETED.name)
                query.orderByDescending("finalScore")

                val results = query.find()
                val entries =
                    results.mapIndexed { index, parseObject ->
                        LeaderboardEntry(
                            rank = index + 1,
                            participantId = parseObject.getString("participantId") ?: "",
                            participantName = parseObject.getString("participantName") ?: "",
                            fowlId = parseObject.getString("fowlId") ?: "",
                            roosterName = parseObject.getString("roosterName") ?: "",
                            score = parseObject.getDouble("finalScore"),
                            profileImageUrl = parseObject.getString("profileImageUrl") ?: "",
                            roosterImageUrl = parseObject.getString("roosterImageUrl") ?: "",
                        )
                    }

                val leaderboard =
                    FestivalLeaderboard(
                        competitionId = competitionId,
                        entries = entries,
                        lastUpdated = Date(),
                    )

                Result.success(leaderboard)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Rooster Showcase Management
    suspend fun createRoosterShowcase(showcase: RoosterShowcase): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = showcase.toParseObject()
                parseObject.save()
                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getFestivalShowcases(
        festivalId: String,
        showcaseType: ShowcaseType? = null,
    ): Result<List<RoosterShowcase>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("RoosterShowcase")
                query.whereEqualTo("festivalId", festivalId)

                showcaseType?.let {
                    query.whereEqualTo("showcaseType", it.name)
                }

                query.orderByDescending("likes")
                query.orderByDescending("createdAt")

                val results = query.find()
                val showcases =
                    results.mapNotNull { parseObject ->
                        parseShowcaseFromParseObject(parseObject)
                    }
                Result.success(showcases)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun likeShowcase(showcaseId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("RoosterShowcase")
                val showcase = query.get(showcaseId)
                showcase.increment("likes")
                showcase.save()
                Result.success(true)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun addShowcaseComment(
        showcaseId: String,
        comment: String,
        culturalAppreciation: String = "",
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val commentObject = ParseObject("ShowcaseComment")
                commentObject.put("showcaseId", showcaseId)
                commentObject.put("userId", currentUser.objectId)
                commentObject.put("userName", currentUser.username ?: "")
                commentObject.put("comment", comment)
                commentObject.put("culturalAppreciation", culturalAppreciation)
                commentObject.put("timestamp", Date())
                commentObject.put("likes", 0)
                commentObject.save()

                Result.success(commentObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Cultural Content Management
    suspend fun uploadCulturalContent(content: MultimediaContent): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = ParseObject("MultimediaContent")
                parseObject.put("type", content.type.name)
                parseObject.put("url", content.url)
                parseObject.put("title", content.title)
                parseObject.put("description", content.description)
                parseObject.put("culturalContext", content.culturalContext)
                parseObject.put("traditionalKnowledge", content.traditionalKnowledge)
                parseObject.put("uploadedBy", ParseUser.getCurrentUser()?.objectId ?: "")
                parseObject.put("uploadDate", Date())
                parseObject.put("approvalStatus", ApprovalStatus.PENDING.name)
                parseObject.put("likes", 0)
                parseObject.put("shares", 0)
                parseObject.put("culturalEducationValue", content.culturalEducationValue)
                parseObject.save()

                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getApprovedCulturalContent(
        contentType: ContentType? = null,
        language: String = "en",
    ): Result<List<MultimediaContent>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("MultimediaContent")
                query.whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)

                contentType?.let {
                    query.whereEqualTo("type", it.name)
                }

                query.orderByDescending("likes")
                query.orderByDescending("uploadDate")

                val results = query.find()
                val content =
                    results.mapNotNull { parseObject ->
                        parseMultimediaContentFromParseObject(parseObject, language)
                    }
                Result.success(content)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getCulturalTraditions(
        category: TraditionCategory? = null,
        language: String = "en",
    ): Result<List<CulturalTradition>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("CulturalTradition")

                category?.let {
                    query.whereEqualTo("category", it.name)
                }

                query.orderByDescending("lastUpdated")

                val results = query.find()
                val traditions =
                    results.mapNotNull { parseObject ->
                        parseCulturalTraditionFromParseObject(parseObject, language)
                    }
                Result.success(traditions)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Utility Functions for Parsing
    private fun parseFestivalFromParseObject(
        parseObject: ParseObject,
        language: String,
    ): Festival? {
        return try {
            Festival(
                id = parseObject.objectId,
                name = parseObject.getString("name") ?: "",
                date = parseObject.getDate("date") ?: Date(),
                duration = parseObject.getInt("duration"),
                significance = parseObject.getString("significance") ?: "",
                imageUrl = parseObject.getString("imageUrl") ?: "",
                isActive = parseObject.getBoolean("isActive"),
                traditions = emptyList(), // Simplified for now
                culturalActivities = emptyList(),
                roosterCompetitions = emptyList(),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseGroupOrderFromParseObject(parseObject: ParseObject): GroupOrder? {
        return try {
            GroupOrder(
                id = parseObject.objectId,
                festivalId = parseObject.getString("festivalId") ?: "",
                title = parseObject.getString("title") ?: "",
                description = parseObject.getString("description") ?: "",
                organizer = parseObject.getParseUser("organizer"),
                targetQuantity = parseObject.getInt("targetQuantity"),
                currentQuantity = parseObject.getInt("currentQuantity"),
                unitPrice = parseObject.getDouble("unitPrice"),
                discountedPrice = parseObject.getDouble("discountedPrice"),
                deadline = parseObject.getDate("deadline") ?: Date(),
                deliveryDate = parseObject.getDate("deliveryDate") ?: Date(),
                status = GroupOrderStatus.valueOf(parseObject.getString("status") ?: "OPEN"),
                itemType = parseObject.getString("itemType") ?: "",
                culturalSignificance = parseObject.getString("culturalSignificance") ?: "",
                festivalContext = parseObject.getString("festivalContext") ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCompetitionFromParseObject(
        parseObject: ParseObject,
        language: String,
    ): FestivalCompetition? {
        return try {
            FestivalCompetition(
                id = parseObject.objectId,
                festivalId = parseObject.getString("festivalId") ?: "",
                name = parseObject.getString("name") ?: "",
                description = parseObject.getString("description") ?: "",
                category =
                    CompetitionCategory.valueOf(
                        parseObject.getString("category") ?: "STRENGTH",
                    ),
                startDate = parseObject.getDate("startDate") ?: Date(),
                endDate = parseObject.getDate("endDate") ?: Date(),
                registrationDeadline = parseObject.getDate("registrationDeadline") ?: Date(),
                maxParticipants = parseObject.getInt("maxParticipants"),
                currentParticipants = parseObject.getInt("currentParticipants"),
                entryFee = parseObject.getDouble("entryFee"),
                culturalContext = parseObject.getString("culturalContext") ?: "",
                traditionalSignificance = parseObject.getString("traditionalSignificance") ?: "",
                status = CompetitionStatus.valueOf(parseObject.getString("status") ?: "UPCOMING"),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseShowcaseFromParseObject(parseObject: ParseObject): RoosterShowcase? {
        return try {
            RoosterShowcase(
                id = parseObject.objectId,
                ownerId = parseObject.getString("ownerId") ?: "",
                ownerName = parseObject.getString("ownerName") ?: "",
                fowlId = parseObject.getString("fowlId") ?: "",
                roosterName = parseObject.getString("roosterName") ?: "",
                showcaseType =
                    ShowcaseType.valueOf(
                        parseObject.getString("showcaseType") ?: "STRENGTH",
                    ),
                festivalId = parseObject.getString("festivalId") ?: "",
                title = parseObject.getString("title") ?: "",
                description = parseObject.getString("description") ?: "",
                culturalStory = parseObject.getString("culturalStory") ?: "",
                traditionalLineage = parseObject.getString("traditionalLineage") ?: "",
                likes = parseObject.getInt("likes"),
                createdAt = parseObject.getDate("createdAt") ?: Date(),
                isVerified = parseObject.getBoolean("isVerified"),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMultimediaContentFromParseObject(
        parseObject: ParseObject,
        language: String,
    ): MultimediaContent? {
        return try {
            MultimediaContent(
                id = parseObject.objectId,
                type = ContentType.valueOf(parseObject.getString("type") ?: "IMAGE"),
                url = parseObject.getString("url") ?: "",
                title = parseObject.getString("title") ?: "",
                description = parseObject.getString("description") ?: "",
                culturalContext = parseObject.getString("culturalContext") ?: "",
                traditionalKnowledge = parseObject.getString("traditionalKnowledge") ?: "",
                uploadedBy = parseObject.getString("uploadedBy") ?: "",
                uploadDate = parseObject.getDate("uploadDate") ?: Date(),
                approvalStatus =
                    ApprovalStatus.valueOf(
                        parseObject.getString("approvalStatus") ?: "PENDING",
                    ),
                likes = parseObject.getInt("likes"),
                shares = parseObject.getInt("shares"),
                culturalEducationValue = parseObject.getString("culturalEducationValue") ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCulturalTraditionFromParseObject(
        parseObject: ParseObject,
        language: String,
    ): CulturalTradition? {
        return try {
            CulturalTradition(
                id = parseObject.objectId,
                name = parseObject.getString("name") ?: "",
                category =
                    TraditionCategory.valueOf(
                        parseObject.getString("category") ?: "ROOSTER_CARE",
                    ),
                description = parseObject.getString("description") ?: "",
                origin = parseObject.getString("origin") ?: "",
                historicalBackground = parseObject.getString("historicalBackground") ?: "",
                modernRelevance = parseObject.getString("modernRelevance") ?: "",
                preservationStatus =
                    PreservationStatus.valueOf(
                        parseObject.getString("preservationStatus") ?: "ACTIVE",
                    ),
                lastUpdated = parseObject.getDate("lastUpdated") ?: Date(),
            )
        } catch (e: Exception) {
            null
        }
    }
}
