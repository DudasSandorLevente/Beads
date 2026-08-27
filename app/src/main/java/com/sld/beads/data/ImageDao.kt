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
}
