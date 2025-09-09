package com.example.rooster

import android.content.Context
import com.parse.ParseCloud
import com.parse.ParseObject
import com.parse.ParseQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enhanced Parse REST client with network-aware optimizations and performance monitoring
 * Designed for rural networks with poor connectivity
 */
class ParseRestClient(private val context: Context) {
    private val networkQualityManager = NetworkQualityManager(context)

    /**
     * Network-aware query execution with automatic optimization
     */
    suspend fun executeOptimizedQuery(
        className: String,
        userId: String? = null,
        customQuery: ((ParseQuery<ParseObject>) -> Unit)? = null,
    ): QueryResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val networkQuality = networkQualityManager.getCurrentNetworkQuality()

            try {
                // Use Parse Cloud Code for network-optimized queries
                val params =
                    mutableMapOf<String, Any>(
                        "className" to className,
                        "networkQuality" to networkQuality.name,
                    )
                userId?.let { params["userId"] = it }

                val cloudResult = ParseCloud.callFunction<Map<String, Any>>("getOptimizedQuery", params)

                // Safe casting with null checks
                @Suppress("UNCHECKED_CAST")
                val results = (cloudResult["results"] as? List<ParseObject>) ?: emptyList()
                val appliedLimit = cloudResult["appliedLimit"] as? Int ?: 20

                // Record performance metrics
                PerformanceMonitor.recordNetworkUsage("query_$className", results.size * 1024L)
                PerformanceMonitor.endTimer("query_$className", startTime)

                QueryResult(
                    data = results,
                    networkQuality = networkQuality,
                    responseTime = System.currentTimeMillis() - startTime,
                    appliedLimit = appliedLimit,
                    fromCache = false,
                    success = true,
                )
            } catch (e: Exception) {
                // Fallback to local query with caching
                executeLocalQuery(className, networkQuality, customQuery, startTime)
            }
        }

    /**
     * Fallback local query with intelligent caching
     */
    private suspend fun executeLocalQuery(
        className: String,
        networkQuality: NetworkQualityLevel,
        customQuery: ((ParseQuery<ParseObject>) -> Unit)?,
        startTime: Long,
    ): QueryResult =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery<ParseObject>(className)

                // Apply network-based optimizations
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
                        query.limit = 5
                        query.cachePolicy = ParseQuery.CachePolicy.CACHE_ONLY
                    }
                }

                // Apply custom query modifications
                customQuery?.invoke(query)

                // Enable local datastore for offline capability
                query.fromLocalDatastore()

                val results = query.find()

                // Record performance metrics
                PerformanceMonitor.recordNetworkUsage("local_query_$className", results.size * 1024L)
                PerformanceMonitor.endTimer("local_query_$className", startTime)

                QueryResult(
                    data = results,
                    networkQuality = networkQuality,
                    responseTime = System.currentTimeMillis() - startTime,
                    appliedLimit = query.limit,
                    fromCache = query.cachePolicy != ParseQuery.CachePolicy.NETWORK_ELSE_CACHE,
                    success = true,
                )
            } catch (e: Exception) {
                QueryResult(
                    data = emptyList(),
                    networkQuality = networkQuality,
                    responseTime = System.currentTimeMillis() - startTime,
                    appliedLimit = 0,
                    fromCache = false,
                    success = false,
                    error = e.message,
                )
            }
        }

    /**
     * Get performance metrics from Parse Cloud Code
     */
    suspend fun getPerformanceMetrics(): PerformanceMetrics? =
        withContext(Dispatchers.IO) {
            try {
                val cloudResult =
                    ParseCloud.callFunction<HashMap<String, Any>>(
                        "getPerformanceMetrics",
                        HashMap<String, Any>(),
                    )
                PerformanceMetrics(
                    transferRequestCount = cloudResult["transferRequestCount"] as? Int ?: 0,
                    activeChatCount = cloudResult["activeChatCount"] as? Int ?: 0,
                    activeListingCount = cloudResult["activeListingCount"] as? Int ?: 0,
                    dbConnectionStatus = cloudResult["dbConnectionStatus"] as? String ?: "unknown",
                    indexingStatus = cloudResult["indexingStatus"] as? String ?: "unknown",
                    timestamp = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Execute batch queries with network optimization
     */
    suspend fun executeBatchQueries(queries: List<BatchQuery>): List<QueryResult> =
        withContext(Dispatchers.IO) {
            val networkQuality = networkQualityManager.getCurrentNetworkQuality()
            val batchSize =
                when (networkQuality) {
                    NetworkQualityLevel.EXCELLENT, NetworkQualityLevel.GOOD -> queries.size
                    NetworkQualityLevel.FAIR -> minOf(queries.size, 3)
                    NetworkQualityLevel.POOR, NetworkQualityLevel.OFFLINE -> minOf(queries.size, 2)
                }

            queries.take(batchSize).map { batchQuery ->
                executeOptimizedQuery(
                    className = batchQuery.className,
                    userId = batchQuery.userId,
                    customQuery = batchQuery.customQuery as? ((ParseQuery<ParseObject>) -> Unit),
                )
            }
        }
}

/**
 * Data classes for query results and performance monitoring
 */
data class QueryResult(
    val data: List<ParseObject>,
    val networkQuality: NetworkQualityLevel,
    val responseTime: Long,
    val appliedLimit: Int,
    val fromCache: Boolean,
    val success: Boolean,
    val error: String? = null,
)

data class PerformanceMetrics(
    val transferRequestCount: Int,
    val activeChatCount: Int,
    val activeListingCount: Int,
    val dbConnectionStatus: String,
    val indexingStatus: String,
    val timestamp: Long,
)

data class BatchQuery(
    val className: String,
    val userId: String? = null,
    val customQuery: ((ParseQuery<*>) -> Unit)? = null,
)
