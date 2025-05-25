package com.example.rooster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.parse.ParseObject
import com.parse.ParseQuery
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

// Performance Monitor
class PerformanceMonitor {
    companion object {
        private var memoryUsage = mutableMapOf<String, Long>()
        private var networkUsage = mutableMapOf<String, Long>()
        private var loadTimes = mutableMapOf<String, Long>()

        fun startTimer(operation: String): Long {
            return System.currentTimeMillis()
        }

        fun endTimer(
            operation: String,
            startTime: Long,
        ) {
            val endTime = System.currentTimeMillis()
            loadTimes[operation] = endTime - startTime
        }

        fun recordMemoryUsage(
            operation: String,
            bytes: Long,
        ) {
            memoryUsage[operation] = bytes
        }

        fun recordNetworkUsage(
            operation: String,
            bytes: Long,
        ) {
            networkUsage[operation] = (networkUsage[operation] ?: 0) + bytes
        }

        fun getMetrics(): Map<String, Any> {
            return mapOf(
                "loadTimes" to loadTimes.toMap(),
                "memoryUsage" to memoryUsage.toMap(),
                "networkUsage" to networkUsage.toMap(),
            )
        }

        fun clearMetrics() {
            memoryUsage.clear()
            networkUsage.clear()
            loadTimes.clear()
        }
    }
}

// Network Quality Manager
class NetworkQualityManager(private val context: Context) {
    fun getCurrentNetworkQuality(): NetworkQualityLevel {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkQualityLevel.OFFLINE
            val capabilities =
                connectivityManager.getNetworkCapabilities(network)
                    ?: return NetworkQualityLevel.OFFLINE

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // For WiFi, assume good quality but check bandwidth if possible
                    val downBandwidth = capabilities.linkDownstreamBandwidthKbps
                    when {
                        downBandwidth > 5000 -> NetworkQualityLevel.EXCELLENT
                        downBandwidth > 1000 -> NetworkQualityLevel.GOOD
                        else -> NetworkQualityLevel.FAIR
                    }
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    when {
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> {
                            // Estimate based on download/upload bandwidth
                            val downBandwidth = capabilities.linkDownstreamBandwidthKbps
                            when {
                                downBandwidth > 5000 -> NetworkQualityLevel.EXCELLENT // >5 Mbps
                                downBandwidth > 1000 -> NetworkQualityLevel.GOOD // >1 Mbps
                                downBandwidth > 256 -> NetworkQualityLevel.FAIR // >256 Kbps
                                else -> NetworkQualityLevel.POOR // <256 Kbps
                            }
                        }

                        else -> NetworkQualityLevel.POOR
                    }
                }

                else -> NetworkQualityLevel.OFFLINE
            }
        } else {
            // Fallback for older Android versions
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            return when {
                activeNetwork?.isConnectedOrConnecting != true -> NetworkQualityLevel.OFFLINE
                activeNetwork.type == ConnectivityManager.TYPE_WIFI -> NetworkQualityLevel.GOOD
                activeNetwork.type == ConnectivityManager.TYPE_MOBILE -> {
                    when (activeNetwork.subtype) {
                        android.telephony.TelephonyManager.NETWORK_TYPE_LTE,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                        -> NetworkQualityLevel.GOOD

                        android.telephony.TelephonyManager.NETWORK_TYPE_UMTS,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA,
                        -> NetworkQualityLevel.FAIR

                        else -> NetworkQualityLevel.POOR
                    }
                }

                else -> NetworkQualityLevel.POOR
            }
        }
    }

    fun getOptimalCompressionLevel(): ImageCompressionLevel {
        return when (getCurrentNetworkQuality()) {
            NetworkQualityLevel.EXCELLENT -> ImageCompressionLevel.LOW
            NetworkQualityLevel.GOOD -> ImageCompressionLevel.MEDIUM
            NetworkQualityLevel.FAIR -> ImageCompressionLevel.HIGH
            NetworkQualityLevel.POOR, NetworkQualityLevel.OFFLINE -> ImageCompressionLevel.ULTRA
        }
    }
}

// Image Compression Utility Function
internal fun getCompressedImageBytes(
    originalBitmap: Bitmap,
    compressionLevel: ImageCompressionLevel,
): ByteArray {
    val quality = compressionLevel.quality
    val maxDimension = compressionLevel.maxDimension

    // Resize if necessary
    val resizedBitmap =
        if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
            val ratio =
                minOf(
                    maxDimension.toFloat() / originalBitmap.width,
                    maxDimension.toFloat() / originalBitmap.height,
                )
            val newWidth = (originalBitmap.width * ratio).toInt()
            val newHeight = (originalBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

    val outputStream = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val byteArray = outputStream.toByteArray()

    // Clean up bitmaps if they were modified (resizedBitmap might be different from originalBitmap)
    if (resizedBitmap != originalBitmap) {
        resizedBitmap.recycle()
    }
    // It's generally safer for the caller to handle recycling of the originalBitmap
    // if it's loaded and passed into this utility, especially if it might be used elsewhere.
    // However, if this utility is always the last consumer of a freshly decoded bitmap that won't be reused,
    // originalBitmap.recycle() could be called here too.
    // For now, let the caller manage originalBitmap's lifecycle outside this utility.

    return byteArray
}

// Optimized Image Manager
class OptimizedImageManager(private val context: Context) {
    private val imageCache = ConcurrentHashMap<String, WeakReference<ImageBitmap>>()
    private val networkQualityManager = NetworkQualityManager(context)
    private val cacheDir = File(context.cacheDir, "optimized_images")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    suspend fun loadOptimizedImage(
        url: String,
        compressionLevel: ImageCompressionLevel? = null,
    ): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val startTime = PerformanceMonitor.startTimer("image_load_$url")

            try {
                imageCache[url]?.get()?.let {
                    PerformanceMonitor.endTimer("image_load_$url", startTime)
                    return@withContext it
                }

                val actualCompressionLevel =
                    compressionLevel ?: networkQualityManager.getOptimalCompressionLevel()
                val cacheKey = "${url.hashCode()}_${actualCompressionLevel.name}"
                val cacheFile = File(cacheDir, "$cacheKey.jpg")

                if (cacheFile.exists()) {
                    BitmapFactory.decodeFile(cacheFile.absolutePath)?.let {
                        val imageBitmap = it.asImageBitmap()
                        imageCache[url] = WeakReference(imageBitmap)
                        PerformanceMonitor.recordMemoryUsage(
                            "image_cache_load_disk",
                            it.byteCount.toLong(),
                        )
                        PerformanceMonitor.endTimer("image_load_$url", startTime)
                        it.recycle() // Recycle bitmap after converting to ImageBitmap and caching
                        return@withContext imageBitmap
                    }
                }

                downloadImage(url)?.let { originalBitmap ->
                    val compressedBytes =
                        getCompressedImageBytes(originalBitmap, actualCompressionLevel)
                    // originalBitmap is not recycled by getCompressedImageBytes by default, so recycle it here
                    originalBitmap.recycle()

                    cacheFile.outputStream().use { it.write(compressedBytes) }
                    PerformanceMonitor.recordNetworkUsage(
                        "image_save_disk_cache",
                        compressedBytes.size.toLong(),
                    )

                    // Now load the just-saved compressed bytes into an ImageBitmap for the cache
                    BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)?.let {
                        val imageBitmap = it.asImageBitmap()
                        imageCache[url] = WeakReference(imageBitmap)
                        PerformanceMonitor.recordMemoryUsage(
                            "image_cache_save_memory",
                            it.byteCount.toLong(),
                        )
                        it.recycle()
                        PerformanceMonitor.endTimer("image_load_$url", startTime)
                        return@withContext imageBitmap
                    }
                }
            } catch (e: Exception) {
                PerformanceMonitor.endTimer("image_load_$url", startTime)
                // Log error e
            }
            null
        }

    private suspend fun downloadImage(url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = java.net.URL(url).openConnection().getInputStream()
                // It's important to use a buffered input stream for efficiency with decodeStream
                // val bufferedInputStream = java.io.BufferedInputStream(inputStream)
                val bitmap = BitmapFactory.decodeStream(inputStream) // Using inputStream directly
                inputStream.close()
                // bufferedInputStream.close() if used

                bitmap?.let {
                    PerformanceMonitor.recordNetworkUsage("image_download", it.byteCount.toLong())
                }
                bitmap
            } catch (e: Exception) {
                // Log error e
                null
            }
        }

    // This private compressImage is no longer needed if getCompressedImageBytes is used internally.
    // For now, keeping it as it was, but it's a candidate for removal/refactor.
    private fun compressImage(
        bitmap: Bitmap,
        compression: ImageCompressionLevel,
    ): Bitmap {
        val maxDimension = compression.maxDimension
        val resized =
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio =
                    minOf(
                        maxDimension.toFloat() / bitmap.width,
                        maxDimension.toFloat() / bitmap.height,
                    )
                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }
        return resized
    }

    fun clearCache() {
        imageCache.clear()
        cacheDir.listFiles()
            ?.forEach { it.deleteRecursively() } // Ensure recursive delete for directories
    }
}

// Optimized Parse Query Manager
class OptimizedParseQueryManager(private val context: Context) {
    private val networkQualityManager = NetworkQualityManager(context)

    fun <T : ParseObject> createOptimizedQuery(className: String): ParseQuery<T> {
        val query = ParseQuery.getQuery<T>(className)
        val networkQuality = networkQualityManager.getCurrentNetworkQuality()

        // Adjust query parameters based on network quality
        when (networkQuality) {
            NetworkQualityLevel.EXCELLENT -> {
                query.limit = 50
                query.cachePolicy = ParseQuery.CachePolicy.NETWORK_ELSE_CACHE
            }

            NetworkQualityLevel.GOOD -> {
                query.limit = 30
                query.cachePolicy = ParseQuery.CachePolicy.CACHE_ELSE_NETWORK
            }

            NetworkQualityLevel.FAIR -> {
                query.limit = 20
                query.cachePolicy = ParseQuery.CachePolicy.CACHE_ELSE_NETWORK
                query.setMaxCacheAge(300000) // 5 minutes
            }

            NetworkQualityLevel.POOR -> {
                query.limit = 10
                query.cachePolicy = ParseQuery.CachePolicy.CACHE_ELSE_NETWORK
                query.setMaxCacheAge(600000) // 10 minutes
            }

            NetworkQualityLevel.OFFLINE -> {
                query.limit = 10
                query.cachePolicy = ParseQuery.CachePolicy.CACHE_ONLY
            }
        }

        // Enable local datastore for offline capability
        query.fromLocalDatastore()

        return query
    }

    suspend fun <T : ParseObject> executeOptimizedQuery(
        query: ParseQuery<T>,
        operation: String,
    ): List<T> =
        withContext(Dispatchers.IO) {
            val startTime = PerformanceMonitor.startTimer("query_$operation")

            try {
                val results = query.find()
                PerformanceMonitor.recordNetworkUsage("query_$operation", results.size * 1024L)
                PerformanceMonitor.endTimer("query_$operation", startTime)
                results
            } catch (e: Exception) {
                PerformanceMonitor.endTimer("query_$operation", startTime)
                // Fallback to cache
                try {
                    query.cachePolicy = ParseQuery.CachePolicy.CACHE_ONLY
                    query.find()
                } catch (cacheException: Exception) {
                    emptyList()
                }
            }
        }
}

// Progressive Loading Manager
class ProgressiveLoadingManager {
    data class LoadingState(
        val isLoading: Boolean = false,
        val progress: Float = 0f,
        val error: String? = null,
        val loadedItems: Int = 0,
        val totalItems: Int = 0,
    )

    suspend fun <T> loadProgressively(
        items: List<T>,
        batchSize: Int = 5,
        loadDelay: Long = 100,
        onProgress: (LoadingState) -> Unit,
        loader: suspend (T) -> Unit,
    ) {
        onProgress(LoadingState(isLoading = true, totalItems = items.size))

        items.chunked(batchSize).forEachIndexed { chunkIndex, chunk ->
            try {
                chunk.forEach { item ->
                    loader(item)
                }

                val loadedCount = (chunkIndex + 1) * batchSize
                val actualLoaded = minOf(loadedCount, items.size)
                val progress = actualLoaded.toFloat() / items.size

                onProgress(
                    LoadingState(
                        isLoading = actualLoaded < items.size,
                        progress = progress,
                        loadedItems = actualLoaded,
                        totalItems = items.size,
                    ),
                )

                if (actualLoaded < items.size) {
                    delay(loadDelay)
                }
            } catch (e: Exception) {
                onProgress(
                    LoadingState(
                        isLoading = false,
                        error = e.localizedMessage,
                        loadedItems = chunkIndex * batchSize,
                        totalItems = items.size,
                    ),
                )
                return@loadProgressively
            }
        }
    }
}

// Offline Mode Manager
class OfflineModeManager(private val context: Context) {
    fun enableOfflineMode() {
        com.parse.Parse.enableLocalDatastore(context)
    }

    suspend fun cacheEssentialData() =
        withContext(Dispatchers.IO) {
            try {
                // Cache user's fowl data
                val fowlQuery = ParseQuery.getQuery<ParseObject>("Fowl")
                fowlQuery.whereEqualTo("owner", com.parse.ParseUser.getCurrentUser())
                fowlQuery.cachePolicy = ParseQuery.CachePolicy.NETWORK_ELSE_CACHE
                val fowlData = fowlQuery.find()
                ParseObject.pinAllInBackground("offline_fowl", fowlData)

                // Cache recent marketplace listings
                val marketplaceQuery = ParseQuery.getQuery<ParseObject>("Listing")
                marketplaceQuery.orderByDescending("createdAt")
                marketplaceQuery.limit = 20
                marketplaceQuery.cachePolicy = ParseQuery.CachePolicy.NETWORK_ELSE_CACHE
                val marketplaceData = marketplaceQuery.find()
                ParseObject.pinAllInBackground("offline_marketplace", marketplaceData)

                // Cache traditional markets
                val marketsQuery = ParseQuery.getQuery<ParseObject>("TraditionalMarket")
                marketsQuery.whereEqualTo("isActive", true)
                marketsQuery.cachePolicy = ParseQuery.CachePolicy.NETWORK_ELSE_CACHE
                val marketsData = marketsQuery.find()
                ParseObject.pinAllInBackground("offline_markets", marketsData)
            } catch (e: Exception) {
                // Offline caching failed, but app should still work
            }
        }

    suspend fun getOfflineData(
        className: String,
        pinName: String,
    ): List<ParseObject> =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>(className)
                query.fromPin(pinName)
                query.find()
            } catch (e: Exception) {
                emptyList()
            }
        }

    fun clearOfflineCache() {
        ParseObject.unpinAllInBackground()
    }
}

// Background Sync Manager
class BackgroundSyncManager(private val context: Context) {
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val networkQualityManager = NetworkQualityManager(context)

    fun startPeriodicSync() {
        syncScope.launch {
            while (true) {
                val networkQuality = networkQualityManager.getCurrentNetworkQuality()

                val syncInterval =
                    when (networkQuality) {
                        NetworkQualityLevel.EXCELLENT -> 30_000L // 30 seconds
                        NetworkQualityLevel.GOOD -> 60_000L // 1 minute
                        NetworkQualityLevel.FAIR -> 300_000L // 5 minutes
                        NetworkQualityLevel.POOR -> 600_000L // 10 minutes
                        NetworkQualityLevel.OFFLINE -> 1800_000L // 30 minutes
                    }

                if (networkQuality != NetworkQualityLevel.OFFLINE) {
                    performSync()
                }

                delay(syncInterval)
            }
        }
    }

    private suspend fun performSync() {
        try {
            syncCriticalData()

            if (networkQualityManager.getCurrentNetworkQuality() in
                listOf(
                    NetworkQualityLevel.EXCELLENT,
                    NetworkQualityLevel.GOOD,
                )
            ) {
                syncNonCriticalData()
            }
        } catch (e: Exception) {
            // Log error but continue
        }
    }

    private suspend fun syncCriticalData() {
        val fowlQuery =
            OptimizedParseQueryManager(context)
                .createOptimizedQuery<ParseObject>("Fowl")
        fowlQuery.whereEqualTo("owner", com.parse.ParseUser.getCurrentUser())

        val fowlData =
            OptimizedParseQueryManager(context)
                .executeOptimizedQuery(fowlQuery, "sync_fowl")

        ParseObject.pinAllInBackground("fowl_cache", fowlData)
    }

    private suspend fun syncNonCriticalData() {
        val marketplaceQuery =
            OptimizedParseQueryManager(context)
                .createOptimizedQuery<ParseObject>("Listing")
        marketplaceQuery.orderByDescending("createdAt")

        val marketplaceData =
            OptimizedParseQueryManager(context)
                .executeOptimizedQuery(marketplaceQuery, "sync_marketplace")

        ParseObject.pinAllInBackground("marketplace_cache", marketplaceData)
    }

    fun stopSync() {
        syncScope.cancel()
    }
}

// Composable for network-aware loading
@Composable
fun NetworkAwareLoader(
    context: Context,
    content: @Composable (NetworkQualityLevel) -> Unit,
) {
    val networkQualityManager = remember { NetworkQualityManager(context) }
    var networkQuality by remember { mutableStateOf(NetworkQualityLevel.GOOD) }

    LaunchedEffect(Unit) {
        while (true) {
            networkQuality = networkQualityManager.getCurrentNetworkQuality()
            delay(5000) // Check every 5 seconds
        }
    }

    content(networkQuality)
}
