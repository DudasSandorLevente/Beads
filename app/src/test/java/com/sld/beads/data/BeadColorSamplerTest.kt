package com.sld.beads.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeadColorSamplerTest {
    @Test
    fun evenlyDivisibleGrid_samplesExactCellCenters() {
        val sampledPoints = mutableListOf<Pair<Int, Int>>()

        val result = sampleBeadColumn(
            workingArea = WorkingArea(x = 0, y = 0, width = 100, height = 80),
            beadGridSize = BeadGridSize(width = 5, height = 4),
            currentColumn = 2
        ) { x, y ->
            sampledPoints += x to y
            y
        }

        assertEquals(listOf(50 to 10, 50 to 30, 50 to 50, 50 to 70), sampledPoints)
        assertEquals(listOf(10, 30, 50, 70), result.colors.map(BeadColor::argb))
    }

    @Test
    fun nonDivisibleGrid_usesFractionalCellsAndWorkingAreaOffset() {
        val points = mutableListOf<Pair<Int, Int>>()

        sampleBeadColumn(
            workingArea = WorkingArea(x = 10, y = 20, width = 10, height = 10),
            beadGridSize = BeadGridSize(width = 3, height = 3),
            currentColumn = 1
        ) { x, y ->
            points += x to y
            0
        }

        assertEquals(listOf(15 to 21, 15 to 25, 15 to 28), points)
    }

    @Test
    fun firstAndLastColumnsStayInsideWorkingArea() {
        val first = sampleBeadColumn(
            WorkingArea(x = 7, y = 11, width = 5, height = 5),
            BeadGridSize(width = 8, height = 8),
            currentColumn = 0
        ) { x, y -> x * 100 + y }
        val last = sampleBeadColumn(
            WorkingArea(x = 7, y = 11, width = 5, height = 5),
            BeadGridSize(width = 8, height = 8),
            currentColumn = 7
        ) { x, y -> x * 100 + y }

        (first.colors + last.colors).forEach { color ->
            assertTrue(color.centerX in 7..11)
            assertTrue(color.centerY in 11..15)
        }
        assertEquals(8, first.colors.size)
        assertEquals(8, last.colors.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCurrentColumn_isRejected() {
        sampleBeadColumn(
            WorkingArea(0, 0, 10, 10),
            BeadGridSize(2, 2),
            currentColumn = 2
        ) { _, _ -> 0 }
    }

    @Test
    fun visibleRange_centersAndShiftsAtEdges() {
        assertEquals(3..7, calculateVisibleColumnRange(5, 10, 5))
        assertEquals(0..4, calculateVisibleColumnRange(0, 10, 5))
        assertEquals(5..9, calculateVisibleColumnRange(9, 10, 5))
    }

    @Test
    fun visibleRange_neverExceedsTotalColumns() {
        assertEquals(0..2, calculateVisibleColumnRange(1, 3, 10))
    }

    @Test
    fun columnWindow_samplesColumnsInLeftToRightOrder() {
        val window = sampleBeadColumnWindow(
            workingArea = WorkingArea(0, 0, 100, 20),
            beadGridSize = BeadGridSize(10, 2),
            currentColumn = 5,
            capacity = 3
        ) { x, y -> x * 100 + y }

        assertEquals(5, window.currentColumn)
        assertEquals(listOf(4, 5, 6), window.columns.map(BeadColumn::column))
        assertEquals(listOf(4505, 5505, 6505), window.columns.map { it.colors.first().argb })
    }
}
