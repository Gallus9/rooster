package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser

@Composable
fun MarketplaceScreen() {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var listings by remember { mutableStateOf(listOf<ParseObject>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

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
        val listing = ParseObject("Listing")
        listing.put("title", title)
        listing.put("price", price)
        listing.put("owner", ParseUser.getCurrentUser())
        listing.saveInBackground { e ->
            if (e == null) {
                title = ""
                price = ""
                fetchListings()
            } else {
                error = e.localizedMessage ?: "Failed to add listing."
            }
        }
    }

    LaunchedEffect(Unit) { fetchListings() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Create Listing", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { addListing() }, enabled = title.isNotBlank() && price.isNotBlank()) {
            Text("Add Listing")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(listings) { listing ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Title: ${listing.getString("title")}")
                            Text("Price: ${listing.getString("price")}")
                            Text("Seller: ${listing.getParseUser("owner")?.username ?: "Unknown"}")
                        }
                    }
                }
            }
        }
    }
}

