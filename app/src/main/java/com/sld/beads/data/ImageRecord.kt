package com.sld.beads.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "images",
    indices = [Index(value = ["sourceUri"], unique = true)]
)
data class ImageRecord(
    @PrimaryKey val id: String,
    val sourceUri: String,
    val localPath: String,
    val createdAt: Long
)
