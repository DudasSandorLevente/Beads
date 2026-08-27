package com.sld.beads.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageDaoTest {
    private lateinit var database: BeadsDatabase
    private lateinit var dao: ImageDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BeadsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.imageDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insert_canBeFoundByIdAndSourceUri() = runBlocking {
        val record = record(id = "one", sourceUri = "content://photos/one", createdAt = 1)

        dao.insert(record)

        assertEquals(record, dao.findById("one"))
        assertEquals(record, dao.findBySourceUri("content://photos/one"))
    }

    @Test
    fun observeAll_returnsNewestFirst() = runBlocking {
        dao.insert(record(id = "older", sourceUri = "content://photos/older", createdAt = 1))
        dao.insert(record(id = "newer", sourceUri = "content://photos/newer", createdAt = 2))

        assertEquals(listOf("newer", "older"), dao.observeAll().first().map(ImageRecord::id))
    }

    @Test
    fun sourceUri_isUnique() = runBlocking {
        dao.insert(record(id = "one", sourceUri = "content://photos/shared", createdAt = 1))

        val error = runCatching {
            dao.insert(record(id = "two", sourceUri = "content://photos/shared", createdAt = 2))
        }.exceptionOrNull()

        assertNotNull(error)
        assertEquals(listOf("one"), dao.observeAll().first().map(ImageRecord::id))
    }

    private fun record(id: String, sourceUri: String, createdAt: Long) = ImageRecord(
        id = id,
        sourceUri = sourceUri,
        localPath = "/images/$id",
        createdAt = createdAt
    )
}
