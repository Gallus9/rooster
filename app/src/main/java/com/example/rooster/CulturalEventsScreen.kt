package com.example.rooster

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulturalEventsScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val culturalEventService = remember { CulturalEventService() }

    var festivals by remember { mutableStateOf<List<Festival>>(emptyList()) }
    var groupOrders by remember { mutableStateOf<List<GroupOrder>>(emptyList()) }
    var competitions by remember { mutableStateOf<List<FestivalCompetition>>(emptyList()) }
    var showcases by remember { mutableStateOf<List<RoosterShowcase>>(emptyList()) }
    var culturalContent by remember { mutableStateOf<List<MultimediaContent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Get current language preference
    var currentLanguage by remember { mutableStateOf("en") }
    val context = LocalContext.current

    LaunchedEffect(currentLanguage) {
        scope.launch {
            isLoading = true

            // Load festivals
            culturalEventService.getUpcomingFestivals(currentLanguage).onSuccess { festivalList ->
                festivals = festivalList
            }

            // Load group orders
            culturalEventService.getActiveGroupOrders().onSuccess { orderList ->
                groupOrders = orderList
            }

            // Load cultural content
            culturalEventService.getApprovedCulturalContent(language = currentLanguage)
                .onSuccess { contentList ->
                    culturalContent = contentList
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
                                Color(0xFFFFF8E1), // Light saffron
                                Color(0xFFE8F5E8), // Light green
                            ),
                    ),
                ),
    ) {
        // Header with cultural theme
        CulturalEventHeaderSection(
            currentLanguage = currentLanguage,
            onLanguageChange = { currentLanguage = it },
        )

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            val tabs =
                listOf(
                    "🗓️ Festivals",
                    "🛒 Group Orders",
                    "🏆 Competitions",
                    "🌟 Showcases",
                    "📚 Culture",
                )

            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
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
                0 -> FestivalCalendarSection(festivals, currentLanguage, culturalEventService)
                1 -> GroupOrdersSection(groupOrders, culturalEventService)
                2 -> CompetitionsSection(competitions, currentLanguage, culturalEventService)
                3 -> ShowcasesSection(showcases, culturalEventService)
                4 -> CulturalContentSection(culturalContent, currentLanguage, culturalEventService)
            }
        }
    }
}

@Composable
fun CulturalEventHeaderSection(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFFF5722).copy(alpha = 0.1f),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    when (currentLanguage) {
                        "te" -> "సాంస్కృతిక వేడుకలు"
                        "ta" -> "பண்பாட்டு விழாக்கள்"
                        "kn" -> "ಸಾಂಸ್ಕೃತಿಕ ಉತ್ಸವಗಳು"
                        "hi" -> "सांस्कृतिक उत्सव"
                        else -> "Cultural Festivals"
                    },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD84315),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text =
                    when (currentLanguage) {
                        "te" -> "మన సంప్రదాయాలను జీవిత చేయండి"
                        "ta" -> "நமது பாரம்பர்யங்களை வாழுங்கள்"
                        "kn" -> "ನಮ್ಮ ಸಂಪ್ರದಾಯಗಳನ್ನು ಬದುಕಿಸಿ"
                        "hi" -> "हमारी परंपराओं को जिवंत रखें"
                        else -> "Celebrate & Preserve Our Heritage"
                    },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color(0xFF5D4037),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Language Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                val languages =
                    listOf(
                        "en" to "English",
                        "te" to "తెలుగు",
                        "ta" to "தமிழ்",
                        "kn" to "ಕನ್ನಡ",
                        "hi" to "हिंदी",
                    )

                items(languages) { (code, name) ->
                    FilterChip(
                        onClick = { onLanguageChange(code) },
                        label = { Text(name, fontSize = 12.sp) },
                        selected = currentLanguage == code,
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CAF50),
                                selectedLabelColor = Color.White,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun FestivalCalendarSection(
    festivals: List<Festival>,
    language: String,
    culturalEventService: CulturalEventService,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(festivals) { festival ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
            ) {
                FestivalCard(festival, language, culturalEventService)
            }
        }
    }
}

@Composable
fun FestivalCard(
    festival: Festival,
    language: String,
    culturalEventService: CulturalEventService,
) {
    val scope = rememberCoroutineScope()
    var showDetails by remember { mutableStateOf(false) }
    var competitions by remember { mutableStateOf<List<FestivalCompetition>>(emptyList()) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { showDetails = !showDetails },
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Festival Image or Icon
                Box(
                    modifier =
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5722).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (festival.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model =
                                ImageRequest.Builder(LocalContext.current)
                                    .data(festival.imageUrl)
                                    .build(),
                            contentDescription = festival.name,
                            modifier =
                                Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Default.Celebration,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color(0xFFD84315),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = festival.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                    )

                    Text(
                        text =
                            SimpleDateFormat(
                                "MMM dd, yyyy",
                                Locale.getDefault(),
                            ).format(festival.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5D4037),
                    )

                    if (festival.duration > 1) {
                        Text(
                            text = "${festival.duration} days celebration",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575),
                        )
                    }
                }

                Icon(
                    if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                )
            }

            if (showDetails) {
                Spacer(modifier = Modifier.height(12.dp))

                // Cultural Significance
                Text(
                    text = festival.significance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242),
                )

                if (festival.traditions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Traditions:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                    )

                    festival.traditions.forEach { tradition ->
                        Text(
                            text = "• $tradition",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                if (festival.roosterCompetitions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                culturalEventService.getFestivalCompetitions(festival.id, language)
                                    .onSuccess { competitionList ->
                                        competitions = competitionList
                                    }
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Competitions")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupOrdersSection(
    groupOrders: List<GroupOrder>,
    culturalEventService: CulturalEventService,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF2E7D32),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Festival Group Buying",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Join community orders for better prices during festivals",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF5D4037),
                    )
                }
            }
        }

        items(groupOrders) { order ->
            GroupOrderCard(order, culturalEventService)
        }
    }
}

@Composable
fun GroupOrderCard(
    order: GroupOrder,
    culturalEventService: CulturalEventService,
) {
    val scope = rememberCoroutineScope()
    val progress =
        if (order.targetQuantity > 0) {
            order.currentQuantity.toFloat() / order.targetQuantity.toFloat()
        } else {
            0f
        }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = order.itemType,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${order.discountedPrice}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                    )
                    Text(
                        text = "₹${order.unitPrice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.description,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (order.culturalSignificance.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cultural Context: ${order.culturalSignificance}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD84315),
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${order.currentQuantity}/${order.targetQuantity} joined",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFFE0E0E0),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Deadline: ${
                        SimpleDateFormat(
                            "MMM dd",
                            Locale.getDefault(),
                        ).format(order.deadline)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A4400),
                )

                Button(
                    onClick = {
                        scope.launch {
                            culturalEventService.joinGroupOrder(order.id, 1)
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                        ),
                ) {
                    Text("Join Order")
                }
            }
        }
    }
}

@Composable
fun CompetitionsSection(
    competitions: List<FestivalCompetition>,
    language: String,
    culturalEventService: CulturalEventService,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(competitions) { competition ->
            CompetitionCard(competition, language, culturalEventService)
        }
    }
}

@Composable
fun CompetitionCard(
    competition: FestivalCompetition,
    language: String,
    culturalEventService: CulturalEventService,
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
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFFFFC107),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = competition.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = competition.category.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                    )
                }

                StatusChip(competition.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = competition.description,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (competition.traditionalSignificance.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Traditional Significance: ${competition.traditionalSignificance}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD84315),
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Registration Fee",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF757575),
                    )
                    Text(
                        text = "₹${competition.entryFee}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                    )
                }

                Column {
                    Text(
                        text = "Participants",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF757575),
                    )
                    Text(
                        text = "${competition.currentParticipants}/${competition.maxParticipants}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: CompetitionStatus) {
    val (color, text) =
        when (status) {
            CompetitionStatus.REGISTRATION_OPEN -> Color(0xFF4CAF50) to "Open"
            CompetitionStatus.REGISTRATION_CLOSED -> Color(0xFFFF9800) to "Closed"
            CompetitionStatus.IN_PROGRESS -> Color(0xFF2196F3) to "In Progress"
            CompetitionStatus.COMPLETED -> Color(0xFF9C27B0) to "Completed"
            CompetitionStatus.CANCELLED -> Color(0xFFF44336) to "Cancelled"
            else -> Color(0xFF757575) to "Upcoming"
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
fun ShowcasesSection(
    showcases: List<RoosterShowcase>,
    culturalEventService: CulturalEventService,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(showcases) { showcase ->
            ShowcaseCard(showcase, culturalEventService)
        }
    }
}

@Composable
fun ShowcaseCard(
    showcase: RoosterShowcase,
    culturalEventService: CulturalEventService,
) {
    val scope = rememberCoroutineScope()

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = showcase.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Rooster: ${showcase.roosterName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                    )
                    Text(
                        text = "Owner: ${showcase.ownerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = showcase.likes.toString(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (showcase.isVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2196F3),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = showcase.description,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (showcase.culturalStory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Story: ${showcase.culturalStory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD84315),
                    fontWeight = FontWeight.Medium,
                )
            }

            if (showcase.traditionalLineage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lineage: ${showcase.traditionalLineage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D4037),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            culturalEventService.likeShowcase(showcase.id)
                        }
                    },
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Like")
                }

                TextButton(
                    onClick = { /* Navigate to showcase details */ },
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Details")
                }
            }
        }
    }
}

@Composable
fun CulturalContentSection(
    culturalContent: List<MultimediaContent>,
    language: String,
    culturalEventService: CulturalEventService,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFFD84315).copy(alpha = 0.1f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFD84315),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cultural Heritage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Preserving traditions through community sharing",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF5D4037),
                    )
                }
            }
        }

        items(culturalContent) { content ->
            CulturalContentCard(content, language)
        }
    }
}

@Composable
fun CulturalContentCard(
    content: MultimediaContent,
    language: String,
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
                    when (content.type) {
                        ContentType.VIDEO -> Icons.Default.PlayCircle
                        ContentType.AUDIO -> Icons.Default.AudioFile
                        ContentType.DOCUMENT -> Icons.Default.Description
                        ContentType.STORY -> Icons.Default.MenuBook
                        ContentType.RECIPE -> Icons.Default.Restaurant
                        ContentType.TRADITION -> Icons.Default.History
                        else -> Icons.Default.Image
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFFD84315),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = content.type.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = content.likes.toString(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (content.culturalContext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cultural Context: ${content.culturalContext}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD84315),
                    fontWeight = FontWeight.Medium,
                )
            }

            if (content.traditionalKnowledge.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Traditional Knowledge: ${content.traditionalKnowledge}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D4037),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags
            if (content.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(content.tags) { tag ->
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
        }
    }
}
