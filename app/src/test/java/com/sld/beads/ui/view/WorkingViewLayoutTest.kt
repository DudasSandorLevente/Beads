package com.sld.beads.ui.view

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkingViewLayoutTest {
    @Test
    fun shortColumn_isCappedAndCenteredByCaller() {
        val layout = calculateBeadColumnLayout(
            availableWidth = 200.dp,
            availableHeight = 500.dp,
            beadCount = 2
        )

        assertEquals(64.dp, layout.squareSize)
        assertEquals(2.dp, layout.gap)
    }

    @Test
    fun squareSize_isLimitedByAvailableWidth() {
        val layout = calculateBeadColumnLayout(
            availableWidth = 30.dp,
            availableHeight = 500.dp,
            beadCount = 4
        )

        assertEquals(30.dp, layout.squareSize)
    }

    @Test
    fun denseColumn_shrinksGapAndFitsAvailableHeight() {
        val availableHeight = 100.dp
        val beadCount = 100

        val layout = calculateBeadColumnLayout(
            availableWidth = 200.dp,
            availableHeight = availableHeight,
            beadCount = beadCount
        )

        assertTrue(layout.gap < 2.dp)
        val occupiedHeight = layout.squareSize * beadCount + layout.gap * (beadCount - 1)
        assertTrue(occupiedHeight <= availableHeight)
    }

    @Test
    fun emptyColumn_hasZeroSizeAndGap() {
        val layout = calculateBeadColumnLayout(100.dp, 100.dp, beadCount = 0)

        assertEquals(0.dp, layout.squareSize)
        assertEquals(0.dp, layout.gap)
    }

    @Test
    fun displayedColumnNumber_isOneBased() {
        assertEquals(1, displayedColumnNumber(0))
        assertEquals(10, displayedColumnNumber(9))
    }

    @Test
    fun visibleColumnCapacity_fitsCompleteColumnsAndCapsAtTotal() {
        assertEquals(
            5,
            calculateVisibleColumnCapacity(availableWidth = 208.dp, squareSize = 40.dp, gap = 2.dp, totalColumns = 10)
        )
        assertEquals(
            3,
            calculateVisibleColumnCapacity(availableWidth = 500.dp, squareSize = 40.dp, gap = 2.dp, totalColumns = 3)
        )
    }

    @Test
    fun visibleColumnCapacity_alwaysReturnsAtLeastOne() {
        assertEquals(
            1,
            calculateVisibleColumnCapacity(availableWidth = 1.dp, squareSize = 40.dp, gap = 2.dp, totalColumns = 10)
        )
    }
}
