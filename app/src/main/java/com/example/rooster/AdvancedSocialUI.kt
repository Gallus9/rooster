package com.example.rooster

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSocialScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val socialService = remember { AdvancedSocialService(context) }

    var groupChats by remember { mutableStateOf<List<GroupChat>>(emptyList()) }
    var liveBroadcasts by remember { mutableStateOf<List<LiveBroadcast>>(emptyList()) }
    var forums by remember { mutableStateOf<List<BreederForum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var networkQuality by remember { mutableStateOf("Good") }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true

            // Load group chats
            socialService.getGroupChats().onSuccess { chats ->
                groupChats = chats
            }

            // Load live broadcasts
            socialService.getLiveBroadcasts().onSuccess { broadcasts ->
                liveBroadcasts = broadcasts
            }

            isLoading = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFFF3E5F5),
                                Color(0xFFE8F5E8),
                            ),
                    ),
                ),
    ) {
        // Header with network quality indicator
        AdvancedSocialHeaderSection(networkQuality)

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            val tabs =
                listOf(
                    "💬 Group Chats",
                    "📡 Live Streams",
                    "🗣️ Forums",
                    "⭐ Reputation",
                    "📞 Calls",
                )

            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else {
            // Tab Content
            when (selectedTab) {
                0 -> GroupChatsSection(groupChats, socialService)
                1 -> LiveBroadcastsSection(liveBroadcasts, socialService)
                2 -> ForumsSection(forums, socialService)
                3 -> ReputationSection(socialService)
                4 -> CallsSection(socialService)
            }
        }
    }
}

@Composable
fun AdvancedSocialHeaderSection(networkQuality: String) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF673AB7).copy(alpha = 0.1f),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Advanced Social Hub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C),
                )

                // Network Quality Indicator
                NetworkQualityIndicator(networkQuality)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect, Share, Learn - Optimized for Rural Networks",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF6A1B9A),
            )
        }
    }
}

@Composable
fun NetworkQualityIndicator(quality: String) {
    val (color, icon) =
        when (quality.lowercase()) {
            "excellent" -> Color(0xFF4CAF50) to Icons.Default.Wifi
            "good" -> Color(0xFF8BC34A) to Icons.Default.NetworkWifi
            "fair" -> Color(0xFFFF9800) to Icons.Default.NetworkWifi1Bar
            "poor" -> Color(0xFFF44336) to Icons.Default.NetworkWifi1Bar
            else -> Color(0xFF9E9E9E) to Icons.Default.WifiOff
        }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = quality,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun GroupChatsSection(
    groupChats: List<GroupChat>,
    socialService: AdvancedSocialService,
) {
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // Create Group Button
            ElevatedButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF4CAF50),
                    ),
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Group Chat", color = Color.White)
            }
        }

        items(groupChats) { chat ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
            ) {
                GroupChatCard(chat, socialService)
            }
        }

        if (groupChats.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Groups,
                    title = "No Group Chats Yet",
                    description = "Create or join group chats to collaborate with other breeders",
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupChatDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { groupChat ->
                scope.launch {
                    socialService.createGroupChat(groupChat)
                    showCreateDialog = false
                }
            },
        )
    }
}

@Composable
fun GroupChatCard(
    chat: GroupChat,
    socialService: AdvancedSocialService,
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        socialService.joinGroupChat(chat.id)
                    }
                },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category Icon
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(chat.category).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        getCategoryIcon(chat.category),
                        contentDescription = null,
                        tint = getCategoryColor(chat.category),
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = chat.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF757575),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${chat.memberCount}/${chat.maxMembers} members",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF757575),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    ChatCategoryChip(chat.category)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (chat.isPrivate) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Private",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (chat.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(chat.tags.take(3)) { tag ->
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Last active: ${formatTimeAgo(chat.lastActivity)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF757575),
                )

                TextButton(
                    onClick = {
                        // Navigate to chat
                    },
                ) {
                    Text("Join Chat")
                }
            }
        }
    }
}

@Composable
fun LiveBroadcastsSection(
    broadcasts: List<LiveBroadcast>,
    socialService: AdvancedSocialService,
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // Start Broadcast Button
            ElevatedButton(
                onClick = {
                    // Start broadcast
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFE91E63),
                    ),
            ) {
                Icon(Icons.Default.VideoCall, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Live Broadcast", color = Color.White)
            }
        }

        items(broadcasts) { broadcast ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
            ) {
                LiveBroadcastCard(broadcast, socialService)
            }
        }

        if (broadcasts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.LiveTv,
                    title = "No Live Broadcasts",
                    description = "Start a broadcast to share knowledge with the community",
                )
            }
        }
    }
}

@Composable
fun LiveBroadcastCard(
    broadcast: LiveBroadcast,
    socialService: AdvancedSocialService,
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        socialService.joinBroadcast(broadcast.id)
                    }
                },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            // Thumbnail with live indicator
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
            ) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(broadcast.thumbnailUrl.ifEmpty { R.drawable.ic_launcher_foreground })
                            .build(),
                    contentDescription = broadcast.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Live indicator
                if (broadcast.isLive) {
                    Surface(
                        modifier =
                            Modifier
                                .padding(12.dp)
                                .align(Alignment.TopStart),
                        color = Color.Red,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(6.dp)
                                        .background(Color.White, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Viewer count
                Surface(
                    modifier =
                        Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${broadcast.viewerCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Broadcaster avatar
                    AsyncImage(
                        model =
                            ImageRequest.Builder(LocalContext.current)
                                .data(broadcast.broadcasterAvatar.ifEmpty { R.drawable.ic_launcher_foreground })
                                .build(),
                        contentDescription = broadcast.broadcasterName,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = broadcast.broadcasterName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2E7D32),
                        )
                        Text(
                            text = formatTimeAgo(broadcast.startTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF757575),
                        )
                    }

                    BroadcastCategoryChip(broadcast.category)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = broadcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (broadcast.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = broadcast.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StreamQualityIndicator(broadcast.quality)

                    ElevatedButton(
                        onClick = {
                            scope.launch {
                                socialService.joinBroadcast(broadcast.id)
                            }
                        },
                        colors =
                            ButtonDefaults.elevatedButtonColors(
                                containerColor =
                                    if (broadcast.isLive) {
                                        Color(0xFFE91E63)
                                    } else {
                                        Color(
                                            0xFF757575,
                                        )
                                    },
                            ),
                    ) {
                        Icon(
                            if (broadcast.isLive) Icons.Default.PlayArrow else Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (broadcast.isLive) "Watch" else "Replay",
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForumsSection(
    forums: List<BreederForum>,
    socialService: AdvancedSocialService,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Breeder Forums",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        items(forums) { forum ->
            ForumCard(forum, socialService)
        }

        if (forums.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Forum,
                    title = "No Forums Available",
                    description = "Forums will appear here when created by community moderators",
                )
            }
        }
    }
}

@Composable
fun ReputationSection(socialService: AdvancedSocialService) {
    // Reputation and expertise display
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = "Your Reputation & Expertise",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEB3B).copy(alpha = 0.1f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFFF9800),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reputation System",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Build your reputation by helping others and sharing knowledge",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF757575),
                )
            }
        }
    }
}

@Composable
fun CallsSection(socialService: AdvancedSocialService) {
    // Voice/Video calling interface
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = "Voice & Video Calls",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFF2196F3).copy(alpha = 0.1f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.VideoCall,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF2196F3),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Consultation Calls",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Connect with expert breeders for personalized advice",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF757575),
                )
            }
        }
    }
}

// Helper Composables
@Composable
fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF9E9E9E),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF757575),
            )
        }
    }
}

@Composable
fun ChatCategoryChip(category: ChatCategory) {
    val (color, text) =
        when (category) {
            ChatCategory.BREEDING -> Color(0xFF4CAF50) to "Breeding"
            ChatCategory.HEALTH_CARE -> Color(0xFFE91E63) to "Health"
            ChatCategory.COMPETITIONS -> Color(0xFFFF9800) to "Competitions"
            ChatCategory.MARKETPLACE -> Color(0xFF2196F3) to "Market"
            ChatCategory.CULTURAL_EVENTS -> Color(0xFF9C27B0) to "Cultural"
            ChatCategory.BEGINNERS -> Color(0xFF00BCD4) to "Beginners"
            ChatCategory.REGIONAL -> Color(0xFF795548) to "Regional"
            else -> Color(0xFF757575) to "General"
        }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun BroadcastCategoryChip(category: BroadcastCategory) {
    val (color, text) =
        when (category) {
            BroadcastCategory.SHOWCASE -> Color(0xFF4CAF50) to "Showcase"
            BroadcastCategory.EDUCATION -> Color(0xFF2196F3) to "Education"
            BroadcastCategory.BREEDING_TIPS -> Color(0xFFFF9800) to "Tips"
            BroadcastCategory.HEALTH_CONSULTATION -> Color(0xFFE91E63) to "Health"
            BroadcastCategory.COMPETITION -> Color(0xFF9C27B0) to "Competition"
            BroadcastCategory.CULTURAL_EVENT -> Color(0xFF795548) to "Cultural"
            BroadcastCategory.Q_AND_A -> Color(0xFF00BCD4) to "Q&A"
            else -> Color(0xFF757575) to "General"
        }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun StreamQualityIndicator(quality: StreamQuality) {
    val (color, text) =
        when (quality) {
            StreamQuality.HD -> Color(0xFF4CAF50) to "HD"
            StreamQuality.HIGH -> Color(0xFF8BC34A) to "720p"
            StreamQuality.MEDIUM -> Color(0xFFFF9800) to "480p"
            StreamQuality.LOW -> Color(0xFFF44336) to "240p"
        }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

// Dialog for creating group chats
@Composable
fun CreateGroupChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (GroupChat) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ChatCategory.GENERAL) }
    var isPrivate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group Chat") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Private group")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            GroupChat(
                                name = name,
                                description = description,
                                category = selectedCategory,
                                isPrivate = isPrivate,
                                requiresApproval = isPrivate,
                            ),
                        )
                    }
                },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// Utility Functions
private fun getCategoryColor(category: ChatCategory): Color {
    return when (category) {
        ChatCategory.BREEDING -> Color(0xFF4CAF50)
        ChatCategory.HEALTH_CARE -> Color(0xFFE91E63)
        ChatCategory.COMPETITIONS -> Color(0xFFFF9800)
        ChatCategory.MARKETPLACE -> Color(0xFF2196F3)
        ChatCategory.CULTURAL_EVENTS -> Color(0xFF9C27B0)
        ChatCategory.BEGINNERS -> Color(0xFF00BCD4)
        ChatCategory.REGIONAL -> Color(0xFF795548)
        else -> Color(0xFF757575)
    }
}

private fun getCategoryIcon(category: ChatCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        ChatCategory.BREEDING -> Icons.Default.Pets
        ChatCategory.HEALTH_CARE -> Icons.Default.HealthAndSafety
        ChatCategory.COMPETITIONS -> Icons.Default.EmojiEvents
        ChatCategory.MARKETPLACE -> Icons.Default.Store
        ChatCategory.CULTURAL_EVENTS -> Icons.Default.Celebration
        ChatCategory.BEGINNERS -> Icons.Default.School
        ChatCategory.REGIONAL -> Icons.Default.LocationOn
        else -> Icons.Default.Groups
    }
}

@Composable
fun ForumCard(
    forum: BreederForum,
    socialService: AdvancedSocialService,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF2196F3),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = forum.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = forum.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${forum.threadCount} threads • ${forum.postCount} posts",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF757575),
                )

                TextButton(
                    onClick = { /* Navigate to forum */ },
                ) {
                    Text("Visit Forum")
                }
            }
        }
    }
}

private fun formatTimeAgo(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInMinutes = diffInMillis / (1000 * 60)

    return when {
        diffInMinutes < 1 -> "just now"
        diffInMinutes < 60 -> "${diffInMinutes}m ago"
        diffInMinutes < 1440 -> "${diffInMinutes / 60}h ago"
        else -> "${diffInMinutes / 1440}d ago"
    }
}
