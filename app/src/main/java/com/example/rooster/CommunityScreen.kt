package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen() {
    val coroutineScope = rememberCoroutineScope()
    var posts by remember { mutableStateOf(listOf<CommunityPost>()) }
    var newPost by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        fetchCommunityPosts(onResult = { posts = it }, onError = { error = it }, setLoading = { isLoading = it })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Farmer Community", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newPost,
            onValueChange = { newPost = it },
            label = { Text("Share an update or event...") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    postCommunityUpdate(newPost,
                        onSuccess = {
                            newPost = ""
                            fetchCommunityPosts(onResult = { posts = it }, onError = { error = it }, setLoading = { isLoading = it })
                        },
                        onError = { error = it }
                    )
                }
            },
            enabled = newPost.isNotBlank(),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Post")
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (posts.isEmpty()) {
            Text("No community posts yet.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(posts, key = { it.objectId }) { post ->
                    CommunityPostCard(post)
                }
            }
        }
        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}

data class CommunityPost(val objectId: String, val content: String, val author: String, val createdAt: String)

@Composable
fun CommunityPostCard(post: CommunityPost) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
            Text(text = "By: ${post.author}", style = MaterialTheme.typography.labelSmall)
            Text(text = post.createdAt, style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun fetchCommunityPosts(
    onResult: (List<CommunityPost>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("CommunityPost")
        query.orderByDescending("createdAt")
        query.include("author")
        query.findInBackground { results, e ->
            setLoading(false)
            if (e != null) {
                onError(e.localizedMessage)
            } else {
                val items = results?.map {
                    CommunityPost(
                        objectId = it.objectId,
                        content = it.getString("content") ?: "",
                        author = it.getParseUser("author")?.username ?: "Unknown",
                        createdAt = it.createdAt?.toString() ?: ""
                    )
                } ?: emptyList()
                onResult(items)
            }
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

fun postCommunityUpdate(
    content: String,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit
) {
    try {
        val post = ParseObject("CommunityPost")
        post.put("content", content)
        post.put("author", ParseUser.getCurrentUser())
        post.saveInBackground { e ->
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

