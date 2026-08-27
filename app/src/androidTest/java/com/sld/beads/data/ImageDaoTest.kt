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
        assertEquals(0, dao.findById("one")?.currentColumn)
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

    @Test
    fun updateConfiguration_updatesAllValuesOnSelectedRecord() = runBlocking {
        dao.insert(record(id = "one", sourceUri = "content://photos/one", createdAt = 1))

        val updatedRows = dao.updateConfiguration(
            id = "one",
            name = "My project",
            workingAreaX = 10,
            workingAreaY = 20,
            workingAreaWidth = 300,
            workingAreaHeight = 400,
            beadWidth = 30,
            beadHeight = 40
        )

        val updated = dao.findById("one")
        assertEquals(1, updatedRows)
        assertEquals("My project", updated?.name)
        assertEquals(10, updated?.workingAreaX)
        assertEquals(20, updated?.workingAreaY)
        assertEquals(300, updated?.workingAreaWidth)
        assertEquals(400, updated?.workingAreaHeight)
        assertEquals(30, updated?.beadWidth)
        assertEquals(40, updated?.beadHeight)
        assertEquals(0, updated?.currentColumn)
    }

    @Test
    fun currentColumn_canBeSetAndAdvancedWithinBeadWidth() = runBlocking {
        dao.insert(configuredRecord(id = "one", beadWidth = 10, currentColumn = 0))

        assertEquals(1, dao.setCurrentColumn("one", 8))
        assertEquals(1, dao.advanceCurrentColumn("one"))

        assertEquals(9, dao.findById("one")?.currentColumn)
        assertEquals(0, dao.advanceCurrentColumn("one"))
        assertEquals(9, dao.findById("one")?.currentColumn)
    }

    @Test
    fun currentColumn_rejectsNegativeOutOfRangeAndUnconfiguredValues() = runBlocking {
        dao.insert(configuredRecord(id = "configured", beadWidth = 10, currentColumn = 0))
        dao.insert(record(id = "unconfigured", sourceUri = "content://photos/unconfigured", createdAt = 2))

        assertEquals(0, dao.setCurrentColumn("configured", -1))
        assertEquals(0, dao.setCurrentColumn("configured", 10))
        assertEquals(0, dao.setCurrentColumn("unconfigured", 0))
        assertEquals(0, dao.findById("configured")?.currentColumn)
    }

    @Test
    fun savingConfiguration_resetsCurrentColumn() = runBlocking {
        dao.insert(configuredRecord(id = "one", beadWidth = 10, currentColumn = 5))

        dao.updateConfiguration("one", "Renamed project", 1, 2, 3, 4, 20, 30)

        assertEquals(0, dao.findById("one")?.currentColumn)
    }

    @Test
    fun observeConfigured_excludesIncompleteRecordsAndUsesNewestFirst() = runBlocking {
        dao.insert(record(id = "draft", sourceUri = "content://photos/draft", createdAt = 3))
        dao.insert(configuredRecord(id = "older", beadWidth = 10, currentColumn = 0, createdAt = 1))
        dao.insert(configuredRecord(id = "newer", beadWidth = 20, currentColumn = 0, createdAt = 2))

        assertEquals(
            listOf("newer", "older"),
            dao.observeConfigured().first().map(ImageRecord::id)
        )
    }

    @Test
    fun deleteIncompleteById_deletesDraftButPreservesConfiguredRecord() = runBlocking {
        dao.insert(record(id = "draft", sourceUri = "content://photos/draft", createdAt = 1))
        dao.insert(configuredRecord(id = "configured", beadWidth = 10, currentColumn = 0))

        assertEquals(1, dao.deleteIncompleteById("draft"))
        assertEquals(0, dao.deleteIncompleteById("configured"))
        assertEquals(null, dao.findById("draft"))
        assertNotNull(dao.findById("configured"))
    }

    @Test
    fun deleteById_deletesOnlyRequestedRecordAndIsIdempotent() = runBlocking {
        dao.insert(configuredRecord(id = "delete", beadWidth = 10, currentColumn = 0))
        dao.insert(configuredRecord(id = "keep", beadWidth = 10, currentColumn = 0))

        assertEquals(1, dao.deleteById("delete"))
        assertEquals(0, dao.deleteById("delete"))
        assertEquals(null, dao.findById("delete"))
        assertNotNull(dao.findById("keep"))
    }

    private fun record(id: String, sourceUri: String, createdAt: Long) = ImageRecord(
        id = id,
        sourceUri = sourceUri,
        localPath = "/images/$id",
        createdAt = createdAt
    )

    private fun configuredRecord(
        id: String,
        beadWidth: Int,
        currentColumn: Int,
        createdAt: Long = 1
    ) = ImageRecord(
        id = id,
        sourceUri = "content://photos/$id",
        localPath = "/images/$id",
        createdAt = createdAt,
        name = id,
        workingAreaX = 0,
        workingAreaY = 0,
        workingAreaWidth = 100,
        workingAreaHeight = 100,
        beadWidth = beadWidth,
        beadHeight = 10,
        currentColumn = currentColumn
    )
}
