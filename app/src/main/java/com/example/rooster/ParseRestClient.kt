/*
package com.example.rooster

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ParseRestClient {
    private const val PARSE_URL = "https://parseapi.back4app.com/classes/"
    private val client = OkHttpClient()

    suspend fun createGameScore(
        context: Context,
        score: Int,
        playerName: String,
        cheatMode: Boolean
    ): String? = suspendCancellableCoroutine { continuation ->
        val appId: String = try { context.getString(R.string.parse_app_id) } catch (e: Exception) { "YOUR_APP_ID" }
        val apiKey: String = try { context.getString(R.string.parse_rest_api_key) } catch (e: Exception) { "YOUR_REST_API_KEY" }

        val json = JSONObject()
        json.put("score", score)
        json.put("playerName", playerName)
        json.put("cheatMode", cheatMode)

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("${PARSE_URL}GameScore")
            .post(requestBody)
            .addHeader("X-Parse-Application-Id", appId)
            .addHeader("X-Parse-REST-API-Key", apiKey)
            .addHeader("Content-Type", "application/json")
            .build()

        val call = client.newCall(request)
        
        continuation.invokeOnCancellation { 
            call.cancel() 
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Ensure response body is closed after use
                response.use { resp -> 
                    if (continuation.isActive) {
                        if (resp.isSuccessful) {
                            continuation.resume(resp.body?.string())
                        } else {
                            continuation.resumeWithException(
                                IOException("Server error: ${resp.code} - ${resp.body?.string() ?: "No error body"}")
                            )
                        }
                    }
                }
            }
        })
    }

    suspend fun fetchGameScores(context: Context): String? = suspendCancellableCoroutine { continuation ->
        val appId: String = try { context.getString(R.string.parse_app_id) } catch (e: Exception) { "YOUR_APP_ID" }
        val apiKey: String = try { context.getString(R.string.parse_rest_api_key) } catch (e: Exception) { "YOUR_REST_API_KEY" }

        val request = Request.Builder()
            .url("${PARSE_URL}GameScore")
            .get()
            .addHeader("X-Parse-Application-Id", appId)
            .addHeader("X-Parse-REST-API-Key", apiKey)
            .build()

        val call = client.newCall(request)

        continuation.invokeOnCancellation { 
            call.cancel() 
        }
        
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                 if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Ensure response body is closed after use
                response.use { resp ->
                     if (continuation.isActive) {
                        if (resp.isSuccessful) {
                            continuation.resume(resp.body?.string())
                        } else {
                             continuation.resumeWithException(
                                IOException("Server error: ${resp.code} - ${resp.body?.string() ?: "No error body"}")
                            )
                        }
                    }
                }
            }
        })
    }
}
*/
