package com.sld.beads.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sld.beads.ImagePickerViewModel
import com.sld.beads.WorkingColumnState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.zIndex

internal data class BeadColumnLayout(val squareSize: Dp, val gap: Dp)

internal fun displayedColumnNumber(zeroBasedColumn: Int): Int = zeroBasedColumn + 1

internal fun calculateColorRuns(colors: List<Int>, minimumLength: Int = 5): Map<Int, Int> {
    require(minimumLength > 0)

    val runs = mutableMapOf<Int, Int>()
    var start = 0

    while (start < colors.size) {
        var end = start + 1

        while (end < colors.size && colors[end] == colors[start]) {
            end++
        }

        val length = end - start

        if (length >= minimumLength) {
            runs[start] = length
        }

        start = end
    }

    return runs
}

internal fun calculateVisibleColumnCapacity(
    availableWidth: Dp, squareSize: Dp, gap: Dp, totalColumns: Int
): Int {
    if (totalColumns <= 0 || squareSize <= 0.dp) return 1
    val capacity = ((availableWidth.value + gap.value) / (squareSize.value + gap.value)).toInt()
    return capacity.coerceIn(1, totalColumns)
}

internal fun calculateBeadColumnLayout(availableWidth: Dp, availableHeight: Dp, beadCount: Int): BeadColumnLayout {
    require(beadCount >= 0)
    if (beadCount == 0) return BeadColumnLayout(0.dp, 0.dp)

    val gap = if (beadCount == 1) {
        0.dp
    } else {
        minOf(2.dp, availableHeight / (beadCount * 2))
    }
    val totalGapHeight = gap * (beadCount - 1)
    val sizeFromHeight = (availableHeight - totalGapHeight) / beadCount
    val squareSize = minOf(sizeFromHeight, availableWidth, 64.dp).coerceAtLeast(0.dp)
    return BeadColumnLayout(squareSize = squareSize, gap = gap)
}

@Composable
private fun RunBubble(count: Int, modifier: Modifier = Modifier) {
    val bubbleColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier.size(width = 8.dp, height = 12.dp).align(Alignment.CenterStart)
        ) {
            val path = Path().apply {
                moveTo(0f, size.height / 2f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, bubbleColor, style = Fill)
        }

        Box(
            modifier = Modifier.padding(start = 7.dp).background(
                color = bubbleColor, shape = RoundedCornerShape(8.dp)
            ).padding(horizontal = 9.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                color = textColor,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun WorkingView(
    imageId: String, viewModel: ImagePickerViewModel,
    onBack: () -> Unit, modifier: Modifier = Modifier
) {
    val state by viewModel.workingColumnState.collectAsState()
    val isChangingColumn by viewModel.isChangingWorkingColumn.collectAsState()
    var showNeighbors by rememberSaveable { mutableStateOf(false) }
    var showRuns by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(imageId) { viewModel.loadWorkingColumn(imageId) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showNeighbors,
                        onCheckedChange = { showNeighbors = it }
                    )
                    Text("Neighbors")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showRuns,
                        onCheckedChange = { showRuns = it }
                    )
                    Text("Runs")
                }
            }
        }

        when (val currentState = state) {
            WorkingColumnState.Idle,
            WorkingColumnState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is WorkingColumnState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(currentState.message)
                }
            }
            is WorkingColumnState.Content -> {
                val window = currentState.window
                val rowCount = window.columns.firstOrNull()?.colors?.size ?: 0
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val layout = calculateBeadColumnLayout(
                        availableWidth = maxWidth,
                        availableHeight = maxHeight,
                        beadCount = rowCount
                    )
                    val horizontalGap = 2.dp
                    val visibleCapacity = calculateVisibleColumnCapacity(
                        availableWidth = maxWidth,
                        squareSize = layout.squareSize,
                        gap = horizontalGap,
                        totalColumns = window.totalColumns
                    )
                    LaunchedEffect(showNeighbors, visibleCapacity, imageId) {
                        viewModel.updateWorkingColumnCapacity(
                            imageId = imageId,
                            capacity = if (showNeighbors) visibleCapacity else 1
                        )
                    }
                    val displayedColumns = if (showNeighbors) {
                        window.columns
                    } else {
                        window.columns.filter { it.column == window.currentColumn }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displayedColumns.forEach { beadColumn ->
                            Column(
                                modifier = Modifier
                                    .zIndex(
                                        if (beadColumn.column == window.currentColumn) 1f else 0f
                                    )
                                    .alpha(
                                        if (beadColumn.column == window.currentColumn) 1f else 0.4f
                                    ),
                                verticalArrangement = Arrangement.spacedBy(layout.gap),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val runs = if (
                                    showRuns && beadColumn.column == window.currentColumn
                                ) {
                                    calculateColorRuns(beadColumn.colors.map { it.argb })
                                } else {
                                    emptyMap()
                                }
                                beadColumn.colors.forEachIndexed { index, beadColor ->
                                    Box(modifier = Modifier.size(layout.squareSize)) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(
                                                Color(beadColor.argb)
                                            )
                                        )
                                        runs[index]?.let { count ->
                                            RunBubble(
                                                count = count,
                                                modifier = Modifier.align(Alignment.TopEnd)
                                                    .offset(x = 12.dp, y = (-8).dp)
                                                    .wrapContentSize(unbounded = true)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (!isChangingColumn) {
                                viewModel.changeWorkingColumn(
                                    imageId, window.currentColumn - 1
                                )
                            }
                        },
                        enabled = window.currentColumn > 0
                    ) {
                        Text("←")
                    }
                    Text(
                        "${displayedColumnNumber(window.currentColumn)}/${window.totalColumns}"
                    )
                    Button(
                        onClick = {
                            if (!isChangingColumn) {
                                viewModel.changeWorkingColumn(
                                    imageId, window.currentColumn + 1
                                )
                            }
                        },
                        enabled = window.currentColumn < window.totalColumns - 1
                    ) {
                        Text("→")
                    }
                }
            }
        }
    }
}
