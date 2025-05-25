package com.example.rooster

import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser

// Fetch notifications/alerts for the current user
fun fetchNotifications(
    onResult: (List<ParseObject>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Notification")
        query.whereEqualTo("user", ParseUser.getCurrentUser())
        query.orderByDescending("createdAt")
        query.findInBackground { objects, e ->
            setLoading(false)
            if (e != null) onError(e.localizedMessage) else onResult(objects ?: emptyList())
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

// Fetch bids for a given listing
fun fetchBids(
    listingId: String,
    onResult: (List<ParseObject>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Bid")
        query.whereEqualTo("listingId", listingId)
        query.orderByDescending("createdAt")
        query.findInBackground { objects, e ->
            setLoading(false)
            if (e != null) onError(e.localizedMessage) else onResult(objects ?: emptyList())
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

// Fetch chat messages for a group or P2P chat
fun fetchChatMessages(
    chatId: String,
    onResult: (List<ParseObject>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("ChatMessage")
        query.whereEqualTo("chatId", chatId)
        query.orderByAscending("createdAt")
        query.findInBackground { objects, e ->
            setLoading(false)
            if (e != null) onError(e.localizedMessage) else onResult(objects ?: emptyList())
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

// Fetch health/medication records for a fowl
fun fetchHealthRecords(
    fowlId: String,
    onResult: (List<ParseObject>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("HealthRecord")
        query.whereEqualTo("fowlId", fowlId)
        query.orderByDescending("date")
        query.findInBackground { objects, e ->
            setLoading(false)
            if (e != null) onError(e.localizedMessage) else onResult(objects ?: emptyList())
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

// Fetch promotions for the Explore or Market screens
fun fetchPromotions(
    onResult: (List<ParseObject>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Promotion")
        query.orderByDescending("createdAt")
        query.findInBackground { objects, e ->
            setLoading(false)
            if (e != null) onError(e.localizedMessage) else onResult(objects ?: emptyList())
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}
