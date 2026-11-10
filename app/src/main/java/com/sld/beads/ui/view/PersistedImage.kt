package com.sld.beads.ui.view

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.CancellationException

internal data class ImageCrop(val x: Int, val y: Int, val width: Int, val height: Int)

@Composable
internal fun PersistedImage(
    path: String, modifier: Modifier = Modifier, fitWidth: Boolean = false, crop: ImageCrop? = null
) {

    var previewWidthPx by remember {
        mutableIntStateOf(0)
    }
    var croppedBitmap by remember(path, crop) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(path, crop, previewWidthPx) {
        croppedBitmap = null
        if (previewWidthPx <= 0) return@LaunchedEffect
        croppedBitmap = withContext(Dispatchers.IO) {
            if (crop != null) decodeCroppedPreview(path, crop, previewWidthPx)
            else decodeFullPreview(path, previewWidthPx)
        }
    }

    AndroidView(
        factory = { context -> ImageView(context) },
        update = { imageView ->

            //imageView.adjustViewBounds = fitWidth || crop != null
            imageView.adjustViewBounds = false


            imageView.scaleType = if (fitWidth || crop != null) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }
            imageView.setImageBitmap(croppedBitmap)

        },
        modifier = modifier.onSizeChanged { size ->
            previewWidthPx = size.width
        }
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
    }.getOrElse { exception ->
        if (exception is CancellationException) throw exception
        null
    }

private fun decodeFullPreview(
    path: String, targetWidth: Int = 1080
): android.graphics.Bitmap? = runCatching {

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    var sampleSize = 1
    BitmapFactory.decodeFile(path, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0)
    while (bounds.outWidth / (sampleSize * 2L) >= targetWidth) { sampleSize *= 2 }

    BitmapFactory.decodeFile(
        path, BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )

}.getOrElse { exception ->
    if (exception is CancellationException) throw exception
    null
}