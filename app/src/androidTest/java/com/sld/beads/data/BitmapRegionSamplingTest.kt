package com.sld.beads.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BitmapRegionSamplingTest {

    @Test
    fun regionSamplingMatchesFullBitmapSampling() {
        val source = Bitmap.createBitmap(
            120,
            80,
            Bitmap.Config.ARGB_8888
        )

        val file = File.createTempFile("beads-test-", ".png")

        try {
            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    source.setPixel(
                        x,
                        y,
                        android.graphics.Color.rgb(
                            x * 2,
                            y * 3,
                            (x + y) % 256
                        )
                    )
                }
            }

            file.outputStream().use {
                source.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val full = BitmapFactory.decodeFile(file.absolutePath)!!

            try {
                val workingArea = WorkingArea(
                    x = 10,
                    y = 5,
                    width = 100,
                    height = 60
                )
                val grid = BeadGridSize(width = 20, height = 12)
                val currentColumn = 10
                val capacity = 5

                val xRange = calculateVisibleSampleXRange(
                    workingArea,
                    grid,
                    currentColumn,
                    capacity
                )

                val region = Rect(
                    xRange.first,
                    workingArea.y,
                    xRange.last + 1,
                    workingArea.y + workingArea.height
                )

                val decoder = BitmapRegionDecoder.newInstance(
                    file.absolutePath,
                    false
                )

                val cropped = try {
                    decoder.decodeRegion(
                        region,
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inSampleSize = 1
                        }
                    )!!
                } finally {
                    decoder.recycle()
                }

                try {
                    val expected = sampleBeadColumnWindow(
                        workingArea,
                        grid,
                        currentColumn,
                        capacity,
                        pixelAt = full::getPixel
                    )

                    val actual = sampleBeadColumnWindow(
                        workingArea,
                        grid,
                        currentColumn,
                        capacity,
                        pixelAt = { x, y ->
                            cropped.getPixel(
                                x - region.left,
                                y - region.top
                            )
                        }
                    )

                    assertEquals(expected, actual)
                } finally {
                    cropped.recycle()
                }
            } finally {
                full.recycle()
            }
        } finally {
            source.recycle()
            file.delete()
        }
    }
}