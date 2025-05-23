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

@Composable
fun CommunityFeedScreen() {
    var postText by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf(listOf<ParseObject>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun fetchPosts() {
        loading = true
        val query = ParseQuery.getQuery<ParseObject>("Post")
        query.orderByDescending("createdAt")
        query.findInBackground { result, e ->
            loading = false
            if (e == null && result != null) {
                posts = result
            } else {
                error = e?.localizedMessage ?: "Failed to load posts."
            }
        }
    }

    LaunchedEffect(Unit) { fetchPosts() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = postText,
            onValueChange = { postText = it },
            label = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val post = ParseObject("Post")
                post.put("content", postText)
                post.put("username", ParseUser.getCurrentUser()?.username ?: "Unknown")
                post.saveInBackground { e ->
                    if (e == null) {
                        postText = ""
                        fetchPosts()
                    } else {
                        error = e.localizedMessage ?: "Failed to post."
                    }
                }
            },
            enabled = postText.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Post")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(posts) { post ->
                    PostCard(post = post, onLike = { /* Like logic placeholder */ })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PostCard(post: ParseObject, onLike: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = post.getString("username") ?: "Unknown", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = post.getString("content") ?: "", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onLike, modifier = Modifier.align(Alignment.End)) {
                Text("Like")
            }
        }
    }
}


