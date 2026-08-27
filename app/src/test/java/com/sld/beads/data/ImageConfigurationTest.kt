package com.sld.beads.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageConfigurationTest {
    @Test
    fun rectangleExactlyAtImageBoundary_isValid() {
        val errors = validateConfiguration(
            workingArea = WorkingArea(x = 10, y = 20, width = 90, height = 80),
            beadGridSize = BeadGridSize(width = 30, height = 40),
            imageWidth = 100,
            imageHeight = 100
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun invalidOriginsSizesAndBeadCounts_areReported() {
        val errors = validateConfiguration(
            workingArea = WorkingArea(x = -1, y = -2, width = 0, height = -3),
            beadGridSize = BeadGridSize(width = 0, height = -1),
            imageWidth = 100,
            imageHeight = 100
        )

        assertEquals(ConfigurationField.entries.toSet(), errors.keys)
    }

    @Test
    fun rectangleOutsideImage_isReportedOnDimensions() {
        val errors = validateConfiguration(
            workingArea = WorkingArea(x = 90, y = 80, width = 11, height = 21),
            beadGridSize = BeadGridSize(width = 1, height = 1),
            imageWidth = 100,
            imageHeight = 100
        )

        assertEquals(
            setOf(ConfigurationField.WORKING_AREA_WIDTH, ConfigurationField.WORKING_AREA_HEIGHT),
            errors.keys
        )
    }
}
