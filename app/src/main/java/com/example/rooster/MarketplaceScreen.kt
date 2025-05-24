package com.example.rooster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.parse.SaveCallback
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
fun MarketplaceScreen() {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var listings by remember { mutableStateOf(listOf<ParseObject>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }
    val context = LocalContext.current

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
        if (imageUri != null) {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val compressedBytes = compressImage(bitmap)
            if (compressedBytes != null) {
                val parseFile = ParseFile("listing_image.jpg", compressedBytes)
                parseFile.saveInBackground(
                    SaveCallback { e ->
                        if (e == null) {
                            listing.put("image", parseFile)
                            listing.saveInBackground(
                                SaveCallback { e2 ->
                                    if (e2 == null) {
                                        title = ""
                                        price = ""
                                        imageUri = null
                                        fetchListings()
                                    } else {
                                        error = (e2 as? com.parse.ParseException)?.localizedMessage ?: "Failed to add listing."
                                    }
                                },
                            )
                        } else {
                            error = (e as? com.parse.ParseException)?.localizedMessage ?: "Failed to upload image."
                        }
                    },
                )
            }
        } else {
            listing.saveInBackground(
                SaveCallback { e ->
                    if (e == null) {
                        title = ""
                        price = ""
                        imageUri = null
                        fetchListings()
                    } else {
                        error = (e as? com.parse.ParseException)?.localizedMessage ?: "Failed to add listing."
                    }
                },
            )
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
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Pick Image")
            }
            imageUri?.let {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(model = it, contentDescription = "Selected image", modifier = Modifier.size(64.dp))
            }
        }
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
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Title: ${listing.getString("title")}")
                                Text("Price: ${listing.getString("price")}")
                                Text("Seller: ${listing.getParseUser("owner")?.username ?: "Unknown"}")
                                val imageUrl = listing.getParseFile("image")?.url
                                imageUrl?.let {
                                    AsyncImage(model = it, contentDescription = "Listing image", modifier = Modifier.height(120.dp).fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                // Bidding Section
                                BiddingSection(listingId = listing.objectId)
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MarketplaceScreenPreview() {
    // This preview uses default state; for richer previews, mock ParseObject data as needed
    MarketplaceScreen()
}

private suspend fun saveListing(
    title: String,
    price: String,
    imageUri: Uri?,
    context: Context,
) {
    val listing = ParseObject("Listing")
    listing.put("title", title)
    listing.put("price", price.toDoubleOrNull() ?: 0.0)
    listing.put("seller", ParseUser.getCurrentUser())
    if (imageUri != null) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val compressedBytes = compressImage(bitmap)
        if (compressedBytes != null) {
            val parseFile = ParseFile("listing_image.jpg", compressedBytes)
            parseFile.saveInBackground(
                SaveCallback { e ->
                    if (e == null) {
                        listing.put("image", parseFile)
                        listing.saveInBackground(
                            SaveCallback { e2 ->
                                // Handle success
                            },
                        )
                    }
                },
            )
        }
    } else {
        listing.saveInBackground(
            SaveCallback { e2 ->
                // Handle success
            },
        )
    }
}

private fun compressImage(bitmap: Bitmap): ByteArray? {
    val outputStream = ByteArrayOutputStream()
    val quality = 80 // 80% quality for JPEG compression
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}

@Composable
fun BiddingSection(listingId: String) {
    var bids by remember { mutableStateOf(listOf<ParseObject>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listingId) {
        fetchBids(
            listingId = listingId,
            onResult = { bids = it },
            onError = { error = it },
            setLoading = { isLoading = it }
        )
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Bids:", style = MaterialTheme.typography.titleSmall)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else if (bids.isEmpty()) {
                Text("No bids yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                bids.forEach { bid ->
                    Text("${bid.getParseUser("user")?.username ?: "User"}: ${bid.getString("amount")}", style = MaterialTheme.typography.bodySmall)
                }
            }
            error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
