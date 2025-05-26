package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import java.text.SimpleDateFormat
import java.util.*

data class FestivalNotification(
    val id: String,
    val title: String,
    val message: String,
    val festivalName: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val createdAt: Date,
    val actionUrl: String? = null
)

enum class NotificationType {
    FESTIVAL_REMINDER,
    MARKET_ALERT,
    UPLOAD_SUCCESS,
    TRANSFER_UPDATE,
    COMMUNITY_POST,
    SYSTEM_UPDATE
}

enum class NotificationPriority {
    HIGH, MEDIUM, LOW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    var notifications by remember { mutableStateOf(listOf<ParseObject>()) }
    var festivalNotifications by remember { mutableStateOf(listOf<FestivalNotification>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    LaunchedEffect(Unit) {
        // Fetch regular notifications
        fetchNotifications(
            onResult = { notifications = it },
            onError = { error = it },
            setLoading = { isLoading = it },
        )
        
        // Generate festival notifications
        festivalNotifications = generateFestivalNotifications()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header with notification count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📢 Notifications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            val totalCount = notifications.size + festivalNotifications.size
            if (totalCount > 0) {
                Badge {
                    Text("$totalCount")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Festival Notifications Section
                if (festivalNotifications.isNotEmpty()) {
                    item {
                        Text(
                            "🎉 Festival & Cultural Events",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    items(festivalNotifications) { notification ->
                        FestivalNotificationCard(
                            notification = notification,
                            dateFormatter = dateFormatter
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Regular Notifications Section
                if (notifications.isNotEmpty()) {
                    item {
                        Text(
                            "📱 General Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    items(notifications) { notification ->
                        RegularNotificationCard(
                            notification = notification,
                            dateFormatter = dateFormatter
                        )
                    }
                } else if (festivalNotifications.isEmpty()) {
                    item {
                        EmptyNotificationsCard()
                    }
                }
            }
        }

        error?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "⚠️ Error loading notifications: $it",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun FestivalNotificationCard(
    notification: FestivalNotification,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (notification.priority) {
                NotificationPriority.HIGH -> MaterialTheme.colorScheme.primaryContainer
                NotificationPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon based on notification type
            val (icon, iconColor) = when (notification.type) {
                NotificationType.FESTIVAL_REMINDER -> Icons.Filled.Celebration to MaterialTheme.colorScheme.primary
                NotificationType.MARKET_ALERT -> Icons.Filled.Store to MaterialTheme.colorScheme.secondary
                else -> Icons.Filled.Notifications to MaterialTheme.colorScheme.outline
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                // Festival badge
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        "🎊 ${notification.festivalName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    dateFormatter.format(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            // Priority indicator
            if (notification.priority == NotificationPriority.HIGH) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}

@Composable
fun RegularNotificationCard(
    notification: ParseObject,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.getString("title") ?: "Notification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    notification.getString("message") ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    notification.createdAt?.let { dateFormatter.format(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🔕",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "No notifications yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                "Festival alerts and app updates will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Generate festival notifications for demonstration
fun generateFestivalNotifications(): List<FestivalNotification> {
    val calendar = Calendar.getInstance()
    
    return listOf(
        FestivalNotification(
            id = "sankranti_2025",
            title = "Sankranti Festival Competition Registration Open! 🎊",
            message = "Register your roosters for the annual Sankranti competition. Prizes worth ₹50,000 await!",
            festivalName = "Makar Sankranti",
            type = NotificationType.FESTIVAL_REMINDER,
            priority = NotificationPriority.HIGH,
            createdAt = calendar.time
        ),
        FestivalNotification(
            id = "traditional_market_alert",
            title = "Traditional Market Tomorrow in Kondapelli",
            message = "Santa market opens at 6 AM. Pre-orders available until 10 PM today.",
            festivalName = "Weekly Market",
            type = NotificationType.MARKET_ALERT,
            priority = NotificationPriority.MEDIUM,
            createdAt = Calendar.getInstance().apply { add(Calendar.HOUR, -2) }.time
        ),
        FestivalNotification(
            id = "holi_group_buy",
            title = "Holi Festival Group Buying - 30% Discount!",
            message = "Join the community group buy for Holi celebrations. 50 participants needed for bulk discount.",
            festivalName = "Holi",
            type = NotificationType.FESTIVAL_REMINDER,
            priority = NotificationPriority.MEDIUM,
            createdAt = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
        )
    )
}
