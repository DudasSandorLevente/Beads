package com.sld.beads.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.sld.beads.ImagePickerViewModel
import com.sld.beads.WorkingColumnState
import com.sld.beads.data.BeadColor

@Composable
fun WorkingView(
    imageId: String,
    viewModel: ImagePickerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.workingColumnState.collectAsState()
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
                val column = currentState.beadColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(column.colors, key = BeadColor::row) { beadColor ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(beadColor.argb))
                        )
                    }
                }
            }
        }
    }
}
