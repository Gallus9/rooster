package com.example.roosterapp.data

import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser

object CommunityRepository {
    suspend fun fetchPosts(): List<Triple<String, String, String?>> {
        val query = ParseQuery.getQuery<ParseObject>("Post")
        query.orderByDescending("createdAt")
        return try {
            val results = query.find()
            results.mapNotNull {
                val content = it.getString("content")
                val user = it.getParseUser("user")?.username
                val image = it.getParseFile("image")?.url
                if (content != null && user != null) Triple(content, user, image) else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addPost(
        content: String,
        imageUrl: String? = null,
    ) {
        val post = ParseObject("Post")
        post.put("content", content)
        post.put("user", ParseUser.getCurrentUser())
        imageUrl?.let { post.put("image", it) }
        post.saveInBackground()
    }
}
