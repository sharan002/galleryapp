package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUploader by viewModel.currentUploader.collectAsState()
    val familyGroupCode by viewModel.familyGroupCode.collectAsState()
    val firebaseDatabaseUrl by viewModel.firebaseDatabaseUrl.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var tempUrl by remember { mutableStateOf(firebaseDatabaseUrl) }
    var tempGroupCode by remember { mutableStateOf(familyGroupCode) }

    val scrollState = rememberScrollState()

    val familyProfiles = listOf("Sharan", "Mom", "Dad", "Grandpa", "Aunt Lisa", "Uncle Mark")

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Gallery Control Board",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back to Gallery"
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Family Profiles Identity
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "My Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Choose which family member you are posting as:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(familyProfiles) { profile ->
                        val isSelected = currentUploader == profile
                        Card(
                            onClick = { viewModel.setCurrentUploader(profile) },
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .width(100.dp)
                                .height(95.dp)
                                .testTag("profile_card_$profile")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ProfileAvatarCircle(name = profile, size = 36)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = profile,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section 2: Shared Group Code Mapping
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Family Space Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "To access the same shared folder with matching photos, make sure your family members enter this exact code too!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = tempGroupCode,
                    onValueChange = {
                        tempGroupCode = it
                        viewModel.setFamilyGroupCode(it)
                    },
                    leadingIcon = { Icon(Icons.Default.GroupWork, contentDescription = null) },
                    label = { Text("Family Shared Code") },
                    placeholder = { Text("e.g. OurSweetHome") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_code_input"),
                    singleLine = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section 3: Shared Backend Database Connection Configuration
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Real-Time Shared Backend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Provide a Firebase Realtime Database URL to enable physical cloud integration so anyone running this app can sync and auto-download photos together!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = {
                        tempUrl = it
                        viewModel.setFirebaseDatabaseUrl(it)
                    },
                    leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                    label = { Text("Firebase Database REST End Url") },
                    placeholder = { Text("https://your-proj-id-default-rtdb.firebaseio.com/") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("database_url_input"),
                    singleLine = true
                )

                // Instructional Guide Panel for Firebase
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = "How-to instructions",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Easy 1-Time Free Setup Instructions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val instructions = listOf(
                            "1. Go to console.firebase.google.com and click 'Add project' (free).",
                            "2. In the sidebar, click 'Realtime Database' under Build and select 'Create database' choosing free guidelines.",
                            "3. Go to the 'Rules' tab, set both '.read' and '.write' to 'true', then hit 'Publish' for access.",
                            "4. Copy your database URL from the 'Data' tab and paste it in the field above!",
                            "5. Done! You and your family now have real-time photo sharing active."
                        )
                        
                        instructions.forEach { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section 4: Maintenance Board
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Collaborative Live Simulations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Test how other family members' devices push images in real-time. Use these toggles to simulate new cloud updates falling safely onto this screen:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Simulate family upload button
                    Button(
                        onClick = { viewModel.syncNow(context, simulateNewUpload = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("simulate_upload_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Family Sync Sim")
                    }

                    // Reset local memory database
                    OutlinedButton(
                        onClick = { viewModel.wipeDatabase() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("wipe_data_btn")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Space")
                    }
                }
            }
        }
    }
}
