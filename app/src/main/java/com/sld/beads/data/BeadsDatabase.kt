package com.sld.beads.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ImageRecord::class], version = 2, exportSchema = false)
abstract class BeadsDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE images ADD COLUMN workingAreaX INTEGER")
                db.execSQL("ALTER TABLE images ADD COLUMN workingAreaY INTEGER")
                db.execSQL("ALTER TABLE images ADD COLUMN workingAreaWidth INTEGER")
                db.execSQL("ALTER TABLE images ADD COLUMN workingAreaHeight INTEGER")
                db.execSQL("ALTER TABLE images ADD COLUMN beadWidth INTEGER")
                db.execSQL("ALTER TABLE images ADD COLUMN beadHeight INTEGER")
            }
        }

        @Volatile
        private var instance: BeadsDatabase? = null

        fun getInstance(context: Context): BeadsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BeadsDatabase::class.java,
                    "beads.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
