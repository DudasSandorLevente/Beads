package com.sld.beads.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Insert
    suspend fun insert(record: ImageRecord)

    @Query("SELECT * FROM images WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ImageRecord?

    @Query("SELECT * FROM images WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun findBySourceUri(sourceUri: String): ImageRecord?

    @Query("SELECT * FROM images ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ImageRecord>>

    @Query(
        """
        UPDATE images SET
            workingAreaX = :workingAreaX,
            workingAreaY = :workingAreaY,
            workingAreaWidth = :workingAreaWidth,
            workingAreaHeight = :workingAreaHeight,
            beadWidth = :beadWidth,
            beadHeight = :beadHeight
        WHERE id = :id
        """
    )
    suspend fun updateConfiguration(
        id: String,
        workingAreaX: Int,
        workingAreaY: Int,
        workingAreaWidth: Int,
        workingAreaHeight: Int,
        beadWidth: Int,
        beadHeight: Int
    ): Int
}
