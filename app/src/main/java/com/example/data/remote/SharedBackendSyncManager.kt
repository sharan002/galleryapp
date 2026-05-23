package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.local.PhotoEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object SharedBackendSyncManager {
    private const val TAG = "SyncManager"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Predefined Unsplash URLs representing typical wonderful family milestones
    // for seamless offline/simulated out-of-the-box fallback
    val PRESET_PHOTOS = listOf(
        PresetPhoto(
            "Family Picnic",
            "https://images.unsplash.com/photo-1543269865-cbf427effbad?auto=format&fit=crop&q=80&w=1000",
            "All", "Dad", "Sunny afternoon at the lakeside park!"
        ),
        PresetPhoto(
            "Sundown Dinner",
            "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&q=80&w=1000",
            "Events", "Mom", "Barbeque backyard party with grandpa and grandma."
        ),
        PresetPhoto(
            "Lola the Golden",
            "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&q=80&w=1000",
            "Pets", "Sharan", "Lola caught stealing cookies again!"
        ),
        PresetPhoto(
            "Morning Coffee Hike",
            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&q=80&w=1000",
            "Scenic", "Grandpa", "Chasing the sun early in the mountains."
        ),
        PresetPhoto(
            "Baking Sunday Bread",
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=1000",
            "Cooking", "Mom", "The kitchen smells incredible today!"
        ),
        PresetPhoto(
            "Graduation Milestone",
            "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&q=80&w=1000",
            "Events", "Dad", "So proud of this huge milestone!"
        ),
        PresetPhoto(
            "New Living Room Rug",
            "https://images.unsplash.com/photo-1484101403633-562f891dc89a?auto=format&fit=crop&q=80&w=1000",
            "General", "Grandma", "Cozy winter vibe is coming together."
        )
    )

    data class PresetPhoto(
        val caption: String,
        val url: String,
        val category: String,
        val uploader: String,
        val detail: String
    )

    /**
     * Converts a local Uri (picked file) into a compressed Base64 string
     * to perform a serverless free transfer directly inside Realtime Database.
     */
    fun compressAndEncodeUri(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream?.close()

            // Scale down to prevent exceeding free Firebase payloads
            val maxDimension = 640
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val (scaledWidth, scaledHeight) = if (originalBitmap.width > originalBitmap.height) {
                Pair(maxDimension, (maxDimension / ratio).toInt())
            } else {
                Pair((maxDimension * ratio).toInt(), maxDimension)
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            val outputStream = ByteArrayOutputStream()
            // Compress heavily to keep inside ~30-50KB
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteBytes = outputStream.toByteArray()
            
            Base64.encodeToString(byteBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            null
        }
    }

    /**
     * Decodes a Base64 string and saves it into the local cache directory as a shared photo.
     * Returns the local file absolute path.
     */
    fun decodeAndSaveBase64(context: Context, base64Str: String, fileName: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            val cacheDirectory = File(context.cacheDir, "downloaded_family_photos")
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs()
            }
            val targetFile = File(cacheDirectory, fileName)
            val fileOutputStream = FileOutputStream(targetFile)
            fileOutputStream.write(decodedBytes)
            fileOutputStream.flush()
            fileOutputStream.close()
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64 back to image file", e)
            null
        }
    }

    /**
     * Fetches photos from Firebase Realtime Database using the REST API.
     * Integrates cleanly with a dynamic URL configured on-the-fly.
     */
    suspend fun fetchRemotePhotos(databaseUrl: String, familyGroup: String): List<RemotePhotoDto> {
        val photos = mutableListOf<RemotePhotoDto>()
        if (databaseUrl.isBlank() || !databaseUrl.startsWith("http")) {
            return emptyList()
        }

        // Sanitizing base URL
        val sanitizedUrl = if (databaseUrl.endsWith("/")) databaseUrl else "$databaseUrl/"
        val requestUrl = "${sanitizedUrl}photos.json"

        try {
            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Firebase HTTP GET failed: Code ${response.code}")
                    return emptyList()
                }

                val bodyString = response.body?.string() ?: return emptyList()
                if (bodyString == "null" || bodyString.isBlank()) {
                    return emptyList()
                }

                val rootObject = JSONObject(bodyString)
                val keys = rootObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = rootObject.getJSONObject(key)
                    
                    val group = item.optString("familyGroup", "General")
                    // Filter by family group immediately on REST feed
                    if (group.lowercase() == familyGroup.lowercase()) {
                        photos.add(
                            RemotePhotoDto(
                                remoteServerId = key,
                                caption = item.optString("caption", ""),
                                category = item.optString("category", "General"),
                                uploadedBy = item.optString("uploadedBy", "Family Member"),
                                timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                familyGroup = group,
                                imageContentBase64 = item.optString("imageContentBase64", ""),
                                remoteUrl = item.optString("remoteUrl", ""),
                                isFavorite = item.optBoolean("isFavorite", false),
                                likesCount = item.optInt("likesCount", 0)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching remote photos via REST: ${e.message}")
        }
        return photos
    }

    /**
     * Syncs a local newly-added photo directly via REST POST payload.
     */
    suspend fun postRemotePhoto(
        databaseUrl: String,
        photo: PhotoEntity,
        base64String: String?
    ): String? {
        if (databaseUrl.isBlank() || !databaseUrl.startsWith("http")) {
            return null
        }

        val sanitizedUrl = if (databaseUrl.endsWith("/")) databaseUrl else "$databaseUrl/"
        val requestUrl = "${sanitizedUrl}photos.json"

        try {
            val photoJson = JSONObject()
            photoJson.put("caption", photo.caption)
            photoJson.put("category", photo.category)
            photoJson.put("uploadedBy", photo.uploadedBy)
            photoJson.put("timestamp", photo.timestamp)
            photoJson.put("familyGroup", photo.familyGroup)
            photoJson.put("isFavorite", photo.isFavorite)
            photoJson.put("likesCount", photo.likesCount)
            
            if (!base64String.isNullOrEmpty()) {
                photoJson.put("imageContentBase64", base64String)
            } else {
                photoJson.put("remoteUrl", photo.remoteUrl)
            }

            val requestBody = photoJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    val jsonResp = JSONObject(respBody)
                    return jsonResp.optString("name", null) // Firebase RTDB returns generated key as "name"
                } else {
                    Log.e(TAG, "Error posting photo via REST: Code ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading photo to Firebase: ${e.message}")
        }
        return null
    }
}

data class RemotePhotoDto(
    val remoteServerId: String,
    val caption: String,
    val category: String,
    val uploadedBy: String,
    val timestamp: Long,
    val familyGroup: String,
    val imageContentBase64: String,
    val remoteUrl: String,
    val isFavorite: Boolean,
    val likesCount: Int
)
