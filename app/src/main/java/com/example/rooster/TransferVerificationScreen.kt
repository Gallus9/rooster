package com.example.rooster

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.parse.ParseCloud
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.launch

@Composable
fun TransferVerificationScreen(
    orderId: String,
    onVerified: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var order by remember { mutableStateOf<ParseObject?>(null) }
    var color by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        isLoading = true
        try {
            val query = ParseQuery.getQuery<ParseObject>("Order")
            query.include("listing")
            query.include("buyer")
            query.include("seller")
            order = query.get(orderId)
        } catch (e: Exception) {
            error = e.message
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Error fetching order: ${e.message}")
            }
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Transfer Verification", style = MaterialTheme.typography.headlineMedium)

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
                return@Column
            }

            order?.let { orderData ->
                val listing = orderData.getParseObject("listing")
                val buyer = orderData.getParseUser("buyer")
                val seller = orderData.getParseUser("seller")
                val price = orderData.getInt("price")

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(text = "Seller Info", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Username: ${seller?.username ?: "Unknown"}")
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(text = "Buyer Info", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Username: ${buyer?.username ?: "Unknown"}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Listing: ${listing?.getString("title") ?: "Unknown"}")
                Text(text = "Price: $price")

                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Confirm Color") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("Confirm Condition") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        if (color.isBlank() || condition.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please confirm color and condition")
                            }
                            return@Button
                        }

                        val currentUser = ParseUser.getCurrentUser()
                        if (currentUser == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please log in again")
                            }
                            return@Button
                        }

                        val params =
                            hashMapOf<String, Any>(
                                "orderId" to orderId,
                                "color" to color,
                                "condition" to condition,
                                "userId" to currentUser.objectId,
                            )

                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val result = ParseCloud.callFunction<String>("verifyTransfer", params)
                                orderData.put("status", "verified")
                                orderData.saveInBackground { e ->
                                    if (e == null) {
                                        onVerified()
                                        Toast.makeText(context, "Transfer verified: $result", Toast.LENGTH_SHORT).show()
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Error saving order: ${e.message}")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                error = e.message
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Error verifying transfer: ${e.message}")
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Confirm Transfer")
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
