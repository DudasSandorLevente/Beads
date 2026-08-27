package com.sld.beads.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
    val deletingImageIds by viewModel.deletingImageIds.collectAsState()
    val deletionError by viewModel.deletionError.collectAsState()
    var pendingDeletion by remember { mutableStateOf<ImageRecord?>(null) }

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

        deletionError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

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
                    SwipeableImageListItem(
                        image = image,
                        isDeleting = image.id in deletingImageIds,
                        onClick = { onOpen(image.id) },
                        onDeleteRequest = { pendingDeletion = image }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDeletion?.let { image ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text("Delete project?") },
            text = { Text("This permanently deletes the project and its copied image.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeletion = null
                        viewModel.deleteImage(image.id)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletion = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SwipeableImageListItem(
    image: ImageRecord,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.35f }
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDeleteRequest()
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isDeleting,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        ImageListItem(image = image, onClick = onClick)
    }
}

@Composable
private fun ImageListItem(image: ImageRecord, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = image.name.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        PersistedImage(
            path = image.localPath,
            modifier = Modifier.fillMaxWidth(),
            fitWidth = true
        )
        Text(
            text = formatImageMetadata(image),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun formatImageMetadata(image: ImageRecord): String {
    val width = image.beadWidth?.toString() ?: "?"
    val height = image.beadHeight?.toString() ?: "?"
    return "Grid: $width×$height · Column: ${image.currentColumn + 1}/$width"
}
