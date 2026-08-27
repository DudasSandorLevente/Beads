package com.sld.beads.data

import kotlin.math.floor

data class BeadColor(
    val row: Int,
    val column: Int,
    val centerX: Int,
    val centerY: Int,
    val argb: Int
)

data class BeadColumn(
    val column: Int,
    val totalColumns: Int,
    val colors: List<BeadColor>
)

fun sampleBeadColumn(
    workingArea: WorkingArea,
    beadGridSize: BeadGridSize,
    currentColumn: Int,
    pixelAt: (x: Int, y: Int) -> Int
): BeadColumn {
    require(workingArea.width > 0 && workingArea.height > 0) {
        "Working area dimensions must be greater than zero"
    }
    require(beadGridSize.width > 0 && beadGridSize.height > 0) {
        "Bead grid dimensions must be greater than zero"
    }
    require(currentColumn in 0 until beadGridSize.width) {
        "Current column must be between 0 and ${beadGridSize.width - 1}"
    }

    val cellWidth = workingArea.width.toDouble() / beadGridSize.width
    val cellHeight = workingArea.height.toDouble() / beadGridSize.height
    val lastX = workingArea.x + workingArea.width - 1
    val lastY = workingArea.y + workingArea.height - 1
    val centerX = (workingArea.x + floor((currentColumn + 0.5) * cellWidth).toInt())
        .coerceIn(workingArea.x, lastX)

    val colors = List(beadGridSize.height) { row ->
        val centerY = (workingArea.y + floor((row + 0.5) * cellHeight).toInt())
            .coerceIn(workingArea.y, lastY)
        BeadColor(
            row = row,
            column = currentColumn,
            centerX = centerX,
            centerY = centerY,
            argb = pixelAt(centerX, centerY)
        )
    }

    return BeadColumn(
        column = currentColumn,
        totalColumns = beadGridSize.width,
        colors = colors
    )
}
