package com.example.rooster

import com.parse.ParseObject
import java.util.Date

// Group Chat System
data class GroupChat(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: ChatCategory = ChatCategory.BREEDING,
    val adminId: String = "",
    val adminName: String = "",
    val memberIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val maxMembers: Int = 50,
    val isPrivate: Boolean = false,
    val requiresApproval: Boolean = true,
    val tags: List<String> = emptyList(),
    val region: String = "",
    val language: String = "en",
    val lastActivity: Date = Date(),
    val createdAt: Date = Date(),
    val rules: List<String> = emptyList(),
    val pinnedMessages: List<String> = emptyList(),
    val chatSettings: ChatSettings = ChatSettings(),
    val metadata: Map<String, Any> = emptyMap(),
)

enum class ChatCategory {
    BREEDING,
    HEALTH_CARE,
    COMPETITIONS,
    MARKETPLACE,
    CULTURAL_EVENTS,
    BEGINNERS,
    REGIONAL,
    GENERAL,
}

data class ChatSettings(
    val allowMediaSharing: Boolean = true,
    val allowVoiceMessages: Boolean = true,
    val allowFileSharing: Boolean = true,
    val messageRetentionDays: Int = 30,
    val lowBandwidthMode: Boolean = false,
    val autoDeleteMedia: Boolean = true,
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val notificationSettings: NotificationSettings = NotificationSettings(),
)

enum class CompressionLevel {
    LOW, // High quality, large files
    MEDIUM, // Balanced quality/size
    HIGH, // Lower quality, small files
    ULTRA, // Very low quality, tiny files for poor connections
}

data class NotificationSettings(
    val muteNotifications: Boolean = false,
    val muteUntil: Date? = null,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showPreview: Boolean = true,
)

// Enhanced Message System
data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val content: String = "",
    val mediaUrl: String = "",
    val mediaThumbnail: String = "",
    val mediaSize: Long = 0,
    val mediaDuration: Int = 0, // For audio/video in seconds
    val replyToMessageId: String = "",
    val timestamp: Date = Date(),
    val editedAt: Date? = null,
    val reactions: Map<String, List<String>> = emptyMap(), // emoji -> list of user IDs
    val mentions: List<String> = emptyList(),
    val isForwarded: Boolean = false,
    val forwardedFrom: String = "",
    val readBy: List<MessageRead> = emptyList(),
    val metadata: MessageMetadata = MessageMetadata(),
    val status: MessageStatus = MessageStatus.SENT,
    val priority: MessagePriority = MessagePriority.NORMAL,
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    VOICE_NOTE,
    DOCUMENT,
    LOCATION,
    CONTACT,
    POLL,
    SYSTEM_NOTIFICATION,
    LIVE_BROADCAST_LINK,
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
}

enum class MessagePriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
}

data class MessageRead(
    val userId: String = "",
    val readAt: Date = Date(),
)

data class MessageMetadata(
    val deviceInfo: String = "",
    val locationSent: String = "",
    val isEncrypted: Boolean = false,
    val compressionUsed: ImageCompressionLevel = ImageCompressionLevel.MEDIUM,
    val originalSize: Long = 0,
    val compressedSize: Long = 0,
    val transmissionTime: Long = 0,
)

// Live Broadcasting System
data class LiveBroadcast(
    val id: String = "",
    val broadcasterId: String = "",
    val broadcasterName: String = "",
    val broadcasterAvatar: String = "",
    val title: String = "",
    val description: String = "",
    val category: BroadcastCategory = BroadcastCategory.SHOWCASE,
    val tags: List<String> = emptyList(),
    val language: String = "en",
    val streamUrl: String = "",
    val thumbnailUrl: String = "",
    val isLive: Boolean = false,
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val duration: Int = 0, // in seconds
    val viewerCount: Int = 0,
    val maxViewers: Int = 0,
    val chatEnabled: Boolean = true,
    val recordingEnabled: Boolean = true,
    val recordingUrl: String = "",
    val quality: StreamQuality = StreamQuality.MEDIUM,
    val viewers: List<BroadcastViewer> = emptyList(),
    val moderators: List<String> = emptyList(),
    val settings: BroadcastSettings = BroadcastSettings(),
    val statistics: BroadcastStatistics = BroadcastStatistics(),
)

enum class BroadcastCategory {
    SHOWCASE,
    EDUCATION,
    BREEDING_TIPS,
    HEALTH_CONSULTATION,
    COMPETITION,
    CULTURAL_EVENT,
    Q_AND_A,
    GENERAL,
}

enum class StreamQuality {
    LOW, // 240p, very low bandwidth
    MEDIUM, // 480p, moderate bandwidth
    HIGH, // 720p, good bandwidth
    HD, // 1080p, excellent bandwidth
}

data class BroadcastViewer(
    val userId: String = "",
    val userName: String = "",
    val joinTime: Date = Date(),
    val leaveTime: Date? = null,
    val isActive: Boolean = true,
    val quality: StreamQuality = StreamQuality.MEDIUM,
)

data class BroadcastSettings(
    val allowComments: Boolean = true,
    val moderateComments: Boolean = false,
    val allowScreenRecording: Boolean = true,
    val restrictedToFollowers: Boolean = false,
    val geoRestrictions: List<String> = emptyList(),
    val ageRestriction: Int = 0,
    val monetizationEnabled: Boolean = false,
)

data class BroadcastStatistics(
    val totalViews: Int = 0,
    val uniqueViewers: Int = 0,
    val averageWatchTime: Int = 0, // in seconds
    val peakViewers: Int = 0,
    val peakViewerTime: Date? = null,
    val chatMessages: Int = 0,
    val likes: Int = 0,
    val shares: Int = 0,
    val engagementRate: Double = 0.0,
)

// Voice/Video Calling System
data class Call(
    val id: String = "",
    val callType: CallType = CallType.VOICE,
    val initiatorId: String = "",
    val initiatorName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val status: CallStatus = CallStatus.INITIATED,
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val duration: Int = 0, // in seconds
    val quality: CallQuality = CallQuality.MEDIUM,
    val callSettings: CallSettings = CallSettings(),
    val metadata: CallMetadata = CallMetadata(),
)

enum class CallType {
    VOICE,
    VIDEO,
    SCREEN_SHARE,
    GROUP_VOICE,
    GROUP_VIDEO,
}

enum class CallStatus {
    INITIATED,
    RINGING,
    ACCEPTED,
    IN_PROGRESS,
    ENDED,
    MISSED,
    DECLINED,
    FAILED,
}

enum class CallQuality {
    LOW, // Poor connection
    MEDIUM, // Stable connection
    HIGH, // Good connection
    EXCELLENT, // Perfect connection
}

data class CallSettings(
    val videoEnabled: Boolean = true,
    val audioEnabled: Boolean = true,
    val speakerEnabled: Boolean = false,
    val recordingEnabled: Boolean = false,
    val encryptionEnabled: Boolean = true,
    val lowBandwidthMode: Boolean = false,
)

data class CallMetadata(
    val connectionQuality: List<QualityMetric> = emptyList(),
    val bandwidth: BandwidthInfo = BandwidthInfo(),
    val deviceInfo: String = "",
    val networkType: String = "",
)

data class QualityMetric(
    val timestamp: Date = Date(),
    val quality: CallQuality = CallQuality.MEDIUM,
    val latency: Int = 0, // in ms
    val packetLoss: Double = 0.0,
)

data class BandwidthInfo(
    val upstream: Int = 0, // kbps
    val downstream: Int = 0, // kbps
    val isStable: Boolean = true,
)

// Advanced Forum System
data class BreederForum(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: ForumCategory = ForumCategory.GENERAL,
    val moderatorIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val threadCount: Int = 0,
    val postCount: Int = 0,
    val tags: List<String> = emptyList(),
    val rules: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val requiresApproval: Boolean = false,
    val expertLevel: ExpertLevel = ExpertLevel.ALL_LEVELS,
    val language: String = "en",
    val region: String = "",
    val createdAt: Date = Date(),
    val lastActivity: Date = Date(),
    val settings: ForumSettings = ForumSettings(),
)

enum class ForumCategory {
    GENERAL,
    BREEDING_TECHNIQUES,
    HEALTH_VETERINARY,
    COMPETITIONS,
    EQUIPMENT_SUPPLIES,
    MARKET_DISCUSSION,
    BEGINNERS_CORNER,
    EXPERT_ADVICE,
    REGIONAL_DISCUSSION,
    CULTURAL_TRADITIONS,
}

enum class ExpertLevel {
    BEGINNERS,
    INTERMEDIATE,
    ADVANCED,
    EXPERTS_ONLY,
    ALL_LEVELS,
}

data class ForumSettings(
    val allowAttachments: Boolean = true,
    val allowPolls: Boolean = true,
    val moderateNewPosts: Boolean = false,
    val enableReputation: Boolean = true,
    val enableExpertBadges: Boolean = true,
    val maxAttachmentSize: Long = 10 * 1024 * 1024, // 10MB
    val archiveOldThreads: Boolean = true,
    val archiveThresholdDays: Int = 365,
)

data class ForumThread(
    val id: String = "",
    val forumId: String = "",
    val title: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val category: ThreadCategory = ThreadCategory.DISCUSSION,
    val tags: List<String> = emptyList(),
    val content: String = "",
    val attachments: List<Attachment> = emptyList(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isAnswered: Boolean = false,
    val acceptedAnswerId: String = "",
    val views: Int = 0,
    val replies: Int = 0,
    val likes: Int = 0,
    val priority: ThreadPriority = ThreadPriority.NORMAL,
    val createdAt: Date = Date(),
    val lastReplyAt: Date = Date(),
    val lastReplyBy: String = "",
    val reputation: ThreadReputation = ThreadReputation(),
)

enum class ThreadCategory {
    QUESTION,
    DISCUSSION,
    ANNOUNCEMENT,
    TUTORIAL,
    SHOWCASE,
    HELP_REQUEST,
    MARKET_POST,
}

enum class ThreadPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
}

data class ThreadReputation(
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val score: Int = 0,
    val helpfulCount: Int = 0,
    val expertEndorsements: List<String> = emptyList(),
)

data class ForumPost(
    val id: String = "",
    val threadId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val attachments: List<Attachment> = emptyList(),
    val replyToPostId: String = "",
    val isAnswer: Boolean = false,
    val isAcceptedAnswer: Boolean = false,
    val createdAt: Date = Date(),
    val editedAt: Date? = null,
    val reputation: PostReputation = PostReputation(),
    val mentions: List<String> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(),
)

data class PostReputation(
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val score: Int = 0,
    val helpfulCount: Int = 0,
    val expertEndorsements: List<String> = emptyList(),
    val reportCount: Int = 0,
)

data class Attachment(
    val id: String = "",
    val fileName: String = "",
    val fileType: String = "",
    val fileUrl: String = "",
    val thumbnailUrl: String = "",
    val fileSize: Long = 0,
    val uploadedAt: Date = Date(),
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val isProcessed: Boolean = false,
)

// User Reputation and Expertise System
data class UserReputation(
    val userId: String = "",
    val overallScore: Int = 0,
    val level: ReputationLevel = ReputationLevel.NEWCOMER,
    val expertiseAreas: List<ExpertiseArea> = emptyList(),
    val badges: List<Badge> = emptyList(),
    val achievements: List<SocialAchievement> = emptyList(),
    val statistics: ReputationStatistics = ReputationStatistics(),
    val endorsements: List<Endorsement> = emptyList(),
    val lastUpdated: Date = Date(),
)

enum class ReputationLevel {
    NEWCOMER, // 0-99 points
    APPRENTICE, // 100-499 points
    PRACTITIONER, // 500-1499 points
    EXPERT, // 1500-4999 points
    MASTER, // 5000-14999 points
    GRANDMASTER, // 15000+ points
}

data class ExpertiseArea(
    val area: String = "",
    val level: ExpertiseLevel = ExpertiseLevel.BEGINNER,
    val score: Int = 0,
    val verifications: Int = 0,
    val contributions: Int = 0,
    val endorsements: Int = 0,
)

enum class ExpertiseLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT,
    AUTHORITY,
}

data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val category: BadgeCategory = BadgeCategory.PARTICIPATION,
    val rarity: BadgeRarity = BadgeRarity.COMMON,
    val unlockedAt: Date = Date(),
    val criteria: String = "",
)

enum class BadgeCategory {
    PARTICIPATION,
    EXPERTISE,
    HELPFULNESS,
    LEADERSHIP,
    ACHIEVEMENT,
    SPECIAL_EVENT,
    CULTURAL_CONTRIBUTION,
}

enum class BadgeRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
}

data class SocialAchievement(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: AchievementType = AchievementType.MILESTONE,
    val progress: Int = 0,
    val target: Int = 100,
    val isCompleted: Boolean = false,
    val completedAt: Date? = null,
    val reward: String = "",
)

enum class AchievementType {
    MILESTONE,
    STREAK,
    CHALLENGE,
    COMMUNITY,
    EXPERTISE,
    CULTURAL,
}

data class ReputationStatistics(
    val totalPosts: Int = 0,
    val totalLikes: Int = 0,
    val totalHelpfulVotes: Int = 0,
    val totalAnswersAccepted: Int = 0,
    val totalQuestionsAsked: Int = 0,
    val totalAnswersGiven: Int = 0,
    val averageResponseTime: Int = 0, // in minutes
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val mentionCount: Int = 0,
)

data class Endorsement(
    val id: String = "",
    val endorserId: String = "",
    val endorserName: String = "",
    val skill: String = "",
    val comment: String = "",
    val createdAt: Date = Date(),
    val isVerified: Boolean = false,
)

// Multimedia Optimization System
data class MediaItem(
    val id: String = "",
    val originalUrl: String = "",
    val optimizedUrls: Map<String, String> = emptyMap(), // quality -> url
    val thumbnailUrl: String = "",
    val mediaType: MediaType = MediaType.IMAGE,
    val originalSize: Long = 0,
    val optimizedSizes: Map<String, Long> = emptyMap(),
    val compressionSettings: MediaCompressionSettings = MediaCompressionSettings(),
    val metadata: MediaMetadata = MediaMetadata(),
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val uploadedAt: Date = Date(),
    val processedAt: Date? = null,
)

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
}

enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}

data class MediaCompressionSettings(
    val imageQuality: Int = 80, // 1-100
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    val audioQuality: AudioQuality = AudioQuality.MEDIUM,
    val maxWidth: Int = 1080,
    val maxHeight: Int = 1080,
    val maxDuration: Int = 300, // seconds for video/audio
    val generateThumbnail: Boolean = true,
    val stripMetadata: Boolean = true,
)

enum class VideoQuality {
    LOW, // 240p
    MEDIUM, // 480p
    HIGH, // 720p
    HD, // 1080p
}

enum class AudioQuality {
    LOW, // 64 kbps
    MEDIUM, // 128 kbps
    HIGH, // 256 kbps
}

data class MediaMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
    val format: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val fps: Int = 0,
    val colorSpace: String = "",
    val orientation: Int = 0,
)

// Parse Object Extensions
fun GroupChat.toParseObject(): ParseObject {
    val parseObject = ParseObject("GroupChat")
    parseObject.put("name", name)
    parseObject.put("description", description)
    parseObject.put("category", category.name)
    parseObject.put("adminId", adminId)
    parseObject.put("adminName", adminName)
    parseObject.put("memberIds", memberIds)
    parseObject.put("memberCount", memberCount)
    parseObject.put("maxMembers", maxMembers)
    parseObject.put("isPrivate", isPrivate)
    parseObject.put("requiresApproval", requiresApproval)
    parseObject.put("tags", tags)
    parseObject.put("region", region)
    parseObject.put("language", language)
    parseObject.put("lastActivity", lastActivity)
    parseObject.put("rules", rules)
    parseObject.put("pinnedMessages", pinnedMessages)
    return parseObject
}

fun ChatMessage.toParseObject(): ParseObject {
    val parseObject = ParseObject("ChatMessage")
    parseObject.put("chatId", chatId)
    parseObject.put("senderId", senderId)
    parseObject.put("senderName", senderName)
    parseObject.put("senderAvatar", senderAvatar)
    parseObject.put("messageType", messageType.name)
    parseObject.put("content", content)
    parseObject.put("mediaUrl", mediaUrl)
    parseObject.put("mediaThumbnail", mediaThumbnail)
    parseObject.put("mediaSize", mediaSize)
    parseObject.put("mediaDuration", mediaDuration)
    parseObject.put("replyToMessageId", replyToMessageId)
    parseObject.put("timestamp", timestamp)
    editedAt?.let { parseObject.put("editedAt", it) }
    parseObject.put("reactions", reactions)
    parseObject.put("mentions", mentions)
    parseObject.put("isForwarded", isForwarded)
    parseObject.put("forwardedFrom", forwardedFrom)
    parseObject.put("status", status.name)
    parseObject.put("priority", priority.name)
    return parseObject
}

fun LiveBroadcast.toParseObject(): ParseObject {
    val parseObject = ParseObject("LiveBroadcast")
    parseObject.put("broadcasterId", broadcasterId)
    parseObject.put("broadcasterName", broadcasterName)
    parseObject.put("broadcasterAvatar", broadcasterAvatar)
    parseObject.put("title", title)
    parseObject.put("description", description)
    parseObject.put("category", category.name)
    parseObject.put("tags", tags)
    parseObject.put("language", language)
    parseObject.put("streamUrl", streamUrl)
    parseObject.put("thumbnailUrl", thumbnailUrl)
    parseObject.put("isLive", isLive)
    parseObject.put("startTime", startTime)
    endTime?.let { parseObject.put("endTime", it) }
    parseObject.put("duration", duration)
    parseObject.put("viewerCount", viewerCount)
    parseObject.put("maxViewers", maxViewers)
    parseObject.put("chatEnabled", chatEnabled)
    parseObject.put("recordingEnabled", recordingEnabled)
    parseObject.put("recordingUrl", recordingUrl)
    parseObject.put("quality", quality.name)
    parseObject.put("moderators", moderators)
    return parseObject
}

fun BreederForum.toParseObject(): ParseObject {
    val parseObject = ParseObject("BreederForum")
    parseObject.put("name", name)
    parseObject.put("description", description)
    parseObject.put("category", category.name)
    parseObject.put("moderatorIds", moderatorIds)
    parseObject.put("memberCount", memberCount)
    parseObject.put("threadCount", threadCount)
    parseObject.put("postCount", postCount)
    parseObject.put("tags", tags)
    parseObject.put("rules", rules)
    parseObject.put("isPrivate", isPrivate)
    parseObject.put("requiresApproval", requiresApproval)
    parseObject.put("expertLevel", expertLevel.name)
    parseObject.put("language", language)
    parseObject.put("region", region)
    parseObject.put("lastActivity", lastActivity)
    return parseObject
}

fun ForumThread.toParseObject(): ParseObject {
    val parseObject = ParseObject("ForumThread")
    parseObject.put("forumId", forumId)
    parseObject.put("title", title)
    parseObject.put("authorId", authorId)
    parseObject.put("authorName", authorName)
    parseObject.put("category", category.name)
    parseObject.put("tags", tags)
    parseObject.put("content", content)
    parseObject.put("isPinned", isPinned)
    parseObject.put("isLocked", isLocked)
    parseObject.put("isAnswered", isAnswered)
    parseObject.put("acceptedAnswerId", acceptedAnswerId)
    parseObject.put("views", views)
    parseObject.put("replies", replies)
    parseObject.put("likes", likes)
    parseObject.put("priority", priority.name)
    parseObject.put("lastReplyAt", lastReplyAt)
    parseObject.put("lastReplyBy", lastReplyBy)
    return parseObject
}

fun UserReputation.toParseObject(): ParseObject {
    val parseObject = ParseObject("UserReputation")
    parseObject.put("userId", userId)
    parseObject.put("overallScore", overallScore)
    parseObject.put("level", level.name)
    parseObject.put("lastUpdated", lastUpdated)
    return parseObject
}
