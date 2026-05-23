package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PhotoEntity
import com.example.data.repository.PhotoRepository
import com.example.data.repository.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel(
    application: Application,
    private val repository: PhotoRepository
) : AndroidViewModel(application) {
    private val TAG = "GalleryViewModel"

    private val prefs = application.getSharedPreferences("fam_gall_prefs", Context.MODE_PRIVATE)

    // User settings states — loaded from persistent storage on every launch
    private val _currentUploader = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val currentUploader: StateFlow<String> = _currentUploader.asStateFlow()

    private val _familyGroupCode = MutableStateFlow("OurSweetHome")
    val familyGroupCode: StateFlow<String> = _familyGroupCode.asStateFlow()

    private val _firebaseDatabaseUrl = MutableStateFlow("https://gallery-ae69c-default-rtdb.firebaseio.com/")
    val firebaseDatabaseUrl: StateFlow<String> = _firebaseDatabaseUrl.asStateFlow()

    // Filter states
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Sync status indicators
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Combined Flow: Reactive state reflecting active group and category filters
    @OptIn(ExperimentalCoroutinesApi::class)
    val photosState: StateFlow<List<PhotoEntity>> = combine(
        _familyGroupCode,
        _selectedCategory,
        repository.allPhotos
    ) { group, category, allPhotos ->
        val groupFiltered = allPhotos.filter { it.familyGroup.lowercase() == group.lowercase() }
        if (category == "All") {
            groupFiltered
        } else {
            groupFiltered.filter { it.category.lowercase() == category.lowercase() }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setCurrentUploader(name: String) {
        _currentUploader.value = name
        prefs.edit().putString("user_name", name).apply()
    }

    fun setFamilyGroupCode(code: String) {
        _familyGroupCode.value = code
    }

    fun setFirebaseDatabaseUrl(url: String) {
        _firebaseDatabaseUrl.value = url.trim()
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun seedPresetsIfEmpty(context: Context) {
        viewModelScope.launch {
            if (_firebaseDatabaseUrl.value.isNotBlank() && _firebaseDatabaseUrl.value.startsWith("http")) {
                syncNow(context, simulateNewUpload = false)
            }
        }
    }

    /**
     * Executes manual sync, downloading metadata and binaries from remote or simulating auto-downloads.
     */
    fun syncNow(context: Context, simulateNewUpload: Boolean = false) {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                when (val result = repository.syncWithBackend(
                    context,
                    _firebaseDatabaseUrl.value,
                    _familyGroupCode.value,
                    simulateNewUpload
                )) {
                    is SyncResult.Success -> {
                        _syncState.value = SyncState.Success(result.message)
                    }
                    is SyncResult.Error -> {
                        _syncState.value = SyncState.Error(result.errorMessage)
                    }
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed mysteriously")
            }
        }
    }

    /**
     * Uploads local image URI to active family group backend database.
     */
    fun uploadNewPhoto(
        context: Context,
        caption: String,
        category: String,
        imageUri: Uri,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val success = repository.uploadPhotoToBackend(
                context = context,
                databaseUrl = _firebaseDatabaseUrl.value,
                familyGroup = _familyGroupCode.value,
                uploadedBy = _currentUploader.value,
                caption = caption,
                category = category,
                imageUri = imageUri
            )
            if (success) {
                _syncState.value = SyncState.Success("Photo shared to family board successfully!")
                onComplete(true)
            } else {
                _syncState.value = SyncState.Error("Upload failed. Verify internet and database setup.")
                onComplete(false)
            }
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }

    fun toggleFavorite(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.insertLocalPhoto(photo.copy(isFavorite = !photo.isFavorite))
        }
    }

    fun wipeDatabase() {
        viewModelScope.launch {
            repository.clearAllLocal()
            _syncState.value = SyncState.Success("Wiped local memory cache.")
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class GalleryViewModelFactory(
    private val application: Application,
    private val repository: PhotoRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
