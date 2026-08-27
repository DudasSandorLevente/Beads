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
        SELECT * FROM images
        WHERE name IS NOT NULL
            AND TRIM(name) != ''
            AND workingAreaX IS NOT NULL
            AND workingAreaY IS NOT NULL
            AND workingAreaWidth IS NOT NULL
            AND workingAreaHeight IS NOT NULL
            AND beadWidth IS NOT NULL
            AND beadHeight IS NOT NULL
        ORDER BY createdAt DESC
        """
    )
    fun observeConfigured(): Flow<List<ImageRecord>>

    @Query(
        """
        DELETE FROM images
        WHERE id = :id
            AND (
                name IS NULL OR TRIM(name) = '' OR
                workingAreaX IS NULL OR workingAreaY IS NULL OR
                workingAreaWidth IS NULL OR workingAreaHeight IS NULL OR
                beadWidth IS NULL OR beadHeight IS NULL
            )
        """
    )
    suspend fun deleteIncompleteById(id: String): Int

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        """
        UPDATE images SET
            name = :name,
            workingAreaX = :workingAreaX,
            workingAreaY = :workingAreaY,
            workingAreaWidth = :workingAreaWidth,
            workingAreaHeight = :workingAreaHeight,
            beadWidth = :beadWidth,
            beadHeight = :beadHeight,
            currentColumn = 0
        WHERE id = :id
        """
    )
    suspend fun updateConfiguration(
        id: String,
        name: String,
        workingAreaX: Int,
        workingAreaY: Int,
        workingAreaWidth: Int,
        workingAreaHeight: Int,
        beadWidth: Int,
        beadHeight: Int
    ): Int

    @Query(
        """
        UPDATE images SET currentColumn = :currentColumn
        WHERE id = :id
            AND beadWidth IS NOT NULL
            AND :currentColumn >= 0
            AND :currentColumn < beadWidth
        """
    )
    suspend fun setCurrentColumn(id: String, currentColumn: Int): Int

    @Query(
        """
        UPDATE images SET currentColumn = currentColumn + 1
        WHERE id = :id
            AND beadWidth IS NOT NULL
            AND currentColumn < beadWidth - 1
        """
    )
    suspend fun advanceCurrentColumn(id: String): Int
}
