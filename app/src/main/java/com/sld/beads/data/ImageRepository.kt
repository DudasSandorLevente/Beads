package com.sld.beads.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class ImageRepository(
    private val context: Context,
    private val imageDao: ImageDao
) {
    suspend fun importImage(sourceUri: Uri): Result<ImageRecord> = withContext(Dispatchers.IO) {
        val sourceUriString = sourceUri.toString()
        imageDao.findBySourceUri(sourceUriString)?.let { return@withContext Result.success(it) }

        val id = UUID.randomUUID().toString()
        val imageDirectory = File(context.filesDir, "images")
        val destination = File(imageDirectory, id)

        try {
            if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
                throw IOException("Could not create the image directory")
            }

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destination.outputStream().use(input::copyTo)
            } ?: throw IOException("The selected image could not be opened")

            val record = ImageRecord(
                id = id,
                sourceUri = sourceUriString,
                localPath = destination.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            imageDao.insert(record)
            Result.success(record)
        } catch (error: CancellationException) {
            destination.delete()
            throw error
        } catch (error: Exception) {
            destination.delete()
            val existing = imageDao.findBySourceUri(sourceUriString)
            if (existing != null) Result.success(existing) else Result.failure(error)
        }
    }
}
