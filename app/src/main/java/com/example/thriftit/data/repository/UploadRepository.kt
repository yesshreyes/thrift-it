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
            // Initialize Cloudinary MediaManager if not already initialized
            try {
                MediaManager.get()
            } catch (e: Exception) {
                // MediaManager not initialized, will be initialized in Application class
            }
        }

        // Upload single image to Cloudinary
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
                            .unsigned("thrift_it_unsigned") // Replace with your upload preset
                            .callback(
                                object : UploadCallback {
                                    override fun onStart(requestId: String) {
                                        // Upload started
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
                trySend(Result.Loading)

                try {
                    val downloadUrls = mutableListOf<String>()
                    val totalImages = imageUris.size
                    var completedUploads = 0

                    imageUris.forEach { uri ->
                        MediaManager
                            .get()
                            .upload(uri)
                            .option("folder", "thrift-it/items")
                            .option("resource_type", "image")
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
                                            downloadUrls.add(secureUrl)
                                            completedUploads++

                                            // Check if all images uploaded
                                            if (completedUploads == totalImages) {
                                                // Create item in Firestore
                                                kotlinx.coroutines.GlobalScope.launch {
                                                    try {
                                                        val itemId = UUID.randomUUID().toString()
                                                        val itemWithImages =
                                                            item.copy(
                                                                id = itemId,
                                                                imageUrls = downloadUrls,
                                                            )

                                                        itemsCollection
                                                            .document(itemId)
                                                            .set(itemWithImages.toFirestoreMap())
                                                            .await()

                                                        trySend(Result.Success(itemId))
                                                        close()
                                                    } catch (e: Exception) {
                                                        trySend(Result.Error(e))
                                                        close()
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    override fun onError(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {
                                        trySend(Result.Error(Exception(error.description)))
                                        close()
                                    }

                                    override fun onReschedule(
                                        requestId: String,
                                        error: ErrorInfo,
                                    ) {}
                                },
                            ).dispatch()
                    }
                } catch (e: Exception) {
                    trySend(Result.Error(e))
                    close()
                }

                awaitClose { }
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
