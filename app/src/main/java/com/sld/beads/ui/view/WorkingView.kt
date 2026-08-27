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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sld.beads.ImagePickerViewModel
import com.sld.beads.WorkingColumnState

internal data class BeadColumnLayout(val squareSize: Dp, val gap: Dp)

internal fun displayedColumnNumber(zeroBasedColumn: Int): Int = zeroBasedColumn + 1

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
    LaunchedEffect(imageId) { viewModel.loadWorkingColumn(imageId) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Button(onClick = onBack) { Text("Back") }

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
                val colors = currentState.beadColumn.colors
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val layout = calculateBeadColumnLayout(
                        availableWidth = maxWidth,
                        availableHeight = maxHeight,
                        beadCount = colors.size
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(layout.gap),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        colors.forEach { beadColor ->
                            Box(
                                modifier = Modifier
                                    .size(layout.squareSize)
                                    .background(Color(beadColor.argb))
                            )
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
                            viewModel.changeWorkingColumn(imageId, currentState.beadColumn.column - 1)
                        },
                        enabled = !isChangingColumn && currentState.beadColumn.column > 0
                    ) {
                        Text("←")
                    }
                    Text(
                        "${displayedColumnNumber(currentState.beadColumn.column)}/${currentState.beadColumn.totalColumns}"
                    )
                    Button(
                        onClick = {
                            viewModel.changeWorkingColumn(imageId, currentState.beadColumn.column + 1)
                        },
                        enabled = !isChangingColumn &&
                            currentState.beadColumn.column < currentState.beadColumn.totalColumns - 1
                    ) {
                        Text("→")
                    }
                }
            }
        }
    }
}
