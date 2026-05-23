package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.PhotoEntity
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.SyncState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GalleryViewModel,
    onPhotoClick: (PhotoEntity) -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by viewModel.photosState.collectAsState()
    val activeGroup by viewModel.familyGroupCode.collectAsState()
    val activeUser by viewModel.currentUploader.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    // Trigger seed or automatic setup on screen start
    LaunchedEffect(activeGroup) {
        viewModel.seedPresetsIfEmpty(context)
    }

    // Spin animation configuration when syncing
    val infiniteTransition = rememberInfiniteTransition(label = "SyncSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Spin"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Family Gallery",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Active family group",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeGroup,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Sync Button icon
                    IconButton(
                        onClick = { viewModel.syncNow(context, simulateNewUpload = false) },
                        modifier = Modifier
                            .testTag("sync_button")
                            .rotate(if (syncState is SyncState.Syncing) rotationAngle else 0f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Synchronize shared library"
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        ProfileAvatarCircle(name = activeUser, size = 32)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddPhotoClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("add_photo_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Share a photo",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Live Status Notification Bar
            AnimatedVisibility(
                visible = syncState !is SyncState.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = when (syncState) {
                        is SyncState.Syncing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        is SyncState.Success -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (syncState) {
                                is SyncState.Syncing -> Icons.Default.Sync
                                is SyncState.Success -> Icons.Default.CheckCircle
                                else -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = contentColorFor(backgroundColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(if (syncState is SyncState.Syncing) rotationAngle else 0f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (val state = syncState) {
                                is SyncState.Syncing -> "Automatic downloading family memory updates..."
                                is SyncState.Success -> state.message
                                is SyncState.Error -> state.error
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Spacer keeping consistent clean padding below header
            Spacer(modifier = Modifier.height(8.dp))

            // Photos Grid
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Empty cloud inbox",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Awaiting Family Moments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Sync' to load shared cloud images, or select 'Share memory' to publish your own!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.syncNow(context, simulateNewUpload = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Cloud Download")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("photos_grid")
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoGridCard(
                            photo = photo,
                            onCardClick = { onPhotoClick(photo) },
                            onFavoriteToggle = { viewModel.toggleFavorite(photo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridCard(
    photo: PhotoEntity,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatted = formatter.format(Date(photo.timestamp))

    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("photo_item_${photo.id}")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Photo Image loaded asynchronously via Coil
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (photo.localUri.startsWith("content://") || photo.localUri.startsWith("/")) Uri.parse(photo.localUri) else photo.localUri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topStart = 16.dp, topEnd = 16.dp))
            )

            // Category Label Pin Overlay
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = photo.category,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Favorite overlay
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (photo.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle favorite status",
                    tint = if (photo.isFavorite) Color.Red else Color.White
                )
            }

            // Dark gradient protection overlay at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            // Metadata text overlays inside the image
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = photo.caption.ifBlank { "Family memory" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        ProfileAvatarCircle(name = photo.uploadedBy, size = 11)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = photo.uploadedBy,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = timeFormatted,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileAvatarCircle(name: String, size: Int = 24, modifier: Modifier = Modifier) {
    val initials = name.trim().take(1).uppercase()
    
    // Choose beautiful colorful background palettes based on uploader names
    val avatarColor = remember(name) {
        val hash = kotlin.math.abs(name.hashCode())
        val hues = listOf(
            Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
            Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DB6AC),
            Color(0xFF81C784), Color(0xFFFFB74D), Color(0xFFFF8A65), Color(0xFFA1887F)
        )
        hues[hash % hues.size]
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(avatarColor)
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.45).sp
        )
    }
}

data class CategoryItem(val id: String, val icon: ImageVector)

@Composable
fun NameEntryDialog(onConfirm: (String) -> Unit) {
    var nameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Welcome to Family Gallery", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "What's your name? Your family will see this on every photo and comment you share.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("e.g. Sharan, Mom, Dad…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nameInput.isNotBlank()) onConfirm(nameInput.trim()) },
                enabled = nameInput.isNotBlank()
            ) {
                Text("Get Started")
            }
        }
    )
}
