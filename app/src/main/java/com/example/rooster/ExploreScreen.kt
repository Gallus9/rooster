package com.example.rooster

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen() {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf(listOf<ExplorePost>()) }
    var showcases by remember { mutableStateOf(listOf<ExplorePost>()) }
    var promotions by remember { mutableStateOf(listOf<ExplorePost>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Fetch all sections on first load or when search changes
    LaunchedEffect(searchQuery) {
        isLoading = true
        error = null
        coroutineScope.launch {
            try {
                // Placeholder data for now
                posts = listOf(ExplorePost("Sample Post 1", "User1"), ExplorePost("Sample Post 2", "User2"))
                showcases = listOf(ExplorePost("Sample Showcase 1", "User3"), ExplorePost("Sample Showcase 2", "User4"))
                promotions = listOf(ExplorePost("Sample Promotion 1", "Offer1"), ExplorePost("Sample Promotion 2", "Offer2"))
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search (@, #, location, name, breed, color,") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
                        .padding(horizontal = 8.dp),
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                // Social Feed Section
                item {
                    Text(
                        text = "Social Feed",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(posts) { post ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "By: ${post.username}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Showcases Section
                item {
                    Text(
                        text = "Showcases",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                items(showcases) { showcase ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = showcase.content, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "By: ${showcase.username}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Promotions Section
                item {
                    Text(
                        text = "Promotions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                items(promotions) { promotion ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = promotion.content, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Offer: ${promotion.username}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ExplorePost(val content: String, val username: String)
