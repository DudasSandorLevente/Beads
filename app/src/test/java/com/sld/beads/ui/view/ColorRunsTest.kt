package com.sld.beads.ui.view

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorRunsTest {

    @Test
    fun `returns empty map when colors are empty`() {
        assertEquals(
            emptyMap<Int, Int>(),
            calculateColorRuns(emptyList())
        )
    }

    @Test
    fun `ignores runs shorter than five`() {
        assertEquals(
            emptyMap<Int, Int>(),
            calculateColorRuns(listOf(1, 1, 1, 1, 2, 2, 2))
        )
    }

    @Test
    fun `counts the first bead in a run`() {
        assertEquals(
            mapOf(0 to 6),
            calculateColorRuns(listOf(1, 1, 1, 1, 1, 1))
        )
    }

    @Test
    fun `detects multiple runs and their starting positions`() {
        val colors = listOf(
            1, 1, 1, 1, 1, 1,
            2, 2, 2,
            3, 3, 3, 3, 3
        )

        assertEquals(
            mapOf(0 to 6, 9 to 5),
            calculateColorRuns(colors)
        )
    }

    @Test
    fun `separates runs of the same color when interrupted`() {
        val colors = listOf(
            1, 1, 1, 1, 1,
            2,
            1, 1, 1, 1, 1
        )

        assertEquals(
            mapOf(0 to 5, 6 to 5),
            calculateColorRuns(colors)
        )
    }

    @Test
    fun `supports a custom minimum run length`() {
        assertEquals(
            mapOf(0 to 3),
            calculateColorRuns(
                colors = listOf(1, 1, 1, 2, 2),
                minimumLength = 3
            )
        )
    }
}