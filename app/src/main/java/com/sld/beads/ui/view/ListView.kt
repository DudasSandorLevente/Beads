package com.sld.beads.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sld.beads.ImagePickerViewModel
import com.sld.beads.data.ImageRecord

@Composable
fun ListView(
    viewModel: ImagePickerViewModel,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val images by viewModel.configuredImages.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Beads")
            Button(onClick = onNew) { Text("New") }
        }
        HorizontalDivider()

        if (images.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No saved bead projects yet")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(images, key = ImageRecord::id) { image ->
                    ImageListItem(image = image, onClick = { onOpen(image.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ImageListItem(image: ImageRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersistedImage(path = image.localPath, modifier = Modifier.size(88.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Grid: ${image.beadWidth} × ${image.beadHeight}")
            Text("Current column: ${image.currentColumn}")
        }
    }
}
