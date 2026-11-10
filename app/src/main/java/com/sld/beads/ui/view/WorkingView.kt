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

internal data class BeadColumnLayout(val squareSize: Dp, val gap: Dp)

internal fun displayedColumnNumber(zeroBasedColumn: Int): Int = zeroBasedColumn + 1

internal fun calculateVisibleColumnCapacity(
    availableWidth: Dp,
    squareSize: Dp,
    gap: Dp,
    totalColumns: Int
): Int {
    if (totalColumns <= 0 || squareSize <= 0.dp) return 1
    val capacity = ((availableWidth.value + gap.value) / (squareSize.value + gap.value)).toInt()
    return capacity.coerceIn(1, totalColumns)
}

internal fun calculateBeadColumnLayout(
    availableWidth: Dp,
    availableHeight: Dp,
    beadCount: Int
): BeadColumnLayout {
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
fun WorkingView(
    imageId: String,
    viewModel: ImagePickerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.workingColumnState.collectAsState()
    val isChangingColumn by viewModel.isChangingWorkingColumn.collectAsState()
    var showNeighbors by rememberSaveable { mutableStateOf(false) }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showNeighbors,
                    onCheckedChange = { showNeighbors = it }
                )
                Text("Neighbors")
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
                                modifier = Modifier.alpha(
                                    if (beadColumn.column == window.currentColumn) 1f else 0.4f
                                ),
                                verticalArrangement = Arrangement.spacedBy(layout.gap),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                beadColumn.colors.forEach { beadColor ->
                                    Box(
                                        modifier = Modifier
                                            .size(layout.squareSize)
                                            .background(Color(beadColor.argb))
                                    )
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
