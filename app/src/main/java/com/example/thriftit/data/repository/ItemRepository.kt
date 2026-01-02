package com.example.thriftit.data.repository

import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.mappers.toDomain
import com.example.thriftit.data.mappers.toDomainList
import com.example.thriftit.data.mappers.toEntity
import com.example.thriftit.data.mappers.toItem
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
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
    ) {
        private val itemsCollection = firestore.collection("items")

        // Get all available items with real-time updates
        @OptIn(DelicateCoroutinesApi::class)
        fun getAllItems(): Flow<Result<List<Item>>> =
            callbackFlow {
                trySend(Result.Loading)

                val registration =
                    itemsCollection
                        .whereEqualTo("isAvailable", true)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val items = snapshot.documents.mapNotNull { it.data?.toItem() }
                                trySend(Result.Success(items))

                                // Cache items locally
                                GlobalScope.launch {
                                    itemDao.insertItems(items.map { it.toEntity() })
                                }
                            }
                        }

                awaitClose { registration.remove() }
            }

        // Get items from local database (offline support)
        fun getLocalItems(): Flow<List<Item>> = itemDao.getAllItems().map { it.toDomainList() }

        // Get item by ID
        suspend fun getItemById(itemId: String): Result<Item?> =
            try {
                val doc = itemsCollection.document(itemId).get().await()
                val item = doc.data?.toItem()
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
        fun searchItems(query: String): Flow<Result<List<Item>>> =
            callbackFlow {
                trySend(Result.Loading)

                if (query.isBlank()) {
                    // If query is empty, return all items
                    getAllItems().collect { trySend(it) }
                    return@callbackFlow
                }

                // Search in local database first
                itemDao.searchItems(query).collect { localItems ->
                    if (localItems.isNotEmpty()) {
                        trySend(Result.Success(localItems.toDomainList()))
                    }
                }

                awaitClose { }
            }

        // Get items by category
        fun getItemsByCategory(category: ItemCategory): Flow<Result<List<Item>>> =
            callbackFlow {
                trySend(Result.Loading)

                val registration =
                    itemsCollection
                        .whereEqualTo("category", category.name)
                        .whereEqualTo("isAvailable", true)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val items = snapshot.documents.mapNotNull { it.data?.toItem() }
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
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val items = snapshot.documents.mapNotNull { it.data?.toItem() }
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
                            doc.data?.toItem()?.let { item ->
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

        // Delete item
        suspend fun deleteItem(itemId: String): Result<Unit> =
            try {
                itemsCollection.document(itemId).delete().await()
                itemDao.deleteItemById(itemId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }

        // Update item availability
        suspend fun updateItemAvailability(
            itemId: String,
            isAvailable: Boolean,
        ): Result<Unit> =
            try {
                itemsCollection
                    .document(itemId)
                    .update("isAvailable", isAvailable)
                    .await()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
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
