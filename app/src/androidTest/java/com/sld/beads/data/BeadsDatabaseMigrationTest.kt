package com.sld.beads.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BeadsDatabaseMigrationTest {
    private val databaseName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration1To2_preservesExistingImageAndAddsEmptyConfiguration() = runBlocking {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE images (
                    id TEXT NOT NULL PRIMARY KEY,
                    sourceUri TEXT NOT NULL,
                    localPath TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE UNIQUE INDEX index_images_sourceUri ON images(sourceUri)"
            )
            database.execSQL(
                "INSERT INTO images (id, sourceUri, localPath, createdAt) VALUES (?, ?, ?, ?)",
                arrayOf<Any>("old", "content://photos/old", "/images/old", 1L)
            )
            database.version = 1
        }

        val migrated = Room.databaseBuilder(context, BeadsDatabase::class.java, databaseName)
            .addMigrations(BeadsDatabase.MIGRATION_1_2)
            .build()
        try {
            val record = migrated.imageDao().findById("old")
            assertEquals("content://photos/old", record?.sourceUri)
            assertNull(record?.workingAreaX)
            assertNull(record?.workingAreaY)
            assertNull(record?.workingAreaWidth)
            assertNull(record?.workingAreaHeight)
            assertNull(record?.beadWidth)
            assertNull(record?.beadHeight)
        } finally {
            migrated.close()
        }
    }
}
