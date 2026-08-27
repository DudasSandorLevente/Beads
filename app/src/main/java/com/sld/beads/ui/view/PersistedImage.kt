package com.sld.beads.ui.view

import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

internal data class ImageCrop(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

private data class PersistedImageKey(
    val path: String,
    val crop: ImageCrop?
)

@Composable
internal fun PersistedImage(
    path: String,
    modifier: Modifier = Modifier,
    fitWidth: Boolean = false,
    crop: ImageCrop? = null
) {
    val uri = Uri.fromFile(File(path))
    AndroidView(
        factory = { context ->
            ImageView(context)
        },
        update = { imageView ->
            imageView.adjustViewBounds = fitWidth || crop != null
            imageView.scaleType = if (fitWidth || crop != null) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }

            val key = PersistedImageKey(path, crop)
            if (imageView.tag != key) {
                imageView.tag = key
                if (crop == null) {
                    imageView.setImageURI(uri)
                } else {
                    imageView.setImageBitmap(decodeCroppedPreview(path, crop, imageView.width))
                }
            }
        },
        modifier = modifier
    )
}

private fun decodeCroppedPreview(path: String, crop: ImageCrop, measuredWidth: Int) =
    runCatching {
        val decoder = BitmapRegionDecoder.newInstance(path, false)
        try {
            require(crop.x >= 0 && crop.y >= 0 && crop.width > 0 && crop.height > 0)
            require(crop.x.toLong() + crop.width <= decoder.width)
            require(crop.y.toLong() + crop.height <= decoder.height)

            val targetWidth = measuredWidth.takeIf { it > 0 } ?: 1080
            var sampleSize = 1
            while (crop.width / (sampleSize * 2) >= targetWidth) {
                sampleSize *= 2
            }
            decoder.decodeRegion(
                Rect(crop.x, crop.y, crop.x + crop.width, crop.y + crop.height),
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        } finally {
            decoder.recycle()
        }
    }.getOrNull()
