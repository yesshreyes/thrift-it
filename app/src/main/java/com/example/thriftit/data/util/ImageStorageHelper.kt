package com.example.thriftit.data.util

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorageHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val imagesDir: File
            get() {
                val dir = File(context.filesDir, "pending_images")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                return dir
            }

        /**
         * Copy an image from a content URI to internal storage
         * Returns the absolute file path
         */
        fun saveImageToInternalStorage(uri: Uri): String? {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val fileName = "${UUID.randomUUID()}.jpg"
                val file = File(imagesDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                inputStream.close()
                Log.d("ImageStorage", "Saved image to: ${file.absolutePath}")
                file.absolutePath
            } catch (e: Exception) {
                Log.e("ImageStorage", "Failed to save image: ${e.message}", e)
                null
            }
        }

        /**
         * Save multiple images and return their file paths
         */
        fun saveImagesToInternalStorage(uris: List<Uri>): List<String> = uris.mapNotNull { saveImageToInternalStorage(it) }

        /**
         * Get a File object from a stored path
         */
        fun getFileFromPath(path: String): File? {
            val file = File(path)
            return if (file.exists()) file else null
        }

        /**
         * Convert file paths to URIs for upload
         */
        fun getUrisFromPaths(paths: List<String>): List<Uri> =
            paths.mapNotNull { path ->
                val file = getFileFromPath(path)
                file?.let { Uri.fromFile(it) }
            }

        /**
         * Delete images after successful sync
         */
        fun deleteImages(paths: List<String>) {
            paths.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                        Log.d("ImageStorage", "Deleted image: $path")
                    }
                } catch (e: Exception) {
                    Log.e("ImageStorage", "Failed to delete image: ${e.message}", e)
                }
            }
        }

        /**
         * Clean up all pending images (use with caution)
         */
        fun cleanupAllPendingImages() {
            try {
                imagesDir.listFiles()?.forEach { it.delete() }
                Log.d("ImageStorage", "Cleaned up all pending images")
            } catch (e: Exception) {
                Log.e("ImageStorage", "Failed to cleanup images: ${e.message}", e)
            }
        }
    }
