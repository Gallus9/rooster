package com.example.rooster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.parse.ParseException
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Date

class AdvancedSocialService(private val context: Context) {
    // Network Quality Assessment
    private fun getNetworkQuality(): NetworkQualityLevel {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkQualityLevel.OFFLINE
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return NetworkQualityLevel.POOR

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkQualityLevel.EXCELLENT
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    NetworkQualityLevel.GOOD
                } else {
                    NetworkQualityLevel.FAIR
                }
            }

            else -> NetworkQualityLevel.POOR
        }
    }

    private fun getOptimalCompressionLevel(): ImageCompressionLevel {
        return when (getNetworkQuality()) {
            NetworkQualityLevel.EXCELLENT -> ImageCompressionLevel.LOW
            NetworkQualityLevel.GOOD -> ImageCompressionLevel.MEDIUM
            NetworkQualityLevel.FAIR -> ImageCompressionLevel.HIGH
            NetworkQualityLevel.POOR -> ImageCompressionLevel.ULTRA
            NetworkQualityLevel.OFFLINE -> ImageCompressionLevel.ULTRA
        }
    }

    // Group Chat Management
    suspend fun createGroupChat(groupChat: GroupChat): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = groupChat.toParseObject()
                parseObject.put("createdBy", ParseUser.getCurrentUser()?.objectId ?: "")
                parseObject.put("networkQuality", getNetworkQuality().name)
                parseObject.save()
                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun joinGroupChat(chatId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val query = ParseQuery.getQuery<ParseObject>("GroupChat")
                val chat = query.get(chatId)

                val memberIds =
                    chat.getList<String>("memberIds")?.toMutableList()
                        ?: mutableListOf()
                val userId = currentUser.objectId

                if (!memberIds.contains(userId)) {
                    memberIds.add(userId)
                    chat.put("memberIds", memberIds)
                    chat.put("memberCount", memberIds.size)
                    chat.put("lastActivity", Date())
                    chat.save()
                }

                Result.success(true)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getGroupChats(category: ChatCategory? = null): Result<List<GroupChat>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("GroupChat")
                category?.let { query.whereEqualTo("category", it.name) }
                query.orderByDescending("lastActivity")

                val results = query.find()
                val chats =
                    results.mapNotNull { parseObject ->
                        parseGroupChatFromParseObject(parseObject)
                    }
                Result.success(chats)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Enhanced Message System
    suspend fun sendEnhancedMessage(message: ChatMessage): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject =
                    message.copy(
                        metadata =
                            message.metadata.copy(
                                compressionUsed = getOptimalCompressionLevel(),
                                deviceInfo = android.os.Build.MODEL,
                                transmissionTime = System.currentTimeMillis(),
                            ),
                    ).toParseObject()

                parseObject.save()

                // Update chat last activity
                updateChatActivity(message.chatId)

                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun sendVoiceMessage(
        chatId: String,
        audioData: ByteArray,
        duration: Int,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                // Compress audio based on network quality
                val compressedAudio = compressAudio(audioData, getOptimalCompressionLevel())
                val audioFile = ParseFile("voice_message.m4a", compressedAudio)
                audioFile.save()

                val message =
                    ChatMessage(
                        chatId = chatId,
                        senderId = currentUser.objectId,
                        senderName = currentUser.username ?: "",
                        messageType = MessageType.VOICE_NOTE,
                        mediaUrl = audioFile.url ?: "",
                        mediaSize = compressedAudio.size.toLong(),
                        mediaDuration = duration,
                        timestamp = Date(),
                        metadata =
                            MessageMetadata(
                                compressionUsed = getOptimalCompressionLevel(),
                                originalSize = audioData.size.toLong(),
                                compressedSize = compressedAudio.size.toLong(),
                            ),
                    )

                sendEnhancedMessage(message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendImageMessage(
        chatId: String,
        imageUri: Uri,
        caption: String = "",
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val compressedImage = compressImageAdvanced(imageUri, getOptimalCompressionLevel())
                if (compressedImage == null) {
                    return@withContext Result.failure(Exception("Failed to process image"))
                }

                val imageFile = ParseFile("image_message.jpg", compressedImage.data)
                imageFile.save()

                val thumbnailFile = ParseFile("image_thumbnail.jpg", compressedImage.thumbnail)
                thumbnailFile.save()

                val message =
                    ChatMessage(
                        chatId = chatId,
                        senderId = currentUser.objectId,
                        senderName = currentUser.username ?: "",
                        messageType = MessageType.IMAGE,
                        content = caption,
                        mediaUrl = imageFile.url ?: "",
                        mediaThumbnail = thumbnailFile.url ?: "",
                        mediaSize = compressedImage.data.size.toLong(),
                        timestamp = Date(),
                        metadata =
                            MessageMetadata(
                                compressionUsed = getOptimalCompressionLevel(),
                                originalSize = compressedImage.originalSize,
                                compressedSize = compressedImage.data.size.toLong(),
                            ),
                    )

                sendEnhancedMessage(message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getChatMessages(
        chatId: String,
        messageLimit: Int = 50,
    ): Result<List<ChatMessage>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("ChatMessage")
                query.whereEqualTo("chatId", chatId)
                query.orderByDescending("timestamp")
                query.limit = messageLimit

                val results = query.find()
                val messages =
                    results.mapNotNull { parseObject ->
                        parseChatMessageFromParseObject(parseObject)
                    }
                Result.success(messages.reversed()) // Show oldest first
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Live Broadcasting System
    suspend fun startLiveBroadcast(broadcast: LiveBroadcast): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject =
                    broadcast.copy(
                        quality = getOptimalStreamQuality(),
                        isLive = true,
                        startTime = Date(),
                    ).toParseObject()

                parseObject.save()
                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun joinBroadcast(broadcastId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val query = ParseQuery.getQuery<ParseObject>("LiveBroadcast")
                val broadcastObj = query.get(broadcastId)

                val viewerCount = broadcastObj.getInt("viewerCount")
                broadcastObj.put("viewerCount", viewerCount + 1)

                // Update max viewers if necessary
                val maxViewers = broadcastObj.getInt("maxViewers")
                if (viewerCount + 1 > maxViewers) {
                    broadcastObj.put("maxViewers", viewerCount + 1)
                }

                broadcastObj.save()

                // Create viewer record
                val viewer = ParseObject("BroadcastViewer")
                viewer.put("broadcastId", broadcastId)
                viewer.put("userId", currentUser.objectId)
                viewer.put("userName", currentUser.username ?: "")
                viewer.put("joinTime", Date())
                viewer.put("quality", getOptimalStreamQuality().name)
                viewer.put("isActive", true)
                viewer.save()

                Result.success(true)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun getLiveBroadcasts(category: BroadcastCategory? = null): Result<List<LiveBroadcast>> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("LiveBroadcast")
                query.whereEqualTo("isLive", true)
                category?.let { query.whereEqualTo("category", it.name) }
                query.orderByDescending("viewerCount")

                val results = query.find()
                val broadcasts =
                    results.mapNotNull { parseObject ->
                        parseBroadcastFromParseObject(parseObject)
                    }
                Result.success(broadcasts)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Forum Management
    suspend fun createForum(forum: BreederForum): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = forum.toParseObject()
                parseObject.put("createdBy", ParseUser.getCurrentUser()?.objectId ?: "")
                parseObject.save()
                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun createForumThread(thread: ForumThread): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = thread.toParseObject()
                parseObject.save()

                // Update forum thread count
                updateForumStats(thread.forumId, threadIncrement = 1)

                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun replyToThread(post: ForumPost): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = ParseObject("ForumPost")
                parseObject.put("threadId", post.threadId)
                parseObject.put("authorId", post.authorId)
                parseObject.put("authorName", post.authorName)
                parseObject.put("content", post.content)
                parseObject.put("replyToPostId", post.replyToPostId)
                parseObject.put("isAnswer", post.isAnswer)
                parseObject.put("createdAt", Date())
                parseObject.put("upvotes", 0)
                parseObject.put("downvotes", 0)
                parseObject.put("score", 0)
                parseObject.save()

                // Update thread reply count
                updateThreadStats(post.threadId)

                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    suspend fun voteOnPost(
        postId: String,
        isUpvote: Boolean,
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("ForumPost")
                val postObj = query.get(postId)

                if (isUpvote) {
                    postObj.increment("upvotes")
                    postObj.increment("score")
                } else {
                    postObj.increment("downvotes")
                    postObj.increment("score", -1)
                }

                postObj.save()

                // Update user reputation
                val authorId = postObj.getString("authorId")
                if (authorId != null) {
                    updateUserReputation(authorId, if (isUpvote) 1 else -1)
                }

                Result.success(true)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Reputation System
    suspend fun getUserReputation(userId: String): Result<UserReputation> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>("UserReputation")
                query.whereEqualTo("userId", userId)

                val result: ParseObject? = query.first
                if (result != null) {
                    val reputation = parseReputationFromParseObject(result)
                    Result.success(reputation)
                } else {
                    // Create new reputation record
                    val newReputation = UserReputation(userId = userId)
                    createUserReputation(newReputation).fold(
                        onSuccess = { Result.success(newReputation) },
                        onFailure = { Result.failure(it) },
                    )
                }
            } catch (e: ParseException) {
                if (e.code == ParseException.OBJECT_NOT_FOUND) {
                    val newReputation = UserReputation(userId = userId)
                    createUserReputation(newReputation).fold(
                        onSuccess = { Result.success(newReputation) },
                        onFailure = { Result.failure(it) },
                    )
                } else {
                    Result.failure(e)
                }
            }
        }

    suspend fun endorseUser(
        userId: String,
        skill: String,
        comment: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser()
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val endorsement = ParseObject("Endorsement")
                endorsement.put("userId", userId)
                endorsement.put("endorserId", currentUser.objectId)
                endorsement.put("endorserName", currentUser.username ?: "")
                endorsement.put("skill", skill)
                endorsement.put("comment", comment)
                endorsement.put("createdAt", Date())
                endorsement.put("isVerified", false)
                endorsement.save()

                // Update user reputation
                updateUserReputation(userId, 5) // Endorsement worth 5 points

                Result.success(endorsement.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Media Optimization
    private fun compressImageAdvanced(
        uri: Uri,
        compressionLevel: ImageCompressionLevel,
    ): CompressedImage? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val quality = compressionLevel.quality
            val maxSize = compressionLevel.maxDimension

            // Resize if necessary
            val resizedBitmap =
                if (originalBitmap.width > maxSize || originalBitmap.height > maxSize) {
                    val ratio =
                        minOf(
                            maxSize.toFloat() / originalBitmap.width,
                            maxSize.toFloat() / originalBitmap.height,
                        )
                    val newWidth = (originalBitmap.width * ratio).toInt()
                    val newHeight = (originalBitmap.height * ratio).toInt()
                    Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                } else {
                    originalBitmap
                }

            // Compress main image
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val compressedData = outputStream.toByteArray()

            // Create thumbnail
            val thumbnailSize = 150
            val thumbnailBitmap =
                Bitmap.createScaledBitmap(resizedBitmap, thumbnailSize, thumbnailSize, true)
            val thumbnailStream = ByteArrayOutputStream()
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 70, thumbnailStream)
            val thumbnailData = thumbnailStream.toByteArray()

            // Calculate original size estimate
            val originalStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, originalStream)
            val originalSize = originalStream.size().toLong()

            // Clean up
            if (resizedBitmap != originalBitmap) resizedBitmap.recycle()
            originalBitmap.recycle()
            thumbnailBitmap.recycle()

            CompressedImage(
                data = compressedData,
                thumbnail = thumbnailData,
                originalSize = originalSize,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun compressAudio(
        audioData: ByteArray,
        compressionLevel: ImageCompressionLevel,
    ): ByteArray {
        // Simple audio compression simulation - in real implementation, use FFmpeg or similar
        val compressionRatio =
            when (compressionLevel) {
                ImageCompressionLevel.LOW -> 0.9f
                ImageCompressionLevel.MEDIUM -> 0.7f
                ImageCompressionLevel.HIGH -> 0.5f
                ImageCompressionLevel.ULTRA -> 0.3f
            }

        val targetSize = (audioData.size * compressionRatio).toInt()
        return if (targetSize < audioData.size) {
            audioData.copyOf(targetSize)
        } else {
            audioData
        }
    }

    private fun getOptimalStreamQuality(): StreamQuality {
        return when (getNetworkQuality()) {
            NetworkQualityLevel.EXCELLENT -> StreamQuality.HD
            NetworkQualityLevel.GOOD -> StreamQuality.HIGH
            NetworkQualityLevel.FAIR -> StreamQuality.MEDIUM
            else -> StreamQuality.LOW
        }
    }

    // Helper Functions
    private suspend fun updateChatActivity(chatId: String) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("GroupChat")
            val chat = query.get(chatId)
            chat.put("lastActivity", Date())
            chat.save()
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    private suspend fun updateForumStats(
        forumId: String,
        threadIncrement: Int = 0,
        postIncrement: Int = 0,
    ) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("BreederForum")
            val forum = query.get(forumId)
            if (threadIncrement != 0) forum.increment("threadCount", threadIncrement)
            if (postIncrement != 0) forum.increment("postCount", postIncrement)
            forum.put("lastActivity", Date())
            forum.save()
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    private suspend fun updateThreadStats(threadId: String) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("ForumThread")
            val thread = query.get(threadId)
            thread.increment("replies")
            thread.put("lastReplyAt", Date())
            thread.put("lastReplyBy", ParseUser.getCurrentUser()?.username ?: "")
            thread.save()
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    private suspend fun updateUserReputation(
        userId: String,
        points: Int,
    ) {
        try {
            val query = ParseQuery.getQuery<ParseObject>("UserReputation")
            query.whereEqualTo("userId", userId)

            var reputationObj: ParseObject? =
                try {
                    query.first
                } catch (e: ParseException) {
                    if (e.code == ParseException.OBJECT_NOT_FOUND) {
                        null
                    } else {
                        throw e
                    }
                }

            if (reputationObj == null) {
                reputationObj = ParseObject("UserReputation")
                reputationObj.put("userId", userId)
                reputationObj.put("overallScore", points)
            } else {
                reputationObj.increment("overallScore", points)
            }

            // Update level based on score
            val newScore = reputationObj.getInt("overallScore")
            val newLevel =
                when {
                    newScore >= 15000 -> ReputationLevel.GRANDMASTER
                    newScore >= 5000 -> ReputationLevel.MASTER
                    newScore >= 1500 -> ReputationLevel.EXPERT
                    newScore >= 500 -> ReputationLevel.PRACTITIONER
                    newScore >= 100 -> ReputationLevel.APPRENTICE
                    else -> ReputationLevel.NEWCOMER
                }

            reputationObj.put("level", newLevel.name)
            reputationObj.put("lastUpdated", Date())
            reputationObj.save()
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    private suspend fun createUserReputation(reputation: UserReputation): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parseObject = reputation.toParseObject()
                parseObject.save()
                Result.success(parseObject.objectId)
            } catch (e: ParseException) {
                Result.failure(e)
            }
        }

    // Parsing Functions
    private fun parseGroupChatFromParseObject(parseObject: ParseObject): GroupChat? {
        return try {
            GroupChat(
                id = parseObject.objectId,
                name = parseObject.getString("name") ?: "",
                description = parseObject.getString("description") ?: "",
                category = ChatCategory.valueOf(parseObject.getString("category") ?: "GENERAL"),
                adminId = parseObject.getString("adminId") ?: "",
                adminName = parseObject.getString("adminName") ?: "",
                memberIds =
                    parseObject.getList<String>("memberIds")?.filterIsInstance<String>()
                        ?: emptyList(),
                memberCount = parseObject.getInt("memberCount"),
                maxMembers = parseObject.getInt("maxMembers"),
                isPrivate = parseObject.getBoolean("isPrivate"),
                requiresApproval = parseObject.getBoolean("requiresApproval"),
                tags =
                    parseObject.getList<String>("tags")?.filterIsInstance<String>()
                        ?: emptyList(),
                region = parseObject.getString("region") ?: "",
                language = parseObject.getString("language") ?: "en",
                lastActivity = parseObject.getDate("lastActivity") ?: Date(),
                createdAt = parseObject.createdAt ?: Date(),
                rules =
                    parseObject.getList<String>("rules")?.filterIsInstance<String>()
                        ?: emptyList(),
                pinnedMessages =
                    parseObject.getList<String>("pinnedMessages")
                        ?.filterIsInstance<String>()
                        ?: emptyList(),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseChatMessageFromParseObject(parseObject: ParseObject): ChatMessage? {
        return try {
            ChatMessage(
                id = parseObject.objectId,
                chatId = parseObject.getString("chatId") ?: "",
                senderId = parseObject.getString("senderId") ?: "",
                senderName = parseObject.getString("senderName") ?: "",
                senderAvatar = parseObject.getString("senderAvatar") ?: "",
                messageType = MessageType.valueOf(parseObject.getString("messageType") ?: "TEXT"),
                content = parseObject.getString("content") ?: "",
                mediaUrl = parseObject.getString("mediaUrl") ?: "",
                mediaThumbnail = parseObject.getString("mediaThumbnail") ?: "",
                mediaSize = parseObject.getLong("mediaSize"),
                mediaDuration = parseObject.getInt("mediaDuration"),
                replyToMessageId = parseObject.getString("replyToMessageId") ?: "",
                timestamp = parseObject.getDate("timestamp") ?: Date(),
                editedAt = parseObject.getDate("editedAt"),
                isForwarded = parseObject.getBoolean("isForwarded"),
                forwardedFrom = parseObject.getString("forwardedFrom") ?: "",
                status = MessageStatus.valueOf(parseObject.getString("status") ?: "SENT"),
                priority = MessagePriority.valueOf(parseObject.getString("priority") ?: "NORMAL"),
                // Ensure metadata is parsed or a default is provided
                metadata =
                    parseObject.getParseObject("metadata")?.let { metaObj ->
                        MessageMetadata(
                            compressionUsed =
                                metaObj.getString("compressionUsed")
                                    ?.let { ImageCompressionLevel.valueOf(it) }
                                    ?: getOptimalCompressionLevel(),
                            originalSize = metaObj.getLong("originalSize"),
                            compressedSize = metaObj.getLong("compressedSize"),
                            deviceInfo = metaObj.getString("deviceInfo") ?: "",
                            transmissionTime = metaObj.getLong("transmissionTime"),
                        )
                    } ?: MessageMetadata(), // Default metadata if not found
            )
        } catch (e: Exception) {
            null // Log error in real app
        }
    }

    private fun parseBroadcastFromParseObject(parseObject: ParseObject): LiveBroadcast? {
        return try {
            LiveBroadcast(
                id = parseObject.objectId,
                broadcasterId = parseObject.getString("broadcasterId") ?: "",
                broadcasterName = parseObject.getString("broadcasterName") ?: "",
                broadcasterAvatar = parseObject.getString("broadcasterAvatar") ?: "",
                title = parseObject.getString("title") ?: "",
                description = parseObject.getString("description") ?: "",
                category =
                    BroadcastCategory.valueOf(
                        parseObject.getString("category") ?: "GENERAL",
                    ),
                tags =
                    parseObject.getList<String>("tags")?.filterIsInstance<String>()
                        ?: emptyList(),
                language = parseObject.getString("language") ?: "en",
                streamUrl = parseObject.getString("streamUrl") ?: "",
                thumbnailUrl = parseObject.getString("thumbnailUrl") ?: "",
                isLive = parseObject.getBoolean("isLive"),
                startTime = parseObject.getDate("startTime") ?: Date(),
                endTime = parseObject.getDate("endTime"),
                duration = parseObject.getInt("duration"),
                viewerCount = parseObject.getInt("viewerCount"),
                maxViewers = parseObject.getInt("maxViewers"),
                chatEnabled = parseObject.getBoolean("chatEnabled"),
                recordingEnabled = parseObject.getBoolean("recordingEnabled"),
                recordingUrl = parseObject.getString("recordingUrl") ?: "",
                quality = StreamQuality.valueOf(parseObject.getString("quality") ?: "MEDIUM"),
                moderators =
                    parseObject.getList<String>("moderators")?.filterIsInstance<String>()
                        ?: emptyList(),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseReputationFromParseObject(parseObject: ParseObject): UserReputation {
        return UserReputation(
            userId = parseObject.getString("userId") ?: "",
            overallScore = parseObject.getInt("overallScore"),
            level = ReputationLevel.valueOf(parseObject.getString("level") ?: "NEWCOMER"),
            lastUpdated = parseObject.getDate("lastUpdated") ?: Date(),
        )
    }
}

// Helper Data Classes
data class CompressedImage(
    val data: ByteArray,
    val thumbnail: ByteArray,
    val originalSize: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompressedImage

        if (!data.contentEquals(other.data)) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false
        if (originalSize != other.originalSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + thumbnail.contentHashCode()
        result = 31 * result + originalSize.hashCode()
        return result
    }
}
