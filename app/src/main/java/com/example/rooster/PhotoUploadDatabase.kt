package com.example.rooster

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhotoUploadEntity::class],
    version = 1,
    exportSchema = false, // Recommended for libraries, true for apps for schema history
)
abstract class PhotoUploadDatabase : RoomDatabase() {
    abstract fun photoUploadDao(): PhotoUploadDao

    companion object {
        @Volatile
        private var INSTANCE: PhotoUploadDatabase? = null

        fun getDatabase(context: Context): PhotoUploadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        PhotoUploadDatabase::class.java,
                        "rooster_photo_uploads.db", // More descriptive name
                    )
                        .fallbackToDestructiveMigration() // Handle migrations simply for now
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
