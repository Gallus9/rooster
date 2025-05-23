package com.example.rooster

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

object ParseRestClient {
    private const val BASE_URL = "https://parseapi.back4app.com/classes/GameScore"

    fun createGameScore(context: Context, score: Int, playerName: String, cheatMode: Boolean, onResult: (Boolean, String?) -> Unit) {
        val appId = context.getString(R.string.parse_app_id)
        val apiKey = context.getString(R.string.parse_rest_api_key)
        val client = OkHttpClient()
        val json = JSONObject()
        json.put("score", score)
        json.put("playerName", playerName)
        json.put("cheatMode", cheatMode)
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("X-Parse-Application-Id", appId)
            .addHeader("X-Parse-REST-API-Key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onResult(true, response.body?.string())
                } else {
                    onResult(false, response.body?.string())
                }
            }
        })
    }
}

