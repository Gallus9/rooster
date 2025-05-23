package com.example.rooster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
fun CommunityFeedScreen() {
    var newPostContent by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf(listOf<ParseObject>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fetch posts on screen load
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            posts = fetchPosts()
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicTextField(
                value = newPostContent,
                onValueChange = { newPostContent = it },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                decorationBox = { innerTextField ->
                    if (newPostContent.isEmpty()) {
                        Text("What's on your mind?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()
                },
            )
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (newPostContent.isNotEmpty()) {
                            isLoading = true
                            savePost(newPostContent, imageUri, context)
                            newPostContent = ""
                            imageUri = null
                            posts = fetchPosts()
                            isLoading = false
                        }
                    }
                },
                enabled = newPostContent.isNotEmpty() && !isLoading,
            ) {
                Text("Post")
            }
        }

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
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(posts) { post ->
                    val content = post.getString("content") ?: ""
                    val username = post.getParseUser("user")?.username ?: "Unknown"
                    val imageUrl = post.getParseFile("image")?.url
                    var likes by remember { mutableStateOf(post.getInt("likes")) }
                    var comments by remember { mutableStateOf<List<String>>(post.getList("comments") ?: emptyList()) }
                    var newComment by remember { mutableStateOf("") }

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = username, style = MaterialTheme.typography.labelMedium)
                            Text(text = content, style = MaterialTheme.typography.bodyLarge)
                            if (imageUrl != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(model = imageUrl, contentDescription = "Post image", modifier = Modifier.size(120.dp))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Likes: $likes", style = MaterialTheme.typography.labelSmall)
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            likes = incrementLikes(post)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Text("Like")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            comments.forEach { comment ->
                                Text(text = "- $comment", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BasicTextField(
                                    value = newComment,
                                    onValueChange = { newComment = it },
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    decorationBox = { innerTextField ->
                                        if (newComment.isEmpty()) {
                                            Text("Add a comment...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        innerTextField()
                                    },
                                )
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (newComment.isNotEmpty()) {
                                                comments = addComment(post, newComment)
                                                newComment = ""
                                            }
                                        }
                                    },
                                    enabled = newComment.isNotEmpty(),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Text("Send")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private suspend fun fetchPosts(): List<ParseObject> {
    val query = ParseQuery.getQuery<ParseObject>("Post")
    query.orderByDescending("createdAt")
    query.include("user")
    return try {
        query.find()
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun savePost(
    content: String,
    imageUri: Uri?,
    context: Context,
) {
    val post = ParseObject("Post")
    post.put("content", content)
    post.put("user", ParseUser.getCurrentUser())
    post.put("likes", 0)
    post.put("comments", emptyList<String>())
    if (imageUri != null) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val compressedBytes = compressImage(bitmap)
        if (compressedBytes != null) {
            val parseFile = ParseFile("post_image.jpg", compressedBytes)
            parseFile.saveInBackground(
                SaveCallback { e ->
                    if (e == null) {
                        post.put("image", parseFile)
                        post.saveInBackground(
                            SaveCallback { e2 ->
                                // Handle success
                            },
                        )
                    }
                },
            )
        }
    } else {
        post.saveInBackground(
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

private suspend fun incrementLikes(post: ParseObject): Int {
    val currentLikes = post.getInt("likes")
    val newLikes = currentLikes + 1
    post.put("likes", newLikes)
    try {
        post.save()
        return newLikes
    } catch (e: Exception) {
        // Handle error
        return currentLikes
    }
}

private suspend fun addComment(
    post: ParseObject,
    comment: String,
): List<String> {
    val currentComments = post.getList<String>("comments") ?: emptyList()
    val updatedComments = currentComments + comment
    post.put("comments", updatedComments)
    try {
        post.save()
        return updatedComments
    } catch (e: Exception) {
        // Handle error
        return currentComments
    }
}
