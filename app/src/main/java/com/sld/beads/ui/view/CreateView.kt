package com.sld.beads.ui.view

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sld.beads.ConfigurationFormState
import com.sld.beads.ImagePickerViewModel
import com.sld.beads.data.ConfigurationField

@Composable
fun CreateView(
    viewModel: ImagePickerViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedImage by viewModel.latestImage.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(Unit) { viewModel.startCreate() }
    LaunchedEffect(viewModel) { viewModel.savedImageIds.collect(onSaved) }
    BackHandler {
        viewModel.cancelCreate()
        onBack()
    }

    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let(viewModel::importImage)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Create project")
        Button(
            enabled = !isImporting,
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            }
        ) {
            Text(if (isImporting) "Importing..." else "Choose a photo")
        }

        errorMessage?.let { Text(it) }

        selectedImage?.let { image ->
            PersistedImage(
                path = image.localPath,
                modifier = Modifier.fillMaxWidth().size(280.dp)
            )
            Text("Image ID: ${image.id}")
            Text("Working area (pixels)")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IntegerField("X", ConfigurationField.WORKING_AREA_X, formState, viewModel, Modifier.weight(1f))
                IntegerField("Y", ConfigurationField.WORKING_AREA_Y, formState, viewModel, Modifier.weight(1f))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IntegerField("Width", ConfigurationField.WORKING_AREA_WIDTH, formState, viewModel, Modifier.weight(1f))
                IntegerField("Height", ConfigurationField.WORKING_AREA_HEIGHT, formState, viewModel, Modifier.weight(1f))
            }
            Text("Bead grid size")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IntegerField("Width", ConfigurationField.BEAD_WIDTH, formState, viewModel, Modifier.weight(1f))
                IntegerField("Height", ConfigurationField.BEAD_HEIGHT, formState, viewModel, Modifier.weight(1f))
            }
            Button(
                onClick = viewModel::saveConfiguration,
                enabled = !formState.isSaving
            ) {
                Text(if (formState.isSaving) "Saving..." else "Save configuration")
            }
        }
    }
}

@Composable
private fun IntegerField(
    label: String,
    field: ConfigurationField,
    state: ConfigurationFormState,
    viewModel: ImagePickerViewModel,
    modifier: Modifier = Modifier
) {
    val error = state.errors[field]
    OutlinedTextField(
        value = state.values[field].orEmpty(),
        onValueChange = { viewModel.updateConfigurationField(field, it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        modifier = modifier
    )
}
