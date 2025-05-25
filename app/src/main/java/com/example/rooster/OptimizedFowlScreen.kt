package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseUser
import kotlinx.coroutines.launch

@Composable
fun OptimizedFowlScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Performance optimization managers
    val imageManager = remember { OptimizedImageManager(context) }
    val queryManager = remember { OptimizedParseQueryManager(context) }
    val progressiveLoader = remember { ProgressiveLoadingManager() }
    val offlineManager = remember { OfflineModeManager(context) }

    // State management
    var fowls by remember { mutableStateOf(listOf<ParseObject>()) }
    var selectedFowl by remember { mutableStateOf<ParseObject?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var progressiveLoadingState by remember {
        mutableStateOf(ProgressiveLoadingManager.LoadingState())
    }

    // Performance metrics
    var performanceMetrics by remember { mutableStateOf(mapOf<String, Any>()) }

    // Load fowl data with optimization
    LaunchedEffect(Unit) {
        offlineManager.enableOfflineMode()
        loadFowlsOptimized(queryManager, { fowls = it }, { error = it }, { loading = it })
    }

    NetworkAwareLoader(context) { networkQuality ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            // Network Quality Indicator
            NetworkQualityIndicator(networkQuality)

            Spacer(modifier = Modifier.height(8.dp))

            // Performance Metrics (Debug)
            if (performanceMetrics.isNotEmpty()) {
                PerformanceMetricsCard(performanceMetrics)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Loading State
            if (loading || progressiveLoadingState.isLoading) {
                OptimizedLoadingIndicator(progressiveLoadingState)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Error State
            if (error.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Content
            if (selectedFowl == null) {
                // Fowl List with Progressive Loading
                OptimizedFowlList(
                    fowls = fowls,
                    imageManager = imageManager,
                    networkQuality = networkQuality,
                    onFowlSelected = { selectedFowl = it },
                    progressiveLoader = progressiveLoader,
                    onProgressUpdate = { progressiveLoadingState = it },
                )
            } else {
                // Fowl Detail with Lineage Tree
                OptimizedFowlDetail(
                    fowl = selectedFowl!!,
                    imageManager = imageManager,
                    queryManager = queryManager,
                    progressiveLoader = progressiveLoader,
                    networkQuality = networkQuality,
                    onBack = { selectedFowl = null },
                    onProgressUpdate = { progressiveLoadingState = it },
                )
            }

            // Performance Metrics Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        performanceMetrics = PerformanceMonitor.getMetrics()
                    },
                ) {
                    Text("Show Metrics")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            imageManager.clearCache()
                            offlineManager.clearOfflineCache()
                            PerformanceMonitor.clearMetrics()
                            performanceMetrics = emptyMap()
                        }
                    },
                ) {
                    Text("Clear Cache")
                }
            }
        }
    }
}

@Composable
fun NetworkQualityIndicator(networkQuality: NetworkQualityLevel) {
    val (color, text) =
        when (networkQuality) {
            NetworkQualityLevel.EXCELLENT -> MaterialTheme.colorScheme.primary to "📶 Excellent"
            NetworkQualityLevel.GOOD -> MaterialTheme.colorScheme.primary to "📶 Good"
            NetworkQualityLevel.FAIR -> MaterialTheme.colorScheme.secondary to "📶 Fair"
            NetworkQualityLevel.POOR -> MaterialTheme.colorScheme.error to "📶 Poor"
            NetworkQualityLevel.OFFLINE -> MaterialTheme.colorScheme.error to "📵 Offline"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Text(
            text = "Network: $text",
            modifier = Modifier.padding(8.dp),
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun PerformanceMetricsCard(metrics: Map<String, Any>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Performance Metrics", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            (metrics["loadTimes"] as? Map<*, *>)?.let { loadTimes ->
                Text("Load Times:", style = MaterialTheme.typography.bodySmall)
                loadTimes.forEach { (key, value) ->
                    Text("  $key: ${value}ms", style = MaterialTheme.typography.bodySmall)
                }
            }

            (metrics["memoryUsage"] as? Map<*, *>)?.let { memoryUsage ->
                Text("Memory Usage:", style = MaterialTheme.typography.bodySmall)
                memoryUsage.forEach { (key, value) ->
                    val mb = (value as Long) / (1024 * 1024)
                    Text("  $key: ${mb}MB", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun OptimizedLoadingIndicator(loadingState: ProgressiveLoadingManager.LoadingState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Loading...")
                Text("${loadingState.loadedItems}/${loadingState.totalItems}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = loadingState.progress,
                modifier = Modifier.fillMaxWidth(),
            )

            if (loadingState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Error: ${loadingState.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun OptimizedFowlList(
    fowls: List<ParseObject>,
    imageManager: OptimizedImageManager,
    networkQuality: NetworkQualityLevel,
    onFowlSelected: (ParseObject) -> Unit,
    progressiveLoader: ProgressiveLoadingManager,
    onProgressUpdate: (ProgressiveLoadingManager.LoadingState) -> Unit,
) {
    var displayedFowls by remember { mutableStateOf(listOf<ParseObject>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(fowls) {
        if (fowls.isNotEmpty()) {
            progressiveLoader.loadProgressively(
                items = fowls,
                batchSize = if (networkQuality == NetworkQualityLevel.POOR) 3 else 5,
                loadDelay = if (networkQuality == NetworkQualityLevel.POOR) 200 else 100,
                onProgress = onProgressUpdate,
            ) { fowl ->
                displayedFowls = displayedFowls + fowl
            }
        }
    }

    LazyColumn {
        items(displayedFowls) { fowl ->
            OptimizedFowlCard(
                fowl = fowl,
                imageManager = imageManager,
                networkQuality = networkQuality,
                onClick = { onFowlSelected(fowl) },
            )
        }
    }
}

@Composable
fun OptimizedFowlCard(
    fowl: ParseObject,
    imageManager: OptimizedImageManager,
    networkQuality: NetworkQualityLevel,
    onClick: () -> Unit,
) {
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(fowl.objectId) {
        fowl.getParseFile("image")?.url?.let { url ->
            val compressionLevel =
                when (networkQuality) {
                    NetworkQualityLevel.POOR, NetworkQualityLevel.OFFLINE -> ImageCompressionLevel.ULTRA
                    NetworkQualityLevel.FAIR -> ImageCompressionLevel.HIGH
                    NetworkQualityLevel.GOOD -> ImageCompressionLevel.MEDIUM
                    NetworkQualityLevel.EXCELLENT -> ImageCompressionLevel.LOW
                }

            imageBitmap = imageManager.loadOptimizedImage(url, compressionLevel)
        }
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Optimized Image Loading
            if (imageBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Fowl image",
                    modifier = Modifier.size(64.dp),
                )
            } else {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    fowl.getString("name") ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    fowl.getString("breed") ?: "Unknown breed",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Age: ${fowl.getInt("ageInWeeks")} weeks",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun OptimizedFowlDetail(
    fowl: ParseObject,
    imageManager: OptimizedImageManager,
    queryManager: OptimizedParseQueryManager,
    progressiveLoader: ProgressiveLoadingManager,
    networkQuality: NetworkQualityLevel,
    onBack: () -> Unit,
    onProgressUpdate: (ProgressiveLoadingManager.LoadingState) -> Unit,
) {
    var lineageData by remember { mutableStateOf(listOf<ParseObject>()) }
    var lineageLoading by remember { mutableStateOf(false) }

    LaunchedEffect(fowl.objectId) {
        loadLineageDataOptimized(
            fowl = fowl,
            queryManager = queryManager,
            progressiveLoader = progressiveLoader,
            onResult = { lineageData = it },
            onProgressUpdate = onProgressUpdate,
            setLoading = { lineageLoading = it },
        )
    }

    LazyColumn {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onBack) {
                    Text("← Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    fowl.getString("name") ?: "Unknown",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OptimizedFowlBasicInfo(fowl, imageManager, networkQuality)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Lineage Tree", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (lineageLoading) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                OptimizedLineageTree(
                    lineageData = lineageData,
                    imageManager = imageManager,
                    networkQuality = networkQuality,
                )
            }
        }
    }
}

@Composable
fun OptimizedFowlBasicInfo(
    fowl: ParseObject,
    imageManager: OptimizedImageManager,
    networkQuality: NetworkQualityLevel,
) {
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(fowl.objectId) {
        fowl.getParseFile("image")?.url?.let { url ->
            imageBitmap = imageManager.loadOptimizedImage(url)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                if (imageBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Fowl image",
                        modifier = Modifier.size(120.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Name: ${fowl.getString("name") ?: "Unknown"}")
                    Text("Breed: ${fowl.getString("breed") ?: "Unknown"}")
                    Text("Gender: ${fowl.getString("gender") ?: "Unknown"}")
                    Text("Age: ${fowl.getInt("ageInWeeks")} weeks")
                    Text("Color: ${fowl.getString("color") ?: "Unknown"}")
                    Text("Weight: ${fowl.getDouble("weight")} kg")
                }
            }
        }
    }
}

@Composable
fun OptimizedLineageTree(
    lineageData: List<ParseObject>,
    imageManager: OptimizedImageManager,
    networkQuality: NetworkQualityLevel,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (lineageData.isEmpty()) {
                Text("No lineage data available")
            } else {
                Text("Family Tree (${lineageData.size} relatives)")
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(lineageData) { relative ->
                        OptimizedLineageCard(
                            fowl = relative,
                            imageManager = imageManager,
                            networkQuality = networkQuality,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptimizedLineageCard(
    fowl: ParseObject,
    imageManager: OptimizedImageManager,
    networkQuality: NetworkQualityLevel,
) {
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(fowl.objectId) {
        fowl.getParseFile("image")?.url?.let { url ->
            val compressionLevel = ImageCompressionLevel.ULTRA // Always use smallest for lineage
            imageBitmap = imageManager.loadOptimizedImage(url, compressionLevel)
        }
    }

    Card(modifier = Modifier.width(100.dp)) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (imageBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Relative image",
                    modifier = Modifier.size(60.dp),
                )
            } else {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                fowl.getString("name") ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            Text(
                fowl.getString("relationship") ?: "Relative",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

// Helper functions for data loading
suspend fun loadFowlsOptimized(
    queryManager: OptimizedParseQueryManager,
    onResult: (List<ParseObject>) -> Unit,
    onError: (String) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    try {
        setLoading(true)
        val query = queryManager.createOptimizedQuery<ParseObject>("Fowl")
        query.whereEqualTo("owner", ParseUser.getCurrentUser())
        query.orderByDescending("createdAt")

        val results = queryManager.executeOptimizedQuery(query, "fowl_list")
        onResult(results)
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Failed to load fowls")
    } finally {
        setLoading(false)
    }
}

suspend fun loadLineageDataOptimized(
    fowl: ParseObject,
    queryManager: OptimizedParseQueryManager,
    progressiveLoader: ProgressiveLoadingManager,
    onResult: (List<ParseObject>) -> Unit,
    onProgressUpdate: (ProgressiveLoadingManager.LoadingState) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    try {
        setLoading(true)

        // Load parents
        val parentsQuery = queryManager.createOptimizedQuery<ParseObject>("Fowl")
        val parentIds =
            listOfNotNull(
                fowl.getParseObject("father")?.objectId,
                fowl.getParseObject("mother")?.objectId,
            )

        if (parentIds.isNotEmpty()) {
            parentsQuery.whereContainedIn("objectId", parentIds)
            val parents = queryManager.executeOptimizedQuery(parentsQuery, "lineage_parents")

            // Load siblings
            val siblingsQuery = queryManager.createOptimizedQuery<ParseObject>("Fowl")
            siblingsQuery.whereEqualTo("father", fowl.getParseObject("father"))
            siblingsQuery.whereEqualTo("mother", fowl.getParseObject("mother"))
            siblingsQuery.whereNotEqualTo("objectId", fowl.objectId)
            val siblings = queryManager.executeOptimizedQuery(siblingsQuery, "lineage_siblings")

            val allRelatives = parents + siblings

            // Progressive load the lineage tree
            val processedRelatives = mutableListOf<ParseObject>()
            progressiveLoader.loadProgressively(
                items = allRelatives,
                batchSize = 3,
                loadDelay = 150,
                onProgress = onProgressUpdate,
            ) { relative ->
                // Add relationship information
                when {
                    parentIds.contains(relative.objectId) -> {
                        relative.put(
                            "relationship",
                            if (relative.getString("gender") == "Male") "Father" else "Mother",
                        )
                    }

                    else -> {
                        relative.put("relationship", "Sibling")
                    }
                }
                processedRelatives.add(relative)
            }

            onResult(processedRelatives)
        } else {
            onResult(emptyList())
        }
    } catch (e: Exception) {
        onResult(emptyList())
    } finally {
        setLoading(false)
    }
}
