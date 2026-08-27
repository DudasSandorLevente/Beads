package com.sld.beads

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sld.beads.data.BeadsDatabase
import com.sld.beads.data.ConfigurationField
import com.sld.beads.data.ImageRecord
import com.sld.beads.data.ImageRepository
import com.sld.beads.ui.theme.BeadsTheme
import java.io.File

private const val LIST_ROUTE = "list"
private const val CREATE_ROUTE = "create"
private const val WORKING_ROUTE = "working/{imageId}"

private fun workingRoute(imageId: String) = "working/$imageId"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = BeadsDatabase.getInstance(applicationContext)
        val viewModelFactory = ImagePickerViewModelFactory(
            repository = ImageRepository(applicationContext, database.imageDao()),
            imageDao = database.imageDao()
        )
        setContent {
            BeadsTheme {
                val beadsViewModel: ImagePickerViewModel = viewModel(factory = viewModelFactory)
                BeadsApp(beadsViewModel)
            }
        }
    }
}

@Composable
private fun BeadsApp(viewModel: ImagePickerViewModel) {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LIST_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(LIST_ROUTE) {
                ListView(
                    viewModel = viewModel,
                    onNew = { navController.navigate(CREATE_ROUTE) },
                    onOpen = { imageId -> navController.navigate(workingRoute(imageId)) }
                )
            }
            composable(CREATE_ROUTE) {
                CreateView(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { imageId ->
                        navController.navigate(workingRoute(imageId)) {
                            popUpTo(CREATE_ROUTE) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = WORKING_ROUTE,
                arguments = listOf(navArgument("imageId") { type = NavType.StringType })
            ) { backStackEntry ->
                WorkingView(
                    imageId = backStackEntry.arguments?.getString("imageId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

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
        PersistedImage(
            path = image.localPath,
            modifier = Modifier.size(88.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Grid: ${image.beadWidth} × ${image.beadHeight}")
            Text("Current column: ${image.currentColumn}")
        }
    }
}

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
fun WorkingView(
    imageId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Button(onClick = onBack) { Text("Back") }
        Text("Working project: $imageId")
    }
}

@Composable
private fun PersistedImage(path: String, modifier: Modifier = Modifier) {
    val uri = Uri.fromFile(File(path))
    AndroidView(
        factory = { context ->
            ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        },
        update = { imageView -> imageView.setImageURI(uri) },
        modifier = modifier
    )
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
