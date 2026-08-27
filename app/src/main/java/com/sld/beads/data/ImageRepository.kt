package com.sld.beads.data

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
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

    suspend fun saveConfiguration(
        imageId: String,
        workingArea: WorkingArea,
        beadGridSize: BeadGridSize
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val record = imageDao.findById(imageId)
                ?: throw IllegalArgumentException("The selected image no longer exists")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(record.localPath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("Could not read the saved image dimensions")
            }

            val errors = validateConfiguration(
                workingArea = workingArea,
                beadGridSize = beadGridSize,
                imageWidth = bounds.outWidth,
                imageHeight = bounds.outHeight
            )
            if (errors.isNotEmpty()) throw ConfigurationValidationException(errors)

            val updatedRows = imageDao.updateConfiguration(
                id = imageId,
                workingAreaX = workingArea.x,
                workingAreaY = workingArea.y,
                workingAreaWidth = workingArea.width,
                workingAreaHeight = workingArea.height,
                beadWidth = beadGridSize.width,
                beadHeight = beadGridSize.height
            )
            if (updatedRows != 1) throw IOException("Could not update the image configuration")
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun setCurrentColumn(imageId: String, column: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val record = imageDao.findById(imageId)
                    ?: throw IllegalArgumentException("The selected image no longer exists")
                val beadWidth = record.beadWidth
                    ?: throw IllegalStateException("Save the bead grid size before changing columns")
                require(column in 0 until beadWidth) {
                    "Current column must be between 0 and ${beadWidth - 1}"
                }
                if (imageDao.setCurrentColumn(imageId, column) != 1) {
                    throw IOException("Could not update the current column")
                }
                Result.success(Unit)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun advanceCurrentColumn(imageId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val record = imageDao.findById(imageId)
                    ?: throw IllegalArgumentException("The selected image no longer exists")
                val beadWidth = record.beadWidth
                    ?: throw IllegalStateException("Save the bead grid size before changing columns")
                if (record.currentColumn >= beadWidth - 1) {
                    throw IllegalStateException("The image is already at its last column")
                }
                if (imageDao.advanceCurrentColumn(imageId) != 1) {
                    throw IOException("Could not advance the current column")
                }
                imageDao.findById(imageId)?.currentColumn
                    ?.let(Result.Companion::success)
                    ?: Result.failure(IOException("Could not read the updated current column"))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun discardIncompleteImage(imageId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val record = imageDao.findById(imageId) ?: return@withContext Result.success(Unit)
                if (imageDao.deleteIncompleteById(imageId) == 1) {
                    val imageFile = File(record.localPath)
                    if (imageFile.exists() && !imageFile.delete()) {
                        throw IOException("Could not delete the abandoned image")
                    }
                }
                Result.success(Unit)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun deleteImage(imageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val record = imageDao.findById(imageId) ?: return@withContext Result.success(Unit)
            if (imageDao.deleteById(imageId) == 0) return@withContext Result.success(Unit)

            val imageFile = File(record.localPath)
            if (imageFile.exists() && !imageFile.delete()) {
                throw IOException("Project deleted, but its image file could not be removed")
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun loadWorkingColumnColors(
        imageId: String,
        visibleColumnCapacity: Int
    ): Result<BeadColumnWindow> =
        withContext(Dispatchers.IO) {
            var bitmap: android.graphics.Bitmap? = null
            try {
                val record = imageDao.findById(imageId)
                    ?: throw IllegalArgumentException("The selected image no longer exists")
                val workingArea = WorkingArea(
                    x = record.workingAreaX ?: throw IllegalStateException("Working area is not configured"),
                    y = record.workingAreaY ?: throw IllegalStateException("Working area is not configured"),
                    width = record.workingAreaWidth ?: throw IllegalStateException("Working area is not configured"),
                    height = record.workingAreaHeight ?: throw IllegalStateException("Working area is not configured")
                )
                val beadGridSize = BeadGridSize(
                    width = record.beadWidth ?: throw IllegalStateException("Bead grid is not configured"),
                    height = record.beadHeight ?: throw IllegalStateException("Bead grid is not configured")
                )

                bitmap = BitmapFactory.decodeFile(
                    record.localPath,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                ) ?: throw IOException("Could not decode the saved image")

                if (workingArea.x < 0 || workingArea.y < 0 ||
                    workingArea.x.toLong() + workingArea.width > bitmap.width ||
                    workingArea.y.toLong() + workingArea.height > bitmap.height
                ) {
                    throw IllegalStateException("Working area is outside the saved image")
                }

                Result.success(
                    sampleBeadColumnWindow(
                        workingArea = workingArea,
                        beadGridSize = beadGridSize,
                        currentColumn = record.currentColumn,
                        capacity = visibleColumnCapacity,
                        pixelAt = bitmap::getPixel
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            } finally {
                bitmap?.recycle()
            }
        }
}
