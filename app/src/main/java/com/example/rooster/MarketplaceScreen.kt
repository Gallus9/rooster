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
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import java.io.InputStream
import com.parse.ParseFile
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.parse.SaveCallback

@Composable
fun MarketplaceScreen(navController: NavController? = null) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var listings by remember { mutableStateOf(listOf<ParseObject>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
            val bytes = inputStream?.readBytes()
            if (bytes != null) {
                val parseFile = ParseFile("listing_image.jpg", bytes)
                parseFile.saveInBackground(SaveCallback { e ->
                    if (e == null) {
                        listing.put("image", parseFile)
                        listing.saveInBackground(SaveCallback { e2 ->
                            if (e2 == null) {
                                title = ""
                                price = ""
                                imageUri = null
                                fetchListings()
                            } else {
                                error = (e2 as? com.parse.ParseException)?.localizedMessage ?: "Failed to add listing."
                            }
                        })
                    } else {
                        error = (e as? com.parse.ParseException)?.localizedMessage ?: "Failed to upload image."
                    }
                })
            }
        } else {
            listing.saveInBackground(SaveCallback { e ->
                if (e == null) {
                    title = ""
                    price = ""
                    imageUri = null
                    fetchListings()
                } else {
                    error = (e as? com.parse.ParseException)?.localizedMessage ?: "Failed to add listing."
                }
            })
        }
    }

    fun goToTransferVerification(orderId: String) {
        navController?.navigate("transferVerification/$orderId")
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
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Title: ${listing.getString("title")}")
                            Text("Price: ${listing.getString("price")}")
                            Text("Seller: ${listing.getParseUser("owner")?.username ?: "Unknown"}")
                            val imageUrl = listing.getParseFile("image")?.url
                            if (imageUrl != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(model = imageUrl, contentDescription = "Listing image", modifier = Modifier.size(120.dp))
                            }
                            // Button to verify transfer (simulate orderId for demo)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { goToTransferVerification(listing.objectId ?: "") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Verify Transfer")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarketplaceScreenPreview() {
    // This preview uses default state; for richer previews, mock ParseObject data as needed
    MarketplaceScreen()
}
