package com.example.thriftit.presentation.util

import android.util.Log
import com.example.thriftit.core.network.NetworkObserver
import com.example.thriftit.core.network.NetworkStatus
import com.example.thriftit.data.local.Converters
import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.mappers.toDomain
import com.example.thriftit.data.repository.UploadRepository
import com.example.thriftit.data.util.ImageStorageHelper
import com.example.thriftit.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager
    @Inject
    constructor(
        @ApplicationScope private val appScope: CoroutineScope,
        private val networkObserver: NetworkObserver,
        private val itemDao: ItemDao,
        private val uploadRepository: UploadRepository,
        private val imageStorageHelper: ImageStorageHelper,
    ) {
        private val converters = Converters()

        init {
            Log.d("SYNC", "SyncManager initialized")

            appScope.launch {
                networkObserver.networkStatus
                    .distinctUntilChanged()
                    .collect { status ->
                        Log.d("SYNC", "Network status = $status")
                        if (status == NetworkStatus.AVAILABLE) {
                            syncPendingItems()
                        }
                    }
            }
        }

        private suspend fun syncPendingItems() {
            val pendingItems = itemDao.getUnsyncedItems()
            Log.d("SYNC", "Found ${pendingItems.size} pending items to sync")

            pendingItems.forEach { entity ->
                try {
                    val imagePaths = converters.toStringList(entity.localImagePaths)
                    Log.d("SYNC", "Syncing item ${entity.id} with ${imagePaths.size} images")

                    if (imagePaths.isEmpty()) {
                        Log.w("SYNC", "Item ${entity.id} has no images, skipping")
                        return@forEach
                    }

                    val imageUris = imageStorageHelper.getUrisFromPaths(imagePaths)

                    if (imageUris.size != imagePaths.size) {
                        Log.e("SYNC", "Some images missing for item ${entity.id}: expected ${imagePaths.size}, found ${imageUris.size}")
                    }

                    uploadRepository
                        .uploadItemWithImages(
                            entity.toDomain(),
                            imageUris,
                        ).collect { result ->
                            when (result) {
                                is com.example.thriftit.domain.util.Result.Success -> {
                                    Log.d("SYNC", "Successfully synced item ${entity.id}, deleting local copy")
                                    itemDao.deleteItemById(entity.id)
                                    imageStorageHelper.deleteImages(imagePaths)
                                    Log.d("SYNC", "Local item and images deleted")
                                }

                                is com.example.thriftit.domain.util.Result.Error -> {
                                    Log.e("SYNC", "Failed to sync item ${entity.id}: ${result.message}")
                                }

                                else -> Unit
                            }
                        }
                } catch (e: Exception) {
                    Log.e("SYNC", "Error syncing item ${entity.id}: ${e.message}", e)
                }
            }
        }
    }
