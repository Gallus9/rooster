package com.example.rooster

import com.parse.ParseAnalytics

object AnalyticsTracker {
    fun trackEvent(
        eventName: String,
        dimensions: Map<String, String>,
    ) {
        ParseAnalytics.trackEventInBackground(eventName, dimensions)
    }

    fun trackLogin(userRole: String) {
        trackEvent("user_login", mapOf("user_role" to userRole))
    }

    fun trackPostCreated(userRole: String) {
        trackEvent("post_created", mapOf("user_role" to userRole))
    }

    fun trackFowlAdded(userRole: String) {
        trackEvent("fowl_added", mapOf("user_role" to userRole))
    }

    fun trackListingCreated(userRole: String) {
        trackEvent("listing_created", mapOf("user_role" to userRole))
    }

    fun trackTransferVerified(userRole: String) {
        trackEvent("transfer_verified", mapOf("user_role" to userRole))
    }
}
