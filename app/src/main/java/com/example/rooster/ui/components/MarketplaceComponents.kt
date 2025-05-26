package com.example.rooster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rooster.*
import com.parse.ParseObject
import kotlinx.coroutines.launch

// Key Marketplace Composables for UAT Demonstration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String> = listOf("Digital Market", "Traditional Markets", "Pre-Orders", "Group Buying", "Trends")
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth(),
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    when (index) {
                        0 -> Icon(Icons.Filled.Store, contentDescription = "Digital Market")
                        1 -> Icon(Icons.Filled.CalendarToday, contentDescription = "Traditional Markets") 
                        2 -> Icon(Icons.Filled.LocalOffer, contentDescription = "Pre-Orders")
                        3 -> Icon(Icons.Filled.Group, contentDescription = "Group Buying")
                        4 -> Icon(Icons.Filled.TrendingUp, contentDescription = "Trends")
                    }
                }
            )
        }
    }
}

@Composable
fun RuralOptimizedBiddingSection(
    listingId: String,
    networkQuality: NetworkQualityLevel,
    modifier: Modifier = Modifier
) {
    var bids by remember { mutableStateOf(listOf<ParseObject>()) }
    var newBidAmount by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Network-aware loading for 2G optimization
    LaunchedEffect(listingId, networkQuality) {
        if (networkQuality != NetworkQualityLevel.OFFLINE) {
            fetchBids(
                listingId = listingId,
                onResult = { bids = it },
                onError = { error = it },
                setLoading = { isLoading = it }
            )
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (networkQuality) {
                    NetworkQualityLevel.POOR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    NetworkQualityLevel.OFFLINE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💰 Bidding Section",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Network quality indicator for farmers
                    NetworkQualityChip(networkQuality = networkQuality)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (networkQuality == NetworkQualityLevel.OFFLINE) {
                    Text(
                        "📵 Offline mode - Bids will sync when connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    // Bidding interface optimized for farmers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newBidAmount,
                            onValueChange = { newBidAmount = it },
                            label = { Text("Your Bid (₹)") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && networkQuality != NetworkQualityLevel.OFFLINE,
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    // Submit bid with network-aware handling
                                    submitBid(
                                        listingId = listingId,
                                        amount = newBidAmount,
                                        networkQuality = networkQuality,
                                        onSuccess = { 
                                            newBidAmount = ""
                                            // Refresh bids
                                        },
                                        onError = { error = it }
                                    )
                                }
                            },
                            enabled = !isLoading && newBidAmount.isNotBlank(),
                            modifier = Modifier.height(56.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Bid")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current bids display
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading bids...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (bids.isEmpty()) {
                        Text(
                            "🔥 Be the first to bid!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "Current Bids (${bids.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        bids.take(3).forEach { bid -> // Show top 3 bids for 2G optimization
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    bid.getParseUser("user")?.username ?: "Anonymous",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "₹${bid.getString("amount") ?: "0"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        if (bids.size > 3) {
                            Text(
                                "+ ${bids.size - 3} more bids",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ $it",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkQualityChip(networkQuality: NetworkQualityLevel) {
    val (emoji, text, color) = when (networkQuality) {
        NetworkQualityLevel.EXCELLENT -> Triple("📶📶📶📶", "Excellent", MaterialTheme.colorScheme.primary)
        NetworkQualityLevel.GOOD -> Triple("📶📶📶", "Good", MaterialTheme.colorScheme.secondary) 
        NetworkQualityLevel.FAIR -> Triple("📶📶", "Fair", MaterialTheme.colorScheme.tertiary)
        NetworkQualityLevel.POOR -> Triple("📶", "Poor", MaterialTheme.colorScheme.error)
        NetworkQualityLevel.OFFLINE -> Triple("📵", "Offline", MaterialTheme.colorScheme.outline)
    }

    AssistChip(
        onClick = { /* Show network tips */ },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text, style = MaterialTheme.typography.labelSmall)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

// Helper functions for bidding operations
private suspend fun submitBid(
    listingId: String,
    amount: String,
    networkQuality: NetworkQualityLevel,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Implementation would handle network-aware bid submission
        // with appropriate timeouts based on network quality
        val timeout = when (networkQuality) {
            NetworkQualityLevel.POOR -> 30_000L // 30s for 2G
            NetworkQualityLevel.FAIR -> 15_000L
            else -> 10_000L
        }
        
        // Submit bid with timeout handling
        // This would be implemented with actual Parse operations
        onSuccess()
    } catch (e: Exception) {
        onError(e.message ?: "Failed to submit bid")
    }
}