package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUri: String = "",
    val remoteUrl: String = "",
    val familyGroup: String = "DefaultHome",
    val uploadedBy: String = "Dad",
    val timestamp: Long = System.currentTimeMillis(),
    val caption: String = "",
    val category: String = "General",
    val isUploaded: Boolean = false,
    val isFavorite: Boolean = false,
    val likesCount: Int = 0,
    val commentCount: Int = 0,
    val deviceFileName: String = ""
)
