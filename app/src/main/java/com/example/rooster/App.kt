package com.example.rooster

import android.app.Application
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
        // Initialize Parse SDK (ensure this is done if not already)
        // Make sure R.string values are defined in your strings.xml or pass actual string values
        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id)) // Replace with actual ID from strings.xml or direct value
                .clientKey(getString(R.string.back4app_client_key)) // Replace with actual key
                .server(getString(R.string.back4app_server_url)) // Replace with actual URL
                .build(),
        )

        photoUploadDatabase =
            Room.databaseBuilder(
                applicationContext,
                PhotoUploadDatabase::class.java,
                "rooster_photo_uploads.db", // Consistent DB name
            ).fallbackToDestructiveMigration().build()
    }
}
