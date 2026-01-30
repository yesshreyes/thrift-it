package com.example.thriftit.data.repository

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.thriftit.data.mappers.toFirestoreMap
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class UploadRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val firestore: FirebaseFirestore,
    ) {
        private val itemsCollection = firestore.collection("items")

        init {
            try {
                MediaManager.get()
            } catch (e: Exception) {
            }
        }

        suspend fun uploadImage(
            uri: Uri,
            folder: String = "thrift-it/items",
        ): Result<String> =
            suspendCancellableCoroutine { continuation ->
                try {
                    val requestId =
                        MediaManager
                            .get()
                            .upload(uri)
                            .option("folder", folder)
                            .option("resource_type", "image")
                            .unsigned("thrift_it_unsigned")
                            .callback(
                                object : UploadCallback {
                                    override fun onStart(requestId: String) {
                                    }

                                    override fun onProgress(
                                        requestId: String,
                                        bytes: Long,
                                        totalBytes: Long,
                                    ) {
                                        // Progress update
                                    }

                                    override fun onSuccess(
                                        requestId: String,
                                        resultData: Map<*, *>,
                                    ) {
                                        val secureUrl = resultData["secure_url"] as? String
                                        if (secureUrl != null) {
                                            continuation.resume(Result.Success(secureUrl))
                                        } else {
                                            continuation.resume(
                                                Result.Error(Exception("Failed to get image URL")),
                                            )
                                        }
                                    }

                                    override fun onError(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {
                                        continuation.resume(
                                            Result.Error(Exception(error.description)),
                                        )
                                    }

                                    override fun onReschedule(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {
                                        continuation.resume(
                                            Result.Error(Exception("Upload rescheduled: ${error.description}")),
                                        )
                                    }
                                },
                            ).dispatch()

                    continuation.invokeOnCancellation {
                        MediaManager.get().cancelRequest(requestId)
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.Error(e))
                }
            }

        // Upload multiple images with progress tracking
        fun uploadMultipleImages(
            uris: List<Uri>,
            folder: String = "thrift-it/items",
        ): Flow<UploadProgress> =
            callbackFlow {
                trySend(UploadProgress.Loading(0f))

                try {
                    val downloadUrls = mutableListOf<String>()
                    val totalImages = uris.size

                    uris.forEachIndexed { index, uri ->
                        val requestId =
                            MediaManager
                                .get()
                                .upload(uri)
                                .option("folder", folder)
                                .option("resource_type", "image")
                                .unsigned("thrift_it_unsigned") // Replace with your upload preset
                                .callback(
                                    object : UploadCallback {
                                        override fun onStart(requestId: String) {
                                            // Upload started for this image
                                        }

                                        override fun onProgress(
                                            requestId: String,
                                            bytes: Long,
                                            totalBytes: Long,
                                        ) {
                                            val imageProgress = (bytes.toFloat() / totalBytes.toFloat())
                                            val overallProgress = ((index + imageProgress) / totalImages) * 100
                                            trySend(UploadProgress.Loading(overallProgress.toFloat()))
                                        }

                                        override fun onSuccess(
                                            requestId: String,
                                            resultData: Map<*, *>,
                                        ) {
                                            val secureUrl = resultData["secure_url"] as? String
                                            if (secureUrl != null) {
                                                downloadUrls.add(secureUrl)

                                                if (downloadUrls.size == totalImages) {
                                                    trySend(UploadProgress.Success(downloadUrls))
                                                    close()
                                                }
                                            } else {
                                                trySend(
                                                    UploadProgress.Error("Failed to get URL for image ${index + 1}"),
                                                )
                                                close()
                                            }
                                        }

                                        override fun onError(
                                            requestId: String,
                                            error: ErrorInfo,
                                        ) {
                                            trySend(UploadProgress.Error(error.description))
                                            close()
                                        }

                                        override fun onReschedule(
                                            requestId: String,
                                            error: ErrorInfo,
                                        ) {
                                            // Handle rescheduling
                                        }
                                    },
                                ).dispatch()
                    }
                } catch (e: Exception) {
                    trySend(UploadProgress.Error(e.message ?: "Unknown error"))
                    close()
                }

                awaitClose { }
            }

        // Create item in Firestore
        suspend fun createItem(item: Item): Result<String> =
            try {
                val itemId = UUID.randomUUID().toString()
                val itemWithId = item.copy(id = itemId)

                itemsCollection
                    .document(itemId)
                    .set(itemWithId.toFirestoreMap())
                    .await()

                Result.Success(itemId)
            } catch (e: Exception) {
                Result.Error(e)
            }

        // Complete flow: Upload images and create item
        @OptIn(DelicateCoroutinesApi::class)
        fun uploadItemWithImages(
            item: Item,
            imageUris: List<Uri>,
        ): Flow<Result<String>> =
            callbackFlow {
                android.util.Log.d("UPLOAD_REPO", "uploadItemWithImages called with ${imageUris.size} images")

                if (imageUris.isEmpty()) {
                    android.util.Log.e("UPLOAD_REPO", "No images provided!")
                    trySend(Result.Error(Exception("No images to upload")))
                    close()
                    return@callbackFlow
                }

                trySend(Result.Loading)

                try {
                    // Verify MediaManager is initialized
                    MediaManager.get()
                    android.util.Log.d("UPLOAD_REPO", "MediaManager initialized successfully")
                } catch (e: Exception) {
                    android.util.Log.e("UPLOAD_REPO", "MediaManager not initialized: ${e.message}", e)
                    trySend(Result.Error(Exception("Cloudinary not initialized: ${e.message}")))
                    close()
                    return@callbackFlow
                }

                val downloadUrls = mutableListOf<String>()
                val totalImages = imageUris.size
                var completedUploads = 0
                var hasError = false

                imageUris.forEachIndexed { index, uri ->
                    if (hasError) return@forEachIndexed

                    android.util.Log.d("UPLOAD_REPO", "Starting upload for image ${index + 1}/$totalImages: $uri")

                    try {
                        MediaManager
                            .get()
                            .upload(uri)
                            .option("folder", "thrift-it/items")
                            .option("resource_type", "image")
                            .unsigned("thrift_it_unsigned")
                            .callback(
                                object : UploadCallback {
                                    override fun onSuccess(
                                        requestId: String,
                                        resultData: Map<*, *>,
                                    ) {
                                        android.util.Log.d("UPLOAD_REPO", "Image ${index + 1} uploaded successfully")
                                        val secureUrl = resultData["secure_url"] as? String

                                        if (secureUrl == null) {
                                            android.util.Log.e("UPLOAD_REPO", "No secure_url in response: $resultData")
                                            if (!hasError) {
                                                hasError = true
                                                sendError("Image upload failed - no URL returned")
                                            }
                                            return
                                        }

                                        downloadUrls.add(secureUrl)
                                        completedUploads++
                                        android.util.Log.d("UPLOAD_REPO", "Progress: $completedUploads/$totalImages images uploaded")

                                        if (completedUploads == totalImages) {
                                            android.util.Log.d("UPLOAD_REPO", "All images uploaded, creating Firestore document")
                                            launch {
                                                try {
                                                    val itemId = UUID.randomUUID().toString()
                                                    val itemWithImages =
                                                        item.copy(
                                                            id = itemId,
                                                            imageUrls = downloadUrls,
                                                        )

                                                    android.util.Log.d("UPLOAD_REPO", "Saving to Firestore with ID: $itemId")
                                                    itemsCollection
                                                        .document(itemId)
                                                        .set(itemWithImages.toFirestoreMap())
                                                        .await()

                                                    android.util.Log.d("UPLOAD_REPO", "Firestore save successful!")
                                                    trySend(Result.Success(itemId))
                                                    close()
                                                } catch (e: Exception) {
                                                    android.util.Log.e("UPLOAD_REPO", "Firestore save failed: ${e.message}", e)
                                                    sendError(e.message ?: "Firestore upload failed")
                                                }
                                            }
                                        }
                                    }

                                    override fun onError(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {
                                        android.util.Log.e("UPLOAD_REPO", "Upload error for image ${index + 1}: ${error.description}")
                                        if (!hasError) {
                                            hasError = true
                                            sendError("Image upload failed: ${error.description}")
                                        }
                                    }

                                    override fun onReschedule(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {
                                        android.util.Log.w("UPLOAD_REPO", "Upload rescheduled for image ${index + 1}: ${error.description}")
                                        if (!hasError) {
                                            hasError = true
                                            sendError("Upload rescheduled: ${error.description}")
                                        }
                                    }

                                    override fun onStart(requestId: String) {
                                        android.util.Log.d("UPLOAD_REPO", "Upload started for image ${index + 1}, requestId: $requestId")
                                    }

                                    override fun onProgress(
                                        requestId: String,
                                        bytes: Long,
                                        totalBytes: Long,
                                    ) {
                                        val progress = (bytes.toFloat() / totalBytes.toFloat() * 100).toInt()
                                        android.util.Log.d("UPLOAD_REPO", "Image ${index + 1} progress: $progress%")
                                    }
                                },
                            ).dispatch()
                    } catch (e: Exception) {
                        android.util.Log.e("UPLOAD_REPO", "Exception dispatching upload for image ${index + 1}: ${e.message}", e)
                        if (!hasError) {
                            hasError = true
                            sendError("Upload dispatch failed: ${e.message}")
                        }
                    }
                }

                awaitClose {
                    android.util.Log.d("UPLOAD_REPO", "Upload flow closed")
                }
            }

        private fun ProducerScope<Result<String>>.sendError(message: String) {
            trySend(Result.Error(Exception(message)))
            close()
        }

        // Upload profile image to Cloudinary
        suspend fun uploadProfileImage(
            uri: Uri,
            userId: String,
        ): Result<String> =
            suspendCancellableCoroutine { continuation ->
                try {
                    val requestId =
                        MediaManager
                            .get()
                            .upload(uri)
                            .option("folder", "thrift-it/profiles")
                            .option("public_id", userId) // Use userId as filename
                            .option("resource_type", "image")
                            .option("overwrite", true) // Overwrite existing profile image
                            .unsigned("thrift_it_unsigned") // Replace with your upload preset
                            .callback(
                                object : UploadCallback {
                                    override fun onStart(requestId: String) {}

                                    override fun onProgress(
                                        requestId: String,
                                        bytes: Long,
                                        totalBytes: Long,
                                    ) {}

                                    override fun onSuccess(
                                        requestId: String,
                                        resultData: Map<*, *>,
                                    ) {
                                        val secureUrl = resultData["secure_url"] as? String
                                        if (secureUrl != null) {
                                            continuation.resume(Result.Success(secureUrl))
                                        } else {
                                            continuation.resume(
                                                Result.Error(Exception("Failed to get profile image URL")),
                                            )
                                        }
                                    }

                                    override fun onError(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {
                                        continuation.resume(Result.Error(Exception(error.description)))
                                    }

                                    override fun onReschedule(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {}
                                },
                            ).dispatch()

                    continuation.invokeOnCancellation {
                        MediaManager.get().cancelRequest(requestId)
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.Error(e))
                }
            }

        // Delete image from Cloudinary (requires authenticated request with API key)
        suspend fun deleteImage(publicId: String): Result<Unit> =
            try {
                // Note: Deletion requires backend API with API secret
                // You'll need to implement this through your backend
                // For now, just mark as success (implement backend deletion later)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }

        // Update item
        suspend fun updateItem(item: Item): Result<Unit> =
            try {
                itemsCollection
                    .document(item.id)
                    .set(item.toFirestoreMap())
                    .await()

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
    }

// Progress sealed class for upload tracking
sealed class UploadProgress {
    data class Loading(
        val progress: Float,
    ) : UploadProgress()

    data class Success(
        val urls: List<String>,
    ) : UploadProgress()

    data class Error(
        val message: String,
    ) : UploadProgress()
}
