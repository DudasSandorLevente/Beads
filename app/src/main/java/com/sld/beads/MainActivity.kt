package com.sld.beads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sld.beads.data.BeadsDatabase
import com.sld.beads.data.ImageRepository
import com.sld.beads.ui.theme.BeadsTheme
import com.sld.beads.ui.view.CreateView
import com.sld.beads.ui.view.ListView
import com.sld.beads.ui.view.WorkingView

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
