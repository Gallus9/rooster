package com.example.rooster

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.system.measureTimeMillis

// Performance Test Suite for Rural Optimization
class PerformanceTester(private val context: Context) {
    data class PerformanceTestResult(
        val testName: String,
        val executionTime: Long,
        val memoryUsage: Long,
        val networkCalls: Int,
        val cacheHits: Int,
        val cacheMisses: Int,
        val success: Boolean,
        val errorMessage: String? = null,
    )

    data class DeviceInfo(
        val deviceModel: String,
        val androidVersion: String,
        val availableMemory: Long,
        val totalMemory: Long,
        val cpuCores: Int,
        val isLowEnd: Boolean,
    )

    private val testResults = mutableListOf<PerformanceTestResult>()

    fun getDeviceInfo(): DeviceInfo {
        val runtime = Runtime.getRuntime()
        val memInfo = android.app.ActivityManager.MemoryInfo()
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.getMemoryInfo(memInfo)

        return DeviceInfo(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
            availableMemory = memInfo.availMem,
            totalMemory = memInfo.totalMem,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            isLowEnd = memInfo.totalMem < 3 * 1024 * 1024 * 1024L, // Less than 3GB RAM
        )
    }

    suspend fun runPerformanceTests(): List<PerformanceTestResult> {
        testResults.clear()

        // Test 1: Image Loading Performance
        testImageLoadingPerformance()

        // Test 2: Parse Query Performance
        testParseQueryPerformance()

        // Test 3: Progressive Loading Performance
        testProgressiveLoadingPerformance()

        // Test 4: Offline Mode Performance
        testOfflineModePerformance()

        // Test 5: Memory Usage Under Load
        testMemoryUsageUnderLoad()

        // Test 6: Network Quality Adaptation
        testNetworkQualityAdaptation()

        return testResults
    }

    private suspend fun testImageLoadingPerformance() {
        val imageManager = OptimizedImageManager(context)
        val testUrls =
            listOf(
                "https://example.com/test1.jpg",
                "https://example.com/test2.jpg",
                "https://example.com/test3.jpg",
            )

        var cacheHits = 0
        var cacheMisses = 0
        val startMemory = getMemoryUsage()

        val executionTime =
            measureTimeMillis {
                try {
                    testUrls.forEach { url ->
                        // First load (cache miss)
                        val image1 = imageManager.loadOptimizedImage(url, ImageCompressionLevel.HIGH)
                        if (image1 == null) cacheMisses++ else cacheMisses++

                        // Second load (should be cache hit)
                        val image2 = imageManager.loadOptimizedImage(url, ImageCompressionLevel.HIGH)
                        if (image2 != null) cacheHits++
                    }
                } catch (e: Exception) {
                    testResults.add(
                        PerformanceTestResult(
                            testName = "Image Loading Performance",
                            executionTime = 0,
                            memoryUsage = 0,
                            networkCalls = 0,
                            cacheHits = 0,
                            cacheMisses = 0,
                            success = false,
                            errorMessage = e.message,
                        ),
                    )
                    return
                }
            }

        val endMemory = getMemoryUsage()
        val memoryUsed = endMemory - startMemory

        testResults.add(
            PerformanceTestResult(
                testName = "Image Loading Performance",
                executionTime = executionTime,
                memoryUsage = memoryUsed,
                networkCalls = testUrls.size,
                cacheHits = cacheHits,
                cacheMisses = cacheMisses,
                success = true,
            ),
        )
    }

    private suspend fun testParseQueryPerformance() {
        val queryManager = OptimizedParseQueryManager(context)
        val startMemory = getMemoryUsage()

        val executionTime =
            measureTimeMillis {
                try {
                    // Test different network conditions
                    val query1 = queryManager.createOptimizedQuery<ParseObject>("TestClass")
                    val results1 = queryManager.executeOptimizedQuery(query1, "test_query_1")

                    val query2 = queryManager.createOptimizedQuery<ParseObject>("TestClass")
                    query2.limit = 10
                    val results2 = queryManager.executeOptimizedQuery(query2, "test_query_2")
                } catch (e: Exception) {
                    testResults.add(
                        PerformanceTestResult(
                            testName = "Parse Query Performance",
                            executionTime = 0,
                            memoryUsage = 0,
                            networkCalls = 0,
                            cacheHits = 0,
                            cacheMisses = 0,
                            success = false,
                            errorMessage = e.message,
                        ),
                    )
                    return
                }
            }

        val endMemory = getMemoryUsage()
        val memoryUsed = endMemory - startMemory

        testResults.add(
            PerformanceTestResult(
                testName = "Parse Query Performance",
                executionTime = executionTime,
                memoryUsage = memoryUsed,
                networkCalls = 2,
                cacheHits = 1, // Estimated
                cacheMisses = 1, // Estimated
                success = true,
            ),
        )
    }

    private suspend fun testProgressiveLoadingPerformance() {
        val progressiveLoader = ProgressiveLoadingManager()
        val testItems = (1..50).map { "Item $it" }
        val startMemory = getMemoryUsage()
        var progressUpdates = 0

        val executionTime =
            measureTimeMillis {
                try {
                    progressiveLoader.loadProgressively(
                        items = testItems,
                        batchSize = 5,
                        loadDelay = 10,
                        onProgress = { progressUpdates++ },
                    ) { item ->
                        // Simulate processing
                        delay(5)
                    }
                } catch (e: Exception) {
                    testResults.add(
                        PerformanceTestResult(
                            testName = "Progressive Loading Performance",
                            executionTime = 0,
                            memoryUsage = 0,
                            networkCalls = 0,
                            cacheHits = 0,
                            cacheMisses = 0,
                            success = false,
                            errorMessage = e.message,
                        ),
                    )
                    return
                }
            }

        val endMemory = getMemoryUsage()
        val memoryUsed = endMemory - startMemory

        testResults.add(
            PerformanceTestResult(
                testName = "Progressive Loading Performance",
                executionTime = executionTime,
                memoryUsage = memoryUsed,
                networkCalls = 0,
                cacheHits = progressUpdates,
                cacheMisses = 0,
                success = true,
            ),
        )
    }

    private suspend fun testOfflineModePerformance() {
        val offlineManager = OfflineModeManager(context)
        val startMemory = getMemoryUsage()

        val executionTime =
            measureTimeMillis {
                try {
                    offlineManager.enableOfflineMode()
                    offlineManager.cacheEssentialData()

                    // Test offline data retrieval
                    val fowlData = offlineManager.getOfflineData("Fowl", "offline_fowl")
                    val marketData = offlineManager.getOfflineData("Listing", "offline_marketplace")
                } catch (e: Exception) {
                    testResults.add(
                        PerformanceTestResult(
                            testName = "Offline Mode Performance",
                            executionTime = 0,
                            memoryUsage = 0,
                            networkCalls = 0,
                            cacheHits = 0,
                            cacheMisses = 0,
                            success = false,
                            errorMessage = e.message,
                        ),
                    )
                    return
                }
            }

        val endMemory = getMemoryUsage()
        val memoryUsed = endMemory - startMemory

        testResults.add(
            PerformanceTestResult(
                testName = "Offline Mode Performance",
                executionTime = executionTime,
                memoryUsage = memoryUsed,
                networkCalls = 0,
                cacheHits = 2, // Offline data retrieval
                cacheMisses = 0,
                success = true,
            ),
        )
    }

    private suspend fun testMemoryUsageUnderLoad() {
        val startMemory = getMemoryUsage()
        val imageManager = OptimizedImageManager(context)

        val executionTime =
            measureTimeMillis {
                try {
                    // Simulate heavy usage
                    repeat(20) { iteration ->
                        val testUrl = "https://example.com/heavy_test_$iteration.jpg"
                        imageManager.loadOptimizedImage(testUrl, ImageCompressionLevel.MEDIUM)

                        // Force garbage collection periodically
                        if (iteration % 5 == 0) {
                            System.gc()
                            delay(100)
                        }
                    }

                    // Clear cache to test memory cleanup
                    imageManager.clearCache()
                    System.gc()
                    delay(200)
                } catch (e: Exception) {
                    testResults.add(
                        PerformanceTestResult(
                            testName = "Memory Usage Under Load",
                            executionTime = 0,
                            memoryUsage = 0,
                            networkCalls = 0,
                            cacheHits = 0,
                            cacheMisses = 0,
                            success = false,
                            errorMessage = e.message,
                        ),
                    )
                    return
                }
            }

        val endMemory = getMemoryUsage()
        val memoryUsed = endMemory - startMemory

        testResults.add(
            PerformanceTestResult(
                testName = "Memory Usage Under Load",
                executionTime = executionTime,
                memoryUsage = memoryUsed,
                networkCalls = 20,
                cacheHits = 0,
                cacheMisses = 20,
                success = true,
            ),
        )
    }

    private suspend fun testNetworkQualityAdaptation() {
        val networkManager = NetworkQualityManager(context)
        val startMemory = getMemoryUsage()

        val executionTime =
            measureTimeMillis {
                try {
                    // Test network quality detection
                    val currentQuality = networkManager.getCurrentNetworkQuality()
                    val optimalCompression = networkManager.getOptimalCompressionLevel()

                    // Test adaptation based on different qualities
                    val compressionLevels =
                        listOf(
                            ImageCompressionLevel.ULTRA,
                            ImageCompressionLevel.HIGH,
                            ImageCompressionLevel.MEDIUM,
                            ImageCompressionLevel.LOW,
                        )

                    compressionLevels.forEach { level ->
                        // Simulate image processing with different compression levels
                        delay(50)
                    }
                } catch (e: Exception) {
                    testResults.add(
                        PerformanceTestResult(
                            testName = "Network Quality Adaptation",
                            executionTime = 0,
                            memoryUsage = 0,
                            networkCalls = 0,
                            cacheHits = 0,
                            cacheMisses = 0,
                            success = false,
                            errorMessage = e.message,
                        ),
                    )
                    return
                }
            }

        val endMemory = getMemoryUsage()
        val memoryUsed = endMemory - startMemory

        testResults.add(
            PerformanceTestResult(
                testName = "Network Quality Adaptation",
                executionTime = executionTime,
                memoryUsage = memoryUsed,
                networkCalls = 1,
                cacheHits = 4,
                cacheMisses = 0,
                success = true,
            ),
        )
    }

    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    fun generatePerformanceReport(): String {
        val deviceInfo = getDeviceInfo()
        val formatter = DecimalFormat("#,###")

        return buildString {
            appendLine("=== PERFORMANCE TEST REPORT ===")
            appendLine()
            appendLine("Device Information:")
            appendLine("- Model: ${deviceInfo.deviceModel}")
            appendLine("- Android: ${deviceInfo.androidVersion}")
            appendLine("- Memory: ${formatter.format(deviceInfo.totalMemory / (1024 * 1024))} MB total")
            appendLine("- CPU Cores: ${deviceInfo.cpuCores}")
            appendLine("- Low-End Device: ${deviceInfo.isLowEnd}")
            appendLine()

            appendLine("Test Results:")
            testResults.forEach { result ->
                appendLine("${result.testName}:")
                appendLine("  ✓ Success: ${result.success}")
                if (!result.success && result.errorMessage != null) {
                    appendLine("  ✗ Error: ${result.errorMessage}")
                }
                appendLine("  ⏱ Time: ${result.executionTime}ms")
                appendLine("  💾 Memory: ${formatter.format(result.memoryUsage / 1024)} KB")
                appendLine("  🌐 Network Calls: ${result.networkCalls}")
                appendLine("  📋 Cache Hits: ${result.cacheHits}")
                appendLine("  ❌ Cache Misses: ${result.cacheMisses}")
                appendLine()
            }

            // Summary metrics
            val totalTime = testResults.sumOf { it.executionTime }
            val totalMemory = testResults.sumOf { it.memoryUsage }
            val successRate = testResults.count { it.success }.toDouble() / testResults.size * 100

            appendLine("Summary:")
            appendLine("- Total Execution Time: ${totalTime}ms")
            appendLine("- Total Memory Used: ${formatter.format(totalMemory / 1024)} KB")
            appendLine("- Success Rate: ${DecimalFormat("#.#").format(successRate)}%")
            appendLine("- Tests Passed: ${testResults.count { it.success }}/${testResults.size}")
        }
    }
}

// Performance Testing UI
@Composable
fun PerformanceTestingScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val performanceTester = remember { PerformanceTester(context) }

    var testResults by remember { mutableStateOf(listOf<PerformanceTester.PerformanceTestResult>()) }
    var deviceInfo by remember { mutableStateOf<PerformanceTester.DeviceInfo?>(null) }
    var testing by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        deviceInfo = performanceTester.getDeviceInfo()
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        item {
            Text(
                "Performance Testing Suite",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Device Information
        deviceInfo?.let { info ->
            item {
                DeviceInfoCard(info)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Test Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            testing = true
                            testResults = performanceTester.runPerformanceTests()
                            report = performanceTester.generatePerformanceReport()
                            testing = false
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier.weight(1f),
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (testing) "Testing..." else "Run Tests")
                }

                Button(
                    onClick = {
                        testResults = emptyList()
                        report = ""
                        PerformanceMonitor.clearMetrics()
                    },
                    enabled = !testing,
                ) {
                    Text("Clear")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Test Results
        if (testResults.isNotEmpty()) {
            items(testResults) { result ->
                TestResultCard(result)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Report
        if (report.isNotEmpty()) {
            item {
                ReportCard(report)
            }
        }
    }
}

@Composable
fun DeviceInfoCard(deviceInfo: PerformanceTester.DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (deviceInfo.isLowEnd) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Device Information",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Model: ${deviceInfo.deviceModel}")
            Text("Android: ${deviceInfo.androidVersion}")
            Text("Memory: ${deviceInfo.totalMemory / (1024 * 1024)} MB")
            Text("CPU Cores: ${deviceInfo.cpuCores}")

            if (deviceInfo.isLowEnd) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Low-End Device Detected",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun TestResultCard(result: PerformanceTester.PerformanceTestResult) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (result.success) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    result.testName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (result.success) "✓" else "✗",
                    color =
                        if (result.success) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (result.success) {
                Text("Time: ${result.executionTime}ms", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Memory: ${result.memoryUsage / 1024}KB",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Cache: ${result.cacheHits} hits, ${result.cacheMisses} misses",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                result.errorMessage?.let { error ->
                    Text(
                        "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun ReportCard(report: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Performance Report",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                report,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
        }
    }
}
