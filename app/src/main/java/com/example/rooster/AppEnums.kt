package com.example.rooster

// This file is the single source of truth for these enums.

enum class UploadStatus {
    PENDING,
    QUEUED,
    UPLOADING,
    COMPLETED,
    FAILED,
    RETRYING,
    CANCELLED,
    LINKING_FAILED,
}

enum class NetworkQualityLevel {
    EXCELLENT, // WiFi, fast mobile
    GOOD, // 4G with good signal
    FAIR, // 3G or weak 4G
    POOR, // 2G or very weak signal
    OFFLINE, // No connection
}

// Restoring constructor parameters as they are functionally necessary.
// The redeclaration issue is likely cache-related if this is the sole definition point.
enum class ImageCompressionLevel(val quality: Int, val maxDimension: Int) {
    ULTRA(20, 240), // 20% quality, max 240px
    HIGH(40, 480), // 40% quality, max 480px
    MEDIUM(60, 720), // 60% quality, max 720px
    LOW(80, 1080), // 80% quality, max 1080px
}

// Example of accessing properties if they were part of the enum constructor:
/*
enum class ImageCompressionLevelWithValue(val quality: Int, val maxDimension: Int) {
    ULTRA(20, 240),
    HIGH(40, 480),
    MEDIUM(60, 720),
    LOW(80, 1080)
}
val ultraQuality = ImageCompressionLevelWithValue.ULTRA.quality // Accessing a property
*/
