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
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    var userPosts by remember { mutableStateOf<List<ParseObject>>(emptyList()) }
    var userFowls by remember { mutableStateOf<List<ParseObject>>(emptyList()) }
    var userListings by remember { mutableStateOf<List<ParseObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = ParseUser.getCurrentUser()

    // Fetch user data on screen load
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            userPosts = fetchUserPosts()
            userFowls = fetchUserFowls()
            userListings = fetchUserListings()
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // User Info Section
        Text(
            text = "Profile: ${currentUser?.username ?: "Unknown User"}",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Role: ${currentUser?.getString("role") ?: "Not specified"}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Display Section
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Posts") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Fowls") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Listings") },
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn {
                        items(userPosts) { post ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Post: ${post.getString("content") ?: "No content"}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Total Posts: ${userPosts.size}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                1 -> {
                    LazyColumn {
                        items(userFowls) { fowl ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = "Fowl: ${fowl.getString("name") ?: "Unnamed"}", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Type: ${fowl.getString("type") ?: "Not specified"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "Birth Date: ${fowl.getString("birthDate") ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Total Fowls: ${userFowls.size}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                2 -> {
                    LazyColumn {
                        items(userListings) { listing ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Listing: ${listing.getString("title") ?: "Untitled"}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(text = "Price: ${listing.getDouble("price")}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Total Listings: ${userListings.size}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun fetchUserPosts(): List<ParseObject> {
    val query = ParseQuery.getQuery<ParseObject>("Post")
    query.whereEqualTo("user", ParseUser.getCurrentUser())
    query.orderByDescending("createdAt")
    query.limit = 10
    return try {
        query.find()
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun fetchUserFowls(): List<ParseObject> {
    val query = ParseQuery.getQuery<ParseObject>("Fowl")
    query.whereEqualTo("owner", ParseUser.getCurrentUser())
    query.orderByDescending("createdAt")
    query.limit = 10
    return try {
        query.find()
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun fetchUserListings(): List<ParseObject> {
    val query = ParseQuery.getQuery<ParseObject>("Listing")
    query.whereEqualTo("seller", ParseUser.getCurrentUser())
    query.orderByDescending("createdAt")
    query.limit = 10
    return try {
        query.find()
    } catch (e: Exception) {
        emptyList()
    }
}
