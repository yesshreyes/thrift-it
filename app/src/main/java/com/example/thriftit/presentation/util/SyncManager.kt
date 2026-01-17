package com.example.thriftit.presentation.util

import android.util.Log
import com.example.thriftit.core.network.NetworkObserver
import com.example.thriftit.core.network.NetworkStatus
import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.mappers.toDomain
import com.example.thriftit.data.repository.UploadRepository
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
    ) {
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

            pendingItems.forEach { entity ->
                uploadRepository
                    .uploadItemWithImages(
                        entity.toDomain(),
                        entity.localImageUris,
                    ).collect { result ->
                        when (result) {
                            is com.example.thriftit.domain.util.Result.Success -> {
                                itemDao.markItemAsSynced(entity.id)
                            }

                            is com.example.thriftit.domain.util.Result.Error -> {
                                // optional: log error, retry later
                            }

                            else -> Unit
                        }
                    }
            }
        }
    }
