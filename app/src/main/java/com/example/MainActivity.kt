package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.PhotoRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NameEntryDialog
import com.example.ui.screens.PhotoDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UploadDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.GalleryViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Core clean architecture structural layers instantiations
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PhotoRepository(database.photoDao())
        val factory = GalleryViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[GalleryViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUploader by viewModel.currentUploader.collectAsState()

                if (currentUploader.isBlank()) {
                    NameEntryDialog { name -> viewModel.setCurrentUploader(name) }
                } else {
                    val navController = rememberNavController()
                    var isUploadDialogOpen by remember { mutableStateOf(false) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "gallery",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            // Main collaborative gallery homefeed view
                            composable("gallery") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onPhotoClick = { photo ->
                                        navController.navigate("detail/${photo.id}")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    },
                                    onAddPhotoClick = {
                                        isUploadDialogOpen = true
                                    }
                                )

                                // Show upload modal when active
                                if (isUploadDialogOpen) {
                                    UploadDialog(
                                        viewModel = viewModel,
                                        onDismiss = { isUploadDialogOpen = false }
                                    )
                                }
                            }

                            // Immersive photo details and comment thread view
                            composable(
                                route = "detail/{photoId}",
                                arguments = listOf(navArgument("photoId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
                                PhotoDetailScreen(
                                    viewModel = viewModel,
                                    photoId = photoId,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // App configuration control board
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
