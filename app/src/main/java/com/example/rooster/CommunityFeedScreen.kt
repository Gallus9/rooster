package com.example.rooster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
fun CommunityFeedScreen() {
    val context = LocalContext.current
    var postInput by remember { mutableStateOf("") }
    var commentInput by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val postsState = remember { mutableStateOf(listOf<PostData>()) }
    val isLoadingState = remember { mutableStateOf(false) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            fetchPosts(postsState, isLoadingState, errorState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = postInput,
                    onValueChange = { postInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("What's on your mind?") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { launcher.launch("image/*") }) {
                    Text("Pick Image")
                }
            }
            imageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Selected image",
                    modifier = Modifier.size(100.dp),
                )
            }
            Button(onClick = {
                if (postInput.isBlank()) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Please enter a post content")
                    }
                    return@Button
                }
                coroutineScope.launch {
                    try {
                        val parseObject = ParseObject("Post")
                        parseObject.put("content", postInput)
                        parseObject.put("user", ParseUser.getCurrentUser() ?: throw IllegalStateException("User not logged in"))
                        parseObject.put("likes", 0)
                        parseObject.put("comments", mutableListOf<String>())
                        imageUri?.let { uri ->
                            val compressedImage = compressImage(context, uri)
                            if (compressedImage != null) {
                                val parseFile = ParseFile("post_image.jpg", compressedImage)
                                parseFile.save()
                                parseObject.put("image", parseFile)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Failed to compress image")
                                }
                            }
                        }
                        parseObject.save()
                        coroutineScope.launch {
                            fetchPosts(postsState, isLoadingState, errorState)
                        }
                        postInput = ""
                        imageUri = null
                        Toast.makeText(context, "Post submitted successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        errorState.value = e.toString()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Error: $e")
                        }
                    }
                }
            }) {
                Text("Submit")
            }
            LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                items(postsState.value, key = { it.objectId }) { post ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                    ) {
                        PostCard(post, onLike = {
                            coroutineScope.launch {
                                try {
                                    val query = ParseQuery.getQuery<ParseObject>("Post")
                                    val parsePost = query.get(post.objectId)
                                    parsePost.put("likes", (parsePost.getInt("likes") ?: 0) + 1)
                                    parsePost.save()
                                    coroutineScope.launch {
                                        fetchPosts(postsState, isLoadingState, errorState)
                                    }
                                } catch (e: Exception) {
                                    errorState.value = e.toString()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error liking post: $e")
                                    }
                                }
                            }
                        }, onComment = { comment ->
                            coroutineScope.launch {
                                try {
                                    val query = ParseQuery.getQuery<ParseObject>("Post")
                                    val parsePost = query.get(post.objectId)
                                    val comments = parsePost.getList<String>("comments")?.toMutableList() ?: mutableListOf()
                                    comments.add(comment)
                                    parsePost.put("comments", comments)
                                    parsePost.save()
                                    coroutineScope.launch {
                                        fetchPosts(postsState, isLoadingState, errorState)
                                    }
                                } catch (e: Exception) {
                                    errorState.value = e.toString()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error adding comment: $e")
                                    }
                                }
                            }
                        })
                    }
                }
                if (isLoadingState.value) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: PostData,
    onLike: () -> Unit,
    onComment: (String) -> Unit,
) {
    var commentInput by remember { mutableStateOf("") }
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = post.username, style = MaterialTheme.typography.labelMedium)
            Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
            post.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Post image",
                    modifier = Modifier.size(100.dp),
                )
            }
            Row {
                Button(onClick = onLike) {
                    Text("Like (${post.likes})")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    label = { Text("Add Comment") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    if (commentInput.isNotBlank()) {
                        onComment(commentInput)
                        commentInput = ""
                    }
                }) {
                    Text("Comment")
                }
            }
            post.comments.forEach { comment ->
                Text(text = comment, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

data class PostData(
    val objectId: String,
    val content: String,
    val username: String,
    val imageUrl: String?,
    val likes: Int,
    val comments: List<String>,
)

fun fetchPosts(
    posts: MutableState<List<PostData>>,
    isLoading: MutableState<Boolean>,
    error: MutableState<String?>,
) {
    isLoading.value = true
    try {
        val query = ParseQuery.getQuery<ParseObject>("Post")
        query.setLimit(20)
        query.orderByDescending("createdAt")
        query.include("user")
        val results = query.find()
        posts.value =
            results.mapNotNull {
                val content = it.getString("content") ?: return@mapNotNull null
                val user = it.getParseUser("user")?.username ?: return@mapNotNull null
                val image = it.getParseFile("image")?.url
                val likes = it.getInt("likes") ?: 0
                val comments = it.getList<String>("comments") ?: emptyList()
                PostData(it.objectId, content, user, image, likes, comments)
            }
    } catch (e: Exception) {
        error.value = e.toString()
    } finally {
        isLoading.value = false
    }
}

fun compressImage(
    context: Context,
    uri: Uri,
): ByteArray? {
    var inputStream: InputStream? = null
    return try {
        inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e("ImageCompression", "Failed to open input stream for URI: $uri")
            return null
        }
        val bitmap = BitmapFactory.decodeStream(inputStream)
        if (bitmap == null) {
            Log.e("ImageCompression", "Failed to decode bitmap from URI: $uri")
            return null
        }
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        Log.e("ImageCompression", "Error compressing image", e)
        null
    } finally {
        try {
            inputStream?.close()
        } catch (e: Exception) {
            Log.e("ImageCompression", "Error closing input stream", e)
        }
    }
}
