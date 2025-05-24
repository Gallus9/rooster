package com.example.roosterapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.launch

@Composable
fun FarmerHomeScreen() {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var alerts by remember { mutableStateOf(listOf<Alert>()) }
    var products by remember { mutableStateOf(listOf<Product>()) }
    var socialPosts by remember { mutableStateOf(listOf<SocialPost>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            fetchAlerts(
                onResult = { alerts = it },
                onError = { error = it },
                setLoading = { isLoading = it }
            )
            fetchProducts(
                onResult = { products = it },
                onError = { error = it },
                setLoading = { isLoading = it }
            )
            fetchSocialPosts(
                onResult = { socialPosts = it },
                onError = { error = it },
                setLoading = { isLoading = it }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Home", style = MaterialTheme.typography.headlineMedium)

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
                return@Column
            }

            LazyColumn {
                // Rankings Section (Static for now)
                item {
                    Text(
                        text = "Rankings",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Top Seller: Farmer John", style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Sales: 150 units")
                            }
                        }
                    }
                }

                // Health Tips Section (Static for now)
                item {
                    Text(
                        text = "Health Tips",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Tip: Ensure regular vaccination", style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Keep coops clean to prevent disease.")
                            }
                        }
                    }
                }

                // Alerts Section
                item {
                    Text(
                        text = "Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(alerts) { alert ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = alert.message, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Date: ${alert.createdAt}")
                            }
                        }
                    }
                }

                // Products Section
                item {
                    Text(
                        text = "Your Products",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(products) { product ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = product.title, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Price: ${product.price}")
                            }
                        }
                    }
                }

                // Social Content Section
                item {
                    Text(
                        text = "Recent Social Posts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(socialPosts) { post ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Posted by: ${post.username}", style = MaterialTheme.typography.bodyLarge)
                                Text(text = post.content)
                            }
                        }
                    }
                }
            }

            error?.let {
                LaunchedEffect(it) {
                    snackbarHostState.showSnackbar("Error: $it")
                }
            }
        }
    }
}

data class Alert(val message: String, val createdAt: String)
data class Product(val title: String, val price: Int)
data class SocialPost(val username: String, val content: String)

suspend fun fetchAlerts(
    onResult: (List<Alert>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Notification")
        query.whereEqualTo("user", ParseUser.getCurrentUser())
        query.orderByDescending("createdAt")
        query.limit = 5
        val results = query.find()
        onResult(results.mapNotNull {
            val message = it.getString("message") ?: return@mapNotNull null
            val createdAt = it.createdAt?.toString() ?: return@mapNotNull null
            Alert(message, createdAt)
        })
    } catch (e: Exception) {
        onError(e.message)
    } finally {
        setLoading(false)
    }
}

suspend fun fetchProducts(
    onResult: (List<Product>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Listing")
        query.whereEqualTo("seller", ParseUser.getCurrentUser())
        query.orderByDescending("createdAt")
        query.limit = 5
        val results = query.find()
        onResult(results.mapNotNull {
            val title = it.getString("title") ?: return@mapNotNull null
            val price = it.getInt("price")
            Product(title, price)
        })
    } catch (e: Exception) {
        onError(e.message)
    } finally {
        setLoading(false)
    }
}

suspend fun fetchSocialPosts(
    onResult: (List<SocialPost>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Post")
        query.include("user")
        query.orderByDescending("createdAt")
        query.limit = 5
        val results = query.find()
        onResult(results.mapNotNull {
            val user = it.getParseUser("user") ?: return@mapNotNull null
            val username = user.getString("username") ?: return@mapNotNull null
            val content = it.getString("content") ?: return@mapNotNull null
            SocialPost(username, content)
        })
    } catch (e: Exception) {
        onError(e.message)
    } finally {
        setLoading(false)
    }
}

@Preview(showBackground = true)
@Composable
fun FarmerHomeScreenPreview() {
    FarmerHomeScreen()
}
