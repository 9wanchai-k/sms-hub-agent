package com.example.sms_hub_agent.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for SMS Hub Agent
 */
@Database(entities = [SmsHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsHistoryDao(): SmsHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_hub_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
