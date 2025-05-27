package com.example.rooster

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* 
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen() {
    var currentTab by remember { mutableStateOf(0) }
    val tabs =
        listOf("Digital Market", "Traditional Markets", "Pre-Orders", "Group Buying", "Trends")

    Column(modifier = Modifier.fillMaxSize()) {
        com.example.rooster.ui.components.MarketplaceTabRow(
            selectedTabIndex = currentTab,
            onTabSelected = { currentTab = it },
            tabs = tabs
        )

        // Tab Content
        when (currentTab) {
            0 -> DigitalMarketplaceTab()
            1 -> TraditionalMarketsTab()
            2 -> PreOrdersTab()
            3 -> GroupBuyingTab()
            4 -> MarketTrendsTab()
        }
    }
}

@Composable
fun DigitalMarketplaceTab() {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var listings by remember { mutableStateOf(listOf<ParseObject>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var digitalEvents by remember { mutableStateOf(listOf<DigitalMarketEvent>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val marketService = remember { TraditionalMarketService() }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }
    val context = LocalContext.current

    fun fetchListings() {
        loading = true
        val query = ParseQuery.getQuery<ParseObject>("Listing")
        query.orderByDescending("createdAt")
        query.findInBackground { result, e ->
            loading = false
            if (e == null && result != null) {
                listings = result
            } else {
                error = e?.localizedMessage ?: "Failed to load listings."
            }
        }
    }

    fun addListing() {
        val currentUser = ParseUser.getCurrentUser()
        if (currentUser == null) {
            error = "User not logged in. Please log in again."
            coroutineScope.launch { snackbarHostState.showSnackbar(error) }
            return
        }
    
        if (title.isBlank() || price.isBlank()) {
            error = "Title and Price cannot be empty."
            coroutineScope.launch { snackbarHostState.showSnackbar(error) }
            return
        }
    
        loading = true
    
        val listing = ParseObject("Listing")
        listing.put("title", title)
        try {
            listing.put("price", price.toDouble())
        } catch (e: NumberFormatException) {
            error = "Invalid price format. Please enter a number."
            loading = false
            coroutineScope.launch { snackbarHostState.showSnackbar(error) }
            return
        }
        listing.put("owner", currentUser)
    
        if (imageUri != null) {
            try {
                context.contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap == null) {
                        error = "Failed to decode image. Please try a different image."
                        loading = false
                        coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                        return@addListing
                    }
    
                    val compressedBytes = compressImage(bitmap)
                    if (compressedBytes == null) {
                        error = "Failed to compress image."
                        loading = false
                        coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                        return@addListing
                    }
    
                    if (compressedBytes.size > 10 * 1024 * 1024) {
                         error = "Image is too large even after compression (max 10MB)."
                         loading = false
                         coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                         return@addListing
                    }
    
                    val parseFile = ParseFile("listing_image.jpg", compressedBytes)
                    parseFile.saveInBackground { e: com.parse.ParseException? ->
                        if (e == null) {
                            listing.put("image", parseFile)
                            listing.saveInBackground { e2: com.parse.ParseException? ->
                                loading = false
                                if (e2 == null) {
                                    title = ""
                                    price = ""
                                    imageUri = null
                                    fetchListings()
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Listing added successfully!") }
                                } else {
                                    error = e2.localizedMessage ?: "Failed to save listing details."
                                    FirebaseCrashlytics.getInstance().recordException(e2)
                                    coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                                }
                            }
                        } else {
                            loading = false
                            error = e.localizedMessage ?: "Failed to upload image."
                            FirebaseCrashlytics.getInstance().recordException(e)
                            coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                        }
                    }
                } ?: run {
                    error = "Failed to open image stream. Please select image again."
                    loading = false
                    coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                }
            } catch (e: Exception) {
                loading = false
                error = "An error occurred with the image: ${e.localizedMessage ?: "Unknown image error"}"
                FirebaseCrashlytics.getInstance().recordException(e)
                coroutineScope.launch { snackbarHostState.showSnackbar(error) }
            }
        } else {
            listing.saveInBackground { e: com.parse.ParseException? ->
                loading = false
                if (e == null) {
                    title = ""
                    price = ""
                    fetchListings()
                    coroutineScope.launch { snackbarHostState.showSnackbar("Listing added successfully (no image)!") }
                } else {
                    error = e.localizedMessage ?: "Failed to add listing without image."
                    FirebaseCrashlytics.getInstance().recordException(e)
                    coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchListings()
        // Fetch active digital market events
        marketService.fetchActiveDigitalMarketEvents(
            onResult = { digitalEvents = it },
            onError = { error = it ?: "Failed to load digital events" },
            setLoading = { },
        )
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Digital Market Events Section
        if (digitalEvents.isNotEmpty()) {
            item {
                Text(
                    "🔄 Active Digital Market Events",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(digitalEvents) { event ->
                        DigitalEventCard(event = event)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Create Listing Section
        item {
            Text("Create Listing", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Pick Image")
                }
                imageUri?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = it,
                        contentDescription = "Selected image",
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { addListing() }, enabled = title.isNotBlank() && price.isNotBlank()) {
                Text("Add Listing")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Listings Section
        if (loading) {
            item { CircularProgressIndicator() }
        } else if (error.isNotEmpty()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        } else {
            items(listings) { listing ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Title: ${listing.getString("title")}")
                            Text("Price: ${listing.getString("price")}")
                            Text("Seller: ${listing.getParseUser("owner")?.username ?: "Unknown"}")
                            val imageUrl = listing.getParseFile("image")?.url
                            imageUrl?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = "Listing image",
                                    modifier =
                                        Modifier
                                            .height(120.dp)
                                            .fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            // Bidding Section
                            BiddingSection(listingId = listing.objectId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TraditionalMarketsTab() {
    var markets by remember { mutableStateOf(listOf<TraditionalMarket>()) }
    var marketCalendar by remember { mutableStateOf(listOf<MarketCalendarEntry>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("") }

    val marketService = remember { TraditionalMarketService() }
    val regions = listOf("All Regions", "Telangana", "Andhra Pradesh", "Karnataka", "Tamil Nadu")

    LaunchedEffect(selectedRegion) {
        marketService.fetchTraditionalMarkets(
            region = if (selectedRegion == "All Regions") "" else selectedRegion,
            onResult = { markets = it },
            onError = { error = it ?: "Failed to load markets" },
            setLoading = { loading = it },
        )

        // Fetch next 30 days market calendar
        val calendar = Calendar.getInstance()
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val endDate = calendar.time

        marketService.fetchMarketCalendar(
            startDate = startDate,
            endDate = endDate,
            onResult = { marketCalendar = it },
            onError = { },
            setLoading = { },
        )
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = "Traditional Markets",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "🏪 Traditional Santa/Bajar Markets",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Region Filter
        item {
            Text(
                "Select Region:",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(regions) { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = { selectedRegion = region },
                        label = { Text(region) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Upcoming Market Calendar
        if (marketCalendar.isNotEmpty()) {
            item {
                Text(
                    "📅 Upcoming Market Days",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(marketCalendar.take(10)) { entry ->
                MarketCalendarCard(entry = entry)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Traditional Markets List
        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (error.isNotEmpty()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        } else {
            item {
                Text(
                    "🏪 Registered Markets (${markets.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(markets) { market ->
                TraditionalMarketCard(market = market)
            }
        }
    }
}

@Composable
fun PreOrdersTab() {
    var preOrders by remember { mutableStateOf(listOf<PreMarketOrder>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val marketService = remember { TraditionalMarketService() }

    LaunchedEffect(Unit) {
        marketService.fetchPreMarketOrders(
            onResult = { preOrders = it },
            onError = { error = it ?: "Failed to load pre-orders" },
            setLoading = { loading = it },
        )
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalOffer,
                        contentDescription = "Pre-Orders",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "📦 Pre-Market Orders",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(onClick = { showCreateDialog = true }) {
                    Text("Create Pre-Order")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (loading) {
            item { CircularProgressIndicator() }
        } else if (error.isNotEmpty()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        } else if (preOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "🛒 No Pre-Orders Available",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Secure your poultry purchases before market day. Create a pre-order to reserve birds from trusted sellers.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(preOrders) { order ->
                PreOrderCard(order = order)
            }
        }
    }

    if (showCreateDialog) {
        CreatePreOrderDialog(
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                // Refresh pre-orders
                marketService.fetchPreMarketOrders(
                    onResult = { preOrders = it },
                    onError = { error = it ?: "Failed to load pre-orders" },
                    setLoading = { loading = it },
                )
            },
        )
    }
}

@Composable
fun GroupBuyingTab() {
    var groupRequests by remember { mutableStateOf(listOf<GroupBuyingRequest>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val marketService = remember { TraditionalMarketService() }

    LaunchedEffect(Unit) {
        marketService.fetchGroupBuyingRequests(
            onResult = { groupRequests = it },
            onError = { error = it ?: "Failed to load group buying requests" },
            setLoading = { loading = it },
        )
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Group,
                        contentDescription = "Group Buying",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "👥 Group Buying",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(onClick = { showCreateDialog = true }) {
                    Text("Start Group Buy")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text(
                    "💡 Coordinate with other buyers to get better prices through bulk purchasing. Perfect for festival seasons and community events!",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (loading) {
            item { CircularProgressIndicator() }
        } else if (error.isNotEmpty()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        } else if (groupRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "👥 No Active Group Buys",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start or join group buying requests to get better prices through collective purchasing power.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(groupRequests) { request ->
                GroupBuyingCard(request = request)
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupBuyDialog(
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                // Refresh group buying requests
                marketService.fetchGroupBuyingRequests(
                    onResult = { groupRequests = it },
                    onError = { error = it ?: "Failed to load group buying requests" },
                    setLoading = { loading = it },
                )
            },
        )
    }
}

@Composable
fun MarketTrendsTab() {
    var trends by remember { mutableStateOf(listOf<MarketTrend>()) }
    var prediction by remember { mutableStateOf<PricePrediction?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selectedMarket by remember { mutableStateOf("") }
    var selectedFowlType by remember { mutableStateOf("Rooster") }
    var selectedBreed by remember { mutableStateOf("") }

    val marketService = remember { TraditionalMarketService() }
    val fowlTypes = listOf("Rooster", "Hen", "Chick", "Fighting Cock")
    val breeds = listOf("Aseel", "Brahma", "Kadaknath", "Country Chicken", "Hybrid")

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = "Market Trends",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "📈 Market Trends & Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filters
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Filter Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Fowl Type:", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(fowlTypes) { type ->
                            FilterChip(
                                selected = selectedFowlType == type,
                                onClick = { selectedFowlType = type },
                                label = { Text(type) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Breed:", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(breeds) { breed ->
                            FilterChip(
                                selected = selectedBreed == breed,
                                onClick = { selectedBreed = breed },
                                label = { Text(breed) },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Price Prediction Card
        prediction?.let { pred ->
            item {
                PricePredictionCard(prediction = pred)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Market Trends
        if (loading) {
            item { CircularProgressIndicator() }
        } else if (error.isNotEmpty()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        } else if (trends.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        "📊 Market trend data will appear here once available. Historical pricing, demand patterns, and seasonal variations help farmers make informed selling decisions.",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(trends) { trend ->
                MarketTrendCard(trend = trend)
            }
        }
    }
}

// UI Component Cards for Traditional Market Features

@Composable
fun DigitalEventCard(event: DigitalMarketEvent) {
    Card(
        modifier =
            Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🔄 Emergency Digital Market",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Reason: ${event.cancellationReason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                "Duration: ${event.eventDuration} hours",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
fun MarketCalendarCard(entry: MarketCalendarEntry) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.marketName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${dateFormatter.format(entry.date)} • ${entry.dayOfWeek}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    entry.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.specialties.isNotEmpty()) {
                    Text(
                        "Specialties: ${entry.specialties.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.marketType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (entry.culturalEvents.isNotEmpty()) {
                    Text(
                        "🎉 Festival",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun TraditionalMarketCard(market: TraditionalMarket) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        market.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        market.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (market.address.isNotEmpty()) {
                        Text(
                            market.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(
                            market.marketType.name.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (market.marketDays.isNotEmpty()) {
                Text(
                    "Days: ${market.marketDays.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (market.startTime.isNotEmpty() && market.endTime.isNotEmpty()) {
                Text(
                    "Time: ${market.startTime} - ${market.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (market.specialties.isNotEmpty()) {
                Text(
                    "Specialties: ${market.specialties.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (market.culturalSignificance.isNotEmpty()) {
                Text(
                    "Cultural: ${market.culturalSignificance}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun PreOrderCard(order: PreMarketOrder) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${order.fowlType} - ${order.breed}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "₹${order.pricePerBird}/bird",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Quantity Available: ${order.quantity - order.reservedQuantity}/${order.quantity}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Market Date: ${dateFormatter.format(order.marketDate)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Reservation Deadline: ${dateFormatter.format(order.reservationDeadline)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            if (order.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    order.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (order.culturalContext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "🎉 ${order.culturalContext}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(
                        order.status.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = { /* Reserve order */ },
                    enabled = order.reservedQuantity < order.quantity,
                ) {
                    Text("Reserve Now")
                }
            }
        }
    }
}

@Composable
fun GroupBuyingCard(request: GroupBuyingRequest) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val progress =
        if (request.targetQuantity > 0) {
            (request.totalCommittedQuantity.toFloat() / request.targetQuantity.toFloat()).coerceIn(
                0f,
                1f,
            )
        } else {
            0f
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                request.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "${request.fowlType} - ${request.breed}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Max Price: ₹${request.maxPricePerBird}/bird",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Target: ${request.targetQuantity} birds",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Deadline: ${dateFormatter.format(request.deadline)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Progress: ${request.totalCommittedQuantity}/${request.targetQuantity}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Participants: ${request.currentParticipants}/${request.maxParticipants}",
                style = MaterialTheme.typography.bodySmall,
            )

            if (request.culturalPurpose.isNotEmpty()) {
                Text(
                    "🎉 ${request.culturalPurpose}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(
                        request.status.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = { /* Join group buy */ },
                    enabled = request.currentParticipants < request.maxParticipants,
                ) {
                    Text("Join Group")
                }
            }
        }
    }
}

@Composable
fun MarketTrendCard(trend: MarketTrend) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${trend.fowlType} - ${trend.breed}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    dateFormatter.format(trend.marketDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Avg Price:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "₹${trend.averagePrice}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column {
                    Text("Range:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "₹${trend.lowestPrice} - ₹${trend.highestPrice}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Column {
                    Text("Sold:", style = MaterialTheme.typography.bodySmall)
                    Text("${trend.totalSold}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Demand: ${trend.demandLevel.name.replace("_", " ")}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Suppliers: ${trend.supplierCount}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (trend.festivalImpact.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "🎉 ${trend.festivalImpact}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun PricePredictionCard(prediction: PricePrediction) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🔮 Price Prediction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "${prediction.fowlType} - ${prediction.breed}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Target Date: ${dateFormatter.format(prediction.targetDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Predicted Price:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "₹${prediction.predictedPrice.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Confidence:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "${prediction.confidence.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Range: ₹${prediction.priceRange.first.toInt()} - ₹${prediction.priceRange.second.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            if (prediction.influencingFactors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Factors: ${prediction.influencingFactors.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            if (prediction.recommendedAction.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "💡 ${prediction.recommendedAction}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// Placeholder dialogs for creating pre-orders and group buys
@Composable
fun CreatePreOrderDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    // Implementation for create pre-order dialog
    // This would include form fields for all PreMarketOrder properties
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Pre-Market Order") },
        text = { Text("Pre-order creation form would be implemented here with all necessary fields.") },
        confirmButton = {
            Button(onClick = onSuccess) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun CreateGroupBuyDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    // Implementation for create group buy dialog
    // This would include form fields for all GroupBuyingRequest properties
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Group Buying") },
        text = { Text("Group buying creation form would be implemented here with all necessary fields.") },
        confirmButton = {
            Button(onClick = onSuccess) {
                Text("Start")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MarketplaceScreenPreview() {
    MarketplaceScreen()
}

@Composable
fun BiddingSection(listingId: String) {
    var bids by remember { mutableStateOf(listOf<ParseObject>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listingId) {
        fetchBids(
            listingId = listingId,
            onResult = { bids = it },
            onError = { error = it },
            setLoading = { isLoading = it },
        )
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        ) {
            Text("Bids:", style = MaterialTheme.typography.titleSmall)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else if (bids.isEmpty()) {
                Text("No bids yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                bids.forEach { bid ->
                    Text(
                        "${bid.getParseUser("user")?.username ?: "User"}: ${bid.getString("amount")}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun compressImage(bitmap: Bitmap): ByteArray? {
    return try {
        val outputStream = ByteArrayOutputStream()
        // Adjusted quality based on previous log. For 2G, it was 20%. Let's keep it higher for general listings for now.
        val quality = 75 
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        if (!bitmap.isRecycled) {
            bitmap.recycle() // Ensure bitmap is recycled after compression
        }
        outputStream.toByteArray()
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().recordException(RuntimeException("Image compression failed: ${e.message}", e))
        null
    }
}

// Dummy object for TraditionalMarketService for compilation. Replace with actual implementation.
object TraditionalMarketService {
    fun fetchActiveDigitalMarketEvents(onResult: (List<DigitalMarketEvent>) -> Unit, onError: (String?) -> Unit, setLoading: (Boolean) -> Unit) { setLoading(false); onResult(emptyList()) }
    fun fetchTraditionalMarkets(region: String, onResult: (List<TraditionalMarket>) -> Unit, onError: (String?) -> Unit, setLoading: (Boolean) -> Unit) { setLoading(false); onResult(emptyList()) }
    fun fetchMarketCalendar(startDate: Date, endDate: Date, onResult: (List<MarketCalendarEntry>) -> Unit, onError: (String?) -> Unit, setLoading: (Boolean) -> Unit) { setLoading(false); onResult(emptyList()) }
    fun fetchPreMarketOrders(onResult: (List<PreMarketOrder>) -> Unit, onError: (String?) -> Unit, setLoading: (Boolean) -> Unit) { setLoading(false); onResult(emptyList()) }
    fun fetchGroupBuyingRequests(onResult: (List<GroupBuyingRequest>) -> Unit, onError: (String?) -> Unit, setLoading: (Boolean) -> Unit) { setLoading(false); onResult(emptyList()) }
}

// Define or import fetchBids if not already present
fun fetchBids(listingId: String, onResult: (List<ParseObject>) -> Unit, onError: (String?) -> Unit, setLoading: (Boolean) -> Unit) {
    // Placeholder - implement actual Parse query
    setLoading(true)
    val query = ParseQuery.getQuery<ParseObject>("Bid") // Assuming "Bid" class
    query.whereEqualTo("listingId", listingId)
    query.include("user") // Include user data for username
    query.orderByDescending("createdAt")
    query.findInBackground { bids, e ->
        setLoading(false)
        if (e == null) {
            onResult(bids ?: emptyList())
        } else {
            onError(e.localizedMessage ?: "Failed to fetch bids.")
        }
    }
}

// Dummy data classes for compilation. Replace with actual definitions from your project.
data class DigitalMarketEvent(val originalMarketName: String, val cancellationReason: String, val eventDuration: Int, val startTime: Date)
data class TraditionalMarket(val id: String?, val name: String, val location: String, val address: String, val marketType: MarketType, val marketDays: List<String>, val startTime: String, val endTime: String, val specialties: List<String>, val culturalSignificance: String)
data class MarketCalendarEntry(val marketName: String, val date: Date, val dayOfWeek: String, val location: String, val specialties: List<String>, val marketType: MarketType, val culturalEvents: List<String>)
data class PreMarketOrder(val id: String?, val fowlType: String, val breed: String, val quantity: Int, val reservedQuantity: Int, val pricePerBird: Double, val marketDate: Date, val reservationDeadline: Date, val description: String, val culturalContext: String, val status: PreMarketOrderStatus, val sellerName: String?)
data class GroupBuyingRequest(val id: String?, val title: String, val fowlType: String, val breed: String, val targetQuantity: Int, val totalCommittedQuantity: Int, val maxPricePerBird: Double, val deadline: Date, val currentParticipants: Int, val maxParticipants: Int, val culturalPurpose: String, val status: GroupBuyingStatus)
data class MarketTrend(val id: String?, val fowlType: String, val breed: String, val marketDate: Date, val averagePrice: Double, val lowestPrice: Double, val highestPrice: Double, val totalSold: Int, val demandLevel: DemandLevel, val supplierCount: Int, val festivalImpact: String)
data class PricePrediction(val fowlType: String, val breed: String, val targetDate: Date, val predictedPrice: Double, val confidence: Double, val priceRange: Pair<Double, Double>, val influencingFactors: List<String>, val recommendedAction: String)

enum class MarketType { DAILY, WEEKLY, MONTHLY, FESTIVAL_SPECIAL }
enum class PreMarketOrderStatus { OPEN, PARTIALLY_RESERVED, FULLY_RESERVED, CONFIRMED, COMPLETED, CANCELLED }
enum class GroupBuyingStatus { ORGANIZING, ACTIVE, MINIMUM_REACHED, CONFIRMED, COMPLETED, CANCELLED }
enum class DemandLevel { VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH }

// Placeholder for RoosterTheme - ensure it's defined in your project
@Composable
fun RoosterTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
