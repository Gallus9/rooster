package com.example.roosterapp.data

import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser

object FowlRepository {
    suspend fun fetchFowls(): List<Triple<String, String, String?>> {
        val query = ParseQuery.getQuery<ParseObject>("Fowl")
        query.orderByDescending("createdAt")
        return try {
            val results = query.find()
            results.mapNotNull {
                val name = it.getString("name")
                val type = it.getString("type")
                val birthDate = it.getString("birthDate")
                if (name != null && type != null) Triple(name, type, birthDate) else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addFowl(
        name: String,
        type: String,
        birthDate: String,
    ) {
        val fowl = ParseObject("Fowl")
        fowl.put("name", name)
        fowl.put("type", type)
        fowl.put("birthDate", birthDate)
        fowl.put("owner", ParseUser.getCurrentUser())
        fowl.saveInBackground()
    }
}
