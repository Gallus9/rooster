package com.example.rooster

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.parse.Parse

class App : Application() {
    companion object {
        lateinit var photoUploadDatabase: PhotoUploadDatabase
            private set

        // Public getter for the DAO instance
        fun getPhotoUploadDao(): PhotoUploadDao {
            if (!::photoUploadDatabase.isInitialized) {
                throw IllegalStateException("PhotoUploadDatabase not initialized. Ensure App.onCreate() is called.")
            }
            return photoUploadDatabase.photoUploadDao()
        }
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize Parse SDK
            Parse.initialize(
                Parse.Configuration.Builder(this)
                    .applicationId(getString(R.string.back4app_app_id))
                    .clientKey(getString(R.string.back4app_client_key))
                    .server(getString(R.string.back4app_server_url))
                    .build(),
            )
            Log.d("RoosterApp", "Parse SDK initialized successfully")
        } catch (e: Exception) {
            Log.e("RoosterApp", "Failed to initialize Parse SDK: ${e.message}", e)
            // Don't crash the app, just log the error
        }

        try {
            // Initialize Room database
            photoUploadDatabase =
                Room.databaseBuilder(
                    applicationContext,
                    PhotoUploadDatabase::class.java,
                    "rooster_photo_uploads.db",
                ).fallbackToDestructiveMigration().build()
            Log.d("RoosterApp", "Room database initialized successfully")
        } catch (e: Exception) {
            Log.e("RoosterApp", "Failed to initialize Room database: ${e.message}", e)
            // Create a fallback database to prevent crashes
            try {
                photoUploadDatabase =
                    Room.inMemoryDatabaseBuilder(
                        applicationContext,
                        PhotoUploadDatabase::class.java,
                    ).build()
                Log.d("RoosterApp", "Fallback in-memory database created")
            } catch (fallbackError: Exception) {
                Log.e(
                    "RoosterApp",
                    "Failed to create fallback database: ${fallbackError.message}",
                    fallbackError,
                )
            }
        }
    }
}
