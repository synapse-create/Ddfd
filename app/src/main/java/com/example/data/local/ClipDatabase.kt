package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.domain.models.ClipItem

@Database(entities = [ClipItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ClipDatabase : RoomDatabase() {
    abstract val clipDao: ClipDao

    companion object {
        @Volatile
        private var INSTANCE: ClipDatabase? = null

        fun getInstance(context: Context): ClipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClipDatabase::class.java,
                    "clipluz_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
