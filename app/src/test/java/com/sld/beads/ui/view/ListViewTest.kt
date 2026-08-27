package com.sld.beads.ui.view

import com.sld.beads.data.ImageRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class ListViewTest {
    @Test
    fun metadataContainsGridAndOneBasedColumnProgress() {
        val image = ImageRecord(
            id = "id",
            sourceUri = "content://image",
            localPath = "/images/id",
            createdAt = 1,
            beadWidth = 8,
            beadHeight = 8,
            currentColumn = 2
        )

        assertEquals("Grid: 8×8 · Column: 3/8", formatImageMetadata(image))
    }
}
