package com.example.thriftit.data.repository

import com.example.thriftit.data.local.dao.UserDao
import com.example.thriftit.data.mappers.toDomain
import com.example.thriftit.data.mappers.toEntity
import com.example.thriftit.data.mappers.toFirestoreMap
import com.example.thriftit.data.mappers.toUser
import com.example.thriftit.domain.models.Coordinates
import com.example.thriftit.domain.models.User
import com.example.thriftit.domain.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val userDao: UserDao,
    ) {
        private val usersCollection = firestore.collection("users")

        // Get user by ID with real-time updates
        @OptIn(DelicateCoroutinesApi::class)
        fun getUserById(userId: String): Flow<Result<User?>> =
            callbackFlow {
                trySend(Result.Loading)

                val registration =
                    usersCollection
                        .document(userId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot?.exists() == true) {
                                val user = snapshot.data?.toUser()
                                trySend(Result.Success(user))

                                // Cache locally
                                user?.let {
                                    kotlinx.coroutines.GlobalScope.launch {
                                        userDao.insertUser(it.toEntity())
                                    }
                                }
                            } else {
                                trySend(Result.Success(null))
                            }
                        }

                awaitClose { registration.remove() }
            }

        // Get user from local database
        suspend fun getLocalUser(userId: String): User? = userDao.getUserByIdSuspend(userId)?.toDomain()

        // Update user profile
        suspend fun updateUserProfile(user: User): Result<Unit> =
            try {
                val updatedUser = user.copy(lastUpdated = System.currentTimeMillis())

                // Update Firestore
                usersCollection
                    .document(user.uid)
                    .set(
                        updatedUser.toFirestoreMap(),
                        com.google.firebase.firestore.SetOptions
                            .merge(),
                    ).await()

                // Update local database
                userDao.updateUser(updatedUser.toEntity())

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }

        // Update user location
        suspend fun updateUserLocation(
            userId: String,
            location: String,
            coordinates: Coordinates?,
        ): Result<Unit> =
            try {
                val timestamp = System.currentTimeMillis()

                // Update Firestore
                usersCollection
                    .document(userId)
                    .set(
                        mapOf(
                            "location" to location,
                            "latitude" to coordinates?.latitude,
                            "longitude" to coordinates?.longitude,
                            "lastUpdated" to timestamp,
                        ),
                        SetOptions.merge(),
                    ).await()

                // Update local database
                userDao.updateUserLocation(
                    userId = userId,
                    location = location,
                    latitude = coordinates?.latitude,
                    longitude = coordinates?.longitude,
                    timestamp = timestamp,
                )

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }

        // Update profile image URL
        suspend fun updateProfileImage(
            userId: String,
            imageUrl: String,
        ): Result<Unit> =
            try {
                val timestamp = System.currentTimeMillis()

                usersCollection
                    .document(userId)
                    .update(
                        mapOf(
                            "profileImageUrl" to imageUrl,
                            "lastUpdated" to timestamp,
                        ),
                    ).await()

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }

        // Delete user
        suspend fun deleteUser(userId: String): Result<Unit> =
            try {
                usersCollection.document(userId).delete().await()
                userDao.deleteUserById(userId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
    }
