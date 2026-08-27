package com.sld.beads.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ImageRecord::class], version = 1, exportSchema = false)
abstract class BeadsDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        @Volatile
        private var instance: BeadsDatabase? = null

        fun getInstance(context: Context): BeadsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BeadsDatabase::class.java,
                    "beads.db"
                ).build().also { instance = it }
            }
    }
}
