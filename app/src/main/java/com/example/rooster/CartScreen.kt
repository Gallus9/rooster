package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.launch

@Composable
fun CartScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var orderSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fetchCartItems(onResult = { cartItems = it }, onError = { error = it }, setLoading = { isLoading = it })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Cart", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (cartItems.isEmpty()) {
            Text(text = "Your cart is empty")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems, key = { it.objectId }) { item ->
                    CartItemCard(item, onOrder = { itemId ->
                        coroutineScope.launch {
                            placeOrder(itemId,
                                onSuccess = {
                                    orderSuccess = true
                                    fetchCartItems(onResult = { cartItems = it }, onError = { error = it }, setLoading = { isLoading = it })
                                },
                                onError = { error = it }
                            )
                        }
                    })
                }
            }
        }
        if (orderSuccess) {
            Text("Order placed successfully!", color = MaterialTheme.colorScheme.primary)
        }
        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}

data class CartItem(
    val objectId: String,
    val title: String,
    val price: Int,
    val seller: String
)

@Composable
fun CartItemCard(item: CartItem, onOrder: (String) -> Unit) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Title: ${item.title}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Price: ${item.price}")
            Text(text = "Seller: ${item.seller}")
            Button(
                onClick = { onOrder(item.objectId) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Place Order")
            }
        }
    }
}

fun fetchCartItems(
    onResult: (List<CartItem>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Cart")
        query.whereEqualTo("user", ParseUser.getCurrentUser())
        query.include("listing")
        query.include("listing.owner")
        query.findInBackground { results, e ->
            setLoading(false)
            if (e != null) {
                onError(e.localizedMessage)
            } else {
                val items = results?.mapNotNull {
                    val listing = it.getParseObject("listing") ?: return@mapNotNull null
                    val title = listing.getString("title") ?: return@mapNotNull null
                    val price = listing.getInt("price")
                    val seller = listing.getParseUser("owner")?.username ?: "Unknown"
                    CartItem(listing.objectId, title, price, seller)
                } ?: emptyList()
                onResult(items)
            }
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

fun placeOrder(
    listingId: String,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit
) {
    try {
        val order = ParseObject("Order")
        order.put("listing", ParseObject.createWithoutData("Listing", listingId))
        order.put("buyer", ParseUser.getCurrentUser())
        order.put("status", "pending")
        order.saveInBackground { e ->
            if (e != null) {
                onError(e.localizedMessage)
            } else {
                onSuccess()
            }
        }
    } catch (e: Exception) {
        onError(e.localizedMessage)
    }
}

