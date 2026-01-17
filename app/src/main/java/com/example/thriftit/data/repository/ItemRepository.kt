package com.example.thriftit.data.repository

import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.mappers.toDomain
import com.example.thriftit.data.mappers.toDomainList
import com.example.thriftit.data.mappers.toEntity
import com.example.thriftit.data.mappers.toItem
import com.example.thriftit.di.ApplicationScope
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val itemDao: ItemDao,
        @ApplicationScope private val appScope: CoroutineScope,
    ) {
        private val itemsCollection = firestore.collection("items")

        fun observeItems(): Flow<List<Item>> {
            syncItemsFromFirestore()
            return itemDao.getAllItems().map { it.toDomainList() }
        }

        private fun syncItemsFromFirestore() {
            itemsCollection
                .whereEqualTo("isAvailable", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    appScope.launch {
                        snapshot.documents.forEach { doc ->
                            val remoteItem = doc.data?.toItem(doc.id) ?: return@forEach

                            val localItem = itemDao.getItemById(doc.id)

                            // 🚫 DO NOT overwrite pending offline items
                            if (localItem?.pendingUpload == true) return@forEach

                            itemDao.insertItem(
                                remoteItem.toEntity(
                                    pendingUpload = false,
                                    isSynced = true,
                                ),
                            )
                        }
                    }
                }
        }

        /** 🔥 Single item */
        fun observeItem(itemId: String): Flow<Item?> = itemDao.getItemByIdFlow(itemId).map { it?.toDomain() }

        fun observeItemsByCategory(category: ItemCategory): Flow<List<Item>> = itemDao.getItemsByCategory(category.name).map { it.toDomainList() }

        fun observeItemsBySeller(sellerId: String): Flow<List<Item>> = itemDao.getItemsBySeller(sellerId).map { it.toDomainList() }

        suspend fun refreshOnce() {
            itemsCollection
                .whereEqualTo("isAvailable", true)
                .get()
                .await()
                .let { snapshot ->
                    val items =
                        snapshot.documents.mapNotNull { doc ->
                            doc.data?.toItem(doc.id)?.toEntity()
                        }
                    itemDao.insertItems(items)
                }
        }

        /** 🔥 Delete */
        suspend fun deleteItem(itemId: String) {
            firestore.document("items/$itemId").delete()
            itemDao.deleteItemById(itemId)
        }

        fun getAllItems(): Flow<List<Item>> {
            // 1️⃣ Start Firestore sync (side-effect)
            syncItemsFromFirestore()

            // 2️⃣ Return ROOM flow only
            return itemDao.getAllItems().map { it.toDomainList() }
        }

        // Get items from local database (offline support)
        fun getLocalItems(): Flow<List<Item>> = itemDao.getAllItems().map { it.toDomainList() }

        // Get item by ID
        suspend fun getItemById(itemId: String): Result<Item?> =
            try {
                val doc = itemsCollection.document(itemId).get().await()
                val item = doc.data?.toItem(doc.id)
                Result.Success(item)
            } catch (e: Exception) {
                // Try to get from local database
                try {
                    val localItem = itemDao.getItemById(itemId)?.toDomain()
                    Result.Success(localItem)
                } catch (localError: Exception) {
                    Result.Error(e)
                }
            }

        // Search items by query
        fun searchItems(query: String): Flow<List<Item>> =
            if (query.isBlank()) {
                getAllItems()
            } else {
                itemDao.searchItems(query).map { it.toDomainList() }
            }

        // Get items by category
        fun getItemsByCategory(category: ItemCategory): Flow<Result<List<Item>>> =
            callbackFlow {
                trySend(Result.Loading)

                val registration =
                    itemsCollection
                        .whereEqualTo("category", category.name)
                        .whereEqualTo("isAvailable", true)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val items =
                                    snapshot.documents.mapNotNull { doc ->
                                        doc.data?.toItem(doc.id)
                                    }
                                trySend(Result.Success(items))
                            }
                        }

                awaitClose { registration.remove() }
            }

        // Get items by seller
        fun getItemsBySeller(sellerId: String): Flow<Result<List<Item>>> =
            callbackFlow {
                trySend(Result.Loading)

                val registration =
                    itemsCollection
                        .whereEqualTo("sellerId", sellerId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val items =
                                    snapshot.documents.mapNotNull { doc ->
                                        doc.data?.toItem(doc.id)
                                    }
                                trySend(Result.Success(items))
                            }
                        }

                awaitClose { registration.remove() }
            }

        // Get nearby items (within radius in km)
        suspend fun getNearbyItems(
            userLat: Double,
            userLng: Double,
            radiusKm: Double = 50.0,
        ): Result<List<Item>> =
            try {
                // Firebase doesn't support geoqueries natively
                // Fetch all items and filter locally
                val snapshot =
                    itemsCollection
                        .whereEqualTo("isAvailable", true)
                        .get()
                        .await()

                val items =
                    snapshot.documents
                        .mapNotNull { doc ->
                            doc.data?.toItem(doc.id)?.let { item ->
                                item.coordinates?.let { coords ->
                                    val distance =
                                        calculateDistance(
                                            userLat,
                                            userLng,
                                            coords.latitude,
                                            coords.longitude,
                                        )
                                    if (distance <= radiusKm) {
                                        item.copy(distance = distance)
                                    } else {
                                        null
                                    }
                                }
                            }
                        }.sortedBy { it.distance }

                Result.Success(items)
            } catch (e: Exception) {
                Result.Error(e)
            }

        suspend fun updateItemAvailability(
            itemId: String,
            isAvailable: Boolean,
        ) {
            firestore
                .document("items/$itemId")
                .update("isAvailable", isAvailable)

            val item = itemDao.getItemById(itemId) ?: return
            itemDao.updateItem(item.copy(isAvailable = isAvailable))
        }

        // Calculate distance between two coordinates (Haversine formula)
        private fun calculateDistance(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
        ): Double {
            val r = 6371 // Earth's radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return r * c
        }
    }
