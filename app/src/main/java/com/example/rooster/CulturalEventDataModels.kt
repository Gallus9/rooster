package com.example.rooster

import com.parse.ParseObject
import com.parse.ParseUser
import java.util.Date

// Festival and Cultural Event Models
data class Festival(
    val id: String = "",
    val name: String = "",
    val nameTranslations: Map<String, String> = emptyMap(), // Multi-language support
    val date: Date = Date(),
    val duration: Int = 1, // Duration in days
    val significance: String = "",
    val significanceTranslations: Map<String, String> = emptyMap(),
    val traditions: List<String> = emptyList(),
    val traditionTranslations: Map<String, List<String>> = emptyMap(),
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val culturalActivities: List<String> = emptyList(),
    val roosterCompetitions: List<String> = emptyList(),
)

data class CulturalEvent(
    val id: String = "",
    val festivalId: String = "",
    val title: String = "",
    val titleTranslations: Map<String, String> = emptyMap(),
    val description: String = "",
    val descriptionTranslations: Map<String, String> = emptyMap(),
    val eventType: CulturalEventType = CulturalEventType.GENERAL,
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val location: String = "",
    val organizer: String = "",
    val maxParticipants: Int = 0,
    val currentParticipants: Int = 0,
    val prizeMoney: Double = 0.0,
    val competitionRules: List<String> = emptyList(),
    val registrationFee: Double = 0.0,
    val isRegistrationOpen: Boolean = true,
    val culturalContext: String = "",
    val multimediaContent: List<MultimediaContent> = emptyList(),
)

enum class CulturalEventType {
    ROOSTER_COMPETITION,
    STRENGTH_CONTEST,
    SPEED_RACE,
    BEAUTY_CONTEST,
    CULTURAL_SHOWCASE,
    GROUP_ORDERING,
    TRADITIONAL_CEREMONY,
    GENERAL,
}

// Group Ordering System
data class GroupOrder(
    val id: String = "",
    val festivalId: String = "",
    val title: String = "",
    val description: String = "",
    val organizer: ParseUser? = null,
    val targetQuantity: Int = 0,
    val currentQuantity: Int = 0,
    val unitPrice: Double = 0.0,
    val discountedPrice: Double = 0.0,
    val deadline: Date = Date(),
    val deliveryDate: Date = Date(),
    val status: GroupOrderStatus = GroupOrderStatus.OPEN,
    val participants: List<GroupOrderParticipant> = emptyList(),
    val itemType: String = "", // roosters, feed, equipment, etc.
    val itemDetails: Map<String, Any> = emptyMap(),
    val culturalSignificance: String = "",
    val festivalContext: String = "",
)

enum class GroupOrderStatus {
    OPEN,
    TARGET_REACHED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
}

data class GroupOrderParticipant(
    val userId: String = "",
    val userName: String = "",
    val quantity: Int = 0,
    val totalAmount: Double = 0.0,
    val joinedAt: Date = Date(),
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
)

enum class PaymentStatus {
    PENDING,
    PAID,
    REFUNDED,
}

// Cultural Showcase System
data class RoosterShowcase(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val fowlId: String = "",
    val roosterName: String = "",
    val showcaseType: ShowcaseType = ShowcaseType.STRENGTH,
    val festivalId: String = "",
    val title: String = "",
    val description: String = "",
    val achievements: List<Achievement> = emptyList(),
    val photos: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val stats: RoosterStats = RoosterStats(),
    val culturalStory: String = "",
    val traditionalLineage: String = "",
    val competitionHistory: List<CompetitionRecord> = emptyList(),
    val likes: Int = 0,
    val comments: List<ShowcaseComment> = emptyList(),
    val createdAt: Date = Date(),
    val isVerified: Boolean = false,
    val verificationDetails: VerificationDetails? = null,
)

enum class ShowcaseType {
    STRENGTH,
    SPEED,
    BEAUTY,
    ENDURANCE,
    INTELLIGENCE,
    CULTURAL_HERITAGE,
    TRADITIONAL_SKILLS,
}

data class RoosterStats(
    val weight: Double = 0.0,
    val height: Double = 0.0,
    val strength: Int = 0, // 1-10 scale
    val speed: Int = 0, // 1-10 scale
    val agility: Int = 0, // 1-10 scale
    val endurance: Int = 0, // 1-10 scale
    val intelligence: Int = 0, // 1-10 scale
    val overallRating: Double = 0.0,
)

data class Achievement(
    val title: String = "",
    val description: String = "",
    val dateAchieved: Date = Date(),
    val eventName: String = "",
    val rank: Int = 0,
    val category: String = "",
    val certificateUrl: String = "",
    val culturalSignificance: String = "",
)

data class CompetitionRecord(
    val eventId: String = "",
    val eventName: String = "",
    val date: Date = Date(),
    val category: String = "",
    val rank: Int = 0,
    val totalParticipants: Int = 0,
    val score: Double = 0.0,
    val prizeMoney: Double = 0.0,
    val certificateUrl: String = "",
)

data class ShowcaseComment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val comment: String = "",
    val timestamp: Date = Date(),
    val likes: Int = 0,
    val culturalAppreciation: String = "", // Cultural appreciation notes
)

data class VerificationDetails(
    val verifiedBy: String = "",
    val verificationDate: Date = Date(),
    val verificationNotes: String = "",
    val witnessNames: List<String> = emptyList(),
    val officialDocuments: List<String> = emptyList(),
)

// Competition and Leaderboard System
data class FestivalCompetition(
    val id: String = "",
    val festivalId: String = "",
    val name: String = "",
    val nameTranslations: Map<String, String> = emptyMap(),
    val description: String = "",
    val category: CompetitionCategory = CompetitionCategory.STRENGTH,
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val registrationDeadline: Date = Date(),
    val maxParticipants: Int = 0,
    val currentParticipants: Int = 0,
    val entryFee: Double = 0.0,
    val prizes: List<Prize> = emptyList(),
    val rules: List<String> = emptyList(),
    val rulesTranslations: Map<String, List<String>> = emptyMap(),
    val judgingCriteria: List<JudgingCriteria> = emptyList(),
    val culturalContext: String = "",
    val traditionalSignificance: String = "",
    val status: CompetitionStatus = CompetitionStatus.UPCOMING,
)

enum class CompetitionCategory {
    STRENGTH,
    SPEED,
    BEAUTY,
    ENDURANCE,
    INTELLIGENCE,
    TRADITIONAL_SKILLS,
    OVERALL_EXCELLENCE,
}

enum class CompetitionStatus {
    UPCOMING,
    REGISTRATION_OPEN,
    REGISTRATION_CLOSED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

data class Prize(
    val rank: Int = 0,
    val title: String = "",
    val cashPrize: Double = 0.0,
    val trophy: String = "",
    val certificate: String = "",
    val culturalHonor: String = "",
    val additionalBenefits: List<String> = emptyList(),
)

data class JudgingCriteria(
    val criterion: String = "",
    val weight: Double = 0.0, // Percentage weight in final score
    val description: String = "",
    val culturalImportance: String = "",
)

data class CompetitionEntry(
    val id: String = "",
    val competitionId: String = "",
    val participantId: String = "",
    val participantName: String = "",
    val fowlId: String = "",
    val roosterName: String = "",
    val registrationDate: Date = Date(),
    val scores: Map<String, Double> = emptyMap(),
    val finalScore: Double = 0.0,
    val rank: Int = 0,
    val status: EntryStatus = EntryStatus.REGISTERED,
    val culturalPresentation: String = "",
    val supportingDocuments: List<String> = emptyList(),
)

enum class EntryStatus {
    REGISTERED,
    CONFIRMED,
    DISQUALIFIED,
    COMPLETED,
    WINNER,
}

// Leaderboard System
data class FestivalLeaderboard(
    val id: String = "",
    val festivalId: String = "",
    val competitionId: String = "",
    val category: String = "",
    val entries: List<LeaderboardEntry> = emptyList(),
    val lastUpdated: Date = Date(),
    val season: String = "",
    val culturalRecognition: Map<String, String> = emptyMap(),
)

data class LeaderboardEntry(
    val rank: Int = 0,
    val participantId: String = "",
    val participantName: String = "",
    val fowlId: String = "",
    val roosterName: String = "",
    val score: Double = 0.0,
    val achievements: List<String> = emptyList(),
    val culturalHonors: List<String> = emptyList(),
    val profileImageUrl: String = "",
    val roosterImageUrl: String = "",
    val traditionalLineage: String = "",
    val villageOrRegion: String = "",
)

// Multimedia Content for Cultural Preservation
data class MultimediaContent(
    val id: String = "",
    val type: ContentType = ContentType.IMAGE,
    val url: String = "",
    val title: String = "",
    val titleTranslations: Map<String, String> = emptyMap(),
    val description: String = "",
    val descriptionTranslations: Map<String, String> = emptyMap(),
    val culturalContext: String = "",
    val traditionalKnowledge: String = "",
    val uploadedBy: String = "",
    val uploadDate: Date = Date(),
    val tags: List<String> = emptyList(),
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val likes: Int = 0,
    val shares: Int = 0,
    val culturalEducationValue: String = "",
)

enum class ContentType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    STORY,
    RECIPE,
    TRADITION,
}

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FLAGGED,
}

// Traditional Culture Promotion
data class CulturalTradition(
    val id: String = "",
    val name: String = "",
    val nameTranslations: Map<String, String> = emptyMap(),
    val category: TraditionCategory = TraditionCategory.ROOSTER_CARE,
    val description: String = "",
    val descriptionTranslations: Map<String, String> = emptyMap(),
    val origin: String = "",
    val region: List<String> = emptyList(),
    val historicalBackground: String = "",
    val modernRelevance: String = "",
    val practiceSteps: List<String> = emptyList(),
    val relatedFestivals: List<String> = emptyList(),
    val multimediaContent: List<MultimediaContent> = emptyList(),
    val preservationStatus: PreservationStatus = PreservationStatus.ACTIVE,
    val communityContributors: List<String> = emptyList(),
    val lastUpdated: Date = Date(),
)

enum class TraditionCategory {
    ROOSTER_CARE,
    BREEDING_TECHNIQUES,
    COMPETITION_PREPARATION,
    FESTIVAL_RITUALS,
    CULTURAL_CEREMONIES,
    TRADITIONAL_MEDICINE,
    FOLKLORE_STORIES,
    COOKING_RECIPES,
}

enum class PreservationStatus {
    ACTIVE,
    DECLINING,
    RARE,
    REVIVED,
    DOCUMENTED_ONLY,
}

// Parse Object Extensions for Backend Integration
fun Festival.toParseObject(): ParseObject {
    val parseObject = ParseObject("Festival")
    parseObject.put("name", name)
    parseObject.put("nameTranslations", nameTranslations)
    parseObject.put("date", date)
    parseObject.put("duration", duration)
    parseObject.put("significance", significance)
    parseObject.put("significanceTranslations", significanceTranslations)
    parseObject.put("traditions", traditions)
    parseObject.put("traditionTranslations", traditionTranslations)
    parseObject.put("imageUrl", imageUrl)
    parseObject.put("isActive", isActive)
    parseObject.put("culturalActivities", culturalActivities)
    parseObject.put("roosterCompetitions", roosterCompetitions)
    return parseObject
}

fun GroupOrder.toParseObject(): ParseObject {
    val parseObject = ParseObject("GroupOrder")
    parseObject.put("festivalId", festivalId)
    parseObject.put("title", title)
    parseObject.put("description", description)
    organizer?.let { parseObject.put("organizer", it) }
    parseObject.put("targetQuantity", targetQuantity)
    parseObject.put("currentQuantity", currentQuantity)
    parseObject.put("unitPrice", unitPrice)
    parseObject.put("discountedPrice", discountedPrice)
    parseObject.put("deadline", deadline)
    parseObject.put("deliveryDate", deliveryDate)
    parseObject.put("status", status.name)
    parseObject.put("itemType", itemType)
    parseObject.put("itemDetails", itemDetails)
    parseObject.put("culturalSignificance", culturalSignificance)
    parseObject.put("festivalContext", festivalContext)
    return parseObject
}

fun RoosterShowcase.toParseObject(): ParseObject {
    val parseObject = ParseObject("RoosterShowcase")
    parseObject.put("ownerId", ownerId)
    parseObject.put("ownerName", ownerName)
    parseObject.put("fowlId", fowlId)
    parseObject.put("roosterName", roosterName)
    parseObject.put("showcaseType", showcaseType.name)
    parseObject.put("festivalId", festivalId)
    parseObject.put("title", title)
    parseObject.put("description", description)
    parseObject.put("photos", photos)
    parseObject.put("videos", videos)
    parseObject.put("culturalStory", culturalStory)
    parseObject.put("traditionalLineage", traditionalLineage)
    parseObject.put("likes", likes)
    parseObject.put("createdAt", createdAt)
    parseObject.put("isVerified", isVerified)
    return parseObject
}

fun FestivalCompetition.toParseObject(): ParseObject {
    val parseObject = ParseObject("FestivalCompetition")
    parseObject.put("festivalId", festivalId)
    parseObject.put("name", name)
    parseObject.put("nameTranslations", nameTranslations)
    parseObject.put("description", description)
    parseObject.put("category", category.name)
    parseObject.put("startDate", startDate)
    parseObject.put("endDate", endDate)
    parseObject.put("registrationDeadline", registrationDeadline)
    parseObject.put("maxParticipants", maxParticipants)
    parseObject.put("currentParticipants", currentParticipants)
    parseObject.put("entryFee", entryFee)
    parseObject.put("rules", rules)
    parseObject.put("rulesTranslations", rulesTranslations)
    parseObject.put("culturalContext", culturalContext)
    parseObject.put("traditionalSignificance", traditionalSignificance)
    parseObject.put("status", status.name)
    return parseObject
}
