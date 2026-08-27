package com.sld.beads.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "images",
    indices = [Index(value = ["sourceUri"], unique = true)]
)
data class ImageRecord(
    @PrimaryKey val id: String,
    val sourceUri: String,
    val localPath: String,
    val createdAt: Long,
    val workingAreaX: Int? = null,
    val workingAreaY: Int? = null,
    val workingAreaWidth: Int? = null,
    val workingAreaHeight: Int? = null,
    val beadWidth: Int? = null,
    val beadHeight: Int? = null,
    @ColumnInfo(defaultValue = "0") val currentColumn: Int = 0
)
