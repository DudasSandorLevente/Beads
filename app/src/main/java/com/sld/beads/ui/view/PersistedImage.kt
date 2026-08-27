package com.sld.beads.ui.view

import android.net.Uri
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
internal fun PersistedImage(
    path: String,
    modifier: Modifier = Modifier,
    fitWidth: Boolean = false
) {
    val uri = Uri.fromFile(File(path))
    AndroidView(
        factory = { context ->
            ImageView(context)
        },
        update = { imageView ->
            imageView.adjustViewBounds = fitWidth
            imageView.scaleType = if (fitWidth) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }
            imageView.setImageURI(uri)
        },
        modifier = modifier
    )
}
