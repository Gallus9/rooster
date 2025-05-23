package com.example.roosterapp.data

import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser

object MarketplaceRepository {
    suspend fun fetchListings(): List<Triple<String, Double, String?>> {
        val query = ParseQuery.getQuery<ParseObject>("Listing")
        query.orderByDescending("createdAt")
        return try {
            val results = query.find()
            results.mapNotNull {
                val title = it.getString("title")
                val price = it.getDouble("price")
                val seller = it.getParseUser("seller")?.username
                if (title != null && seller != null) Triple(title, price, seller) else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addListing(
        title: String,
        price: Double,
    ) {
        val listing = ParseObject("Listing")
        listing.put("title", title)
        listing.put("price", price)
        listing.put("seller", ParseUser.getCurrentUser())
        listing.saveInBackground()
    }
}
