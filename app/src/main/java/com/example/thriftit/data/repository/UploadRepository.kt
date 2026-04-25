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

        /** Uploads all [imageUris] to Cloudinary then saves the item to Firestore. */
        fun uploadItemWithImages(item: Item, imageUris: List<Uri>): Flow<Result<String>> =
            callbackFlow {
                if (imageUris.isEmpty()) {
                    trySend(Result.Error(Exception("No images to upload")))
                    close()
                    return@callbackFlow
                }

                trySend(Result.Loading)

                try {
                    MediaManager.get()
                } catch (e: Exception) {
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
                    try {
                        MediaManager.get()
                            .upload(uri)
                            .option("folder", "thrift-it/items")
                            .option("resource_type", "image")
                            .unsigned("thrift_it_unsigned")
                            .callback(object : UploadCallback {
                                override fun onStart(requestId: String) {}
                                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                                override fun onReschedule(requestId: String, error: ErrorInfo) {
                                    if (!hasError) { hasError = true; sendError("Upload rescheduled: ${error.description}") }
                                }
                                override fun onError(requestId: String, error: ErrorInfo) {
                                    if (!hasError) { hasError = true; sendError("Image upload failed: ${error.description}") }
                                }
                                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                    val url = resultData["secure_url"] as? String
                                    if (url == null) {
                                        if (!hasError) { hasError = true; sendError("Image upload failed — no URL returned") }
                                        return
                                    }
                                    downloadUrls.add(url)
                                    completedUploads++
                                    if (completedUploads == totalImages) {
                                        launch {
                                            try {
                                                val itemId = UUID.randomUUID().toString()
                                                itemsCollection
                                                    .document(itemId)
                                                    .set(item.copy(id = itemId, imageUrls = downloadUrls).toFirestoreMap())
                                                    .await()
                                                trySend(Result.Success(itemId))
                                                close()
                                            } catch (e: Exception) {
                                                sendError(e.message ?: "Firestore write failed")
                                            }
                                        }
                                    }
                                }
                            }).dispatch()
                    } catch (e: Exception) {
                        if (!hasError) { hasError = true; sendError("Upload dispatch failed: ${e.message}") }
                    }
                }

                awaitClose {}
            }

        /** Uploads a single profile image to Cloudinary and returns its URL. */
        suspend fun uploadProfileImage(uri: Uri, userId: String): Result<String> =
            suspendCancellableCoroutine { continuation ->
                try {
                    val requestId = MediaManager.get()
                        .upload(uri)
                        .option("folder", "thrift-it/profiles")
                        .option("public_id", userId)
                        .option("resource_type", "image")
                        .option("overwrite", true)
                        .unsigned("thrift_it_unsigned")
                        .callback(object : UploadCallback {
                            override fun onStart(requestId: String) {}
                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                            override fun onReschedule(requestId: String, error: ErrorInfo) {}
                            override fun onError(requestId: String, error: ErrorInfo) {
                                continuation.resume(Result.Error(Exception(error.description)))
                            }
                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                val url = resultData["secure_url"] as? String
                                if (url != null) continuation.resume(Result.Success(url))
                                else continuation.resume(Result.Error(Exception("Failed to get profile image URL")))
                            }
                        }).dispatch()
                    continuation.invokeOnCancellation { MediaManager.get().cancelRequest(requestId) }
                } catch (e: Exception) {
                    continuation.resume(Result.Error(e))
                }
            }

        private fun ProducerScope<Result<String>>.sendError(message: String) {
            trySend(Result.Error(Exception(message)))
            close()
        }
    }
