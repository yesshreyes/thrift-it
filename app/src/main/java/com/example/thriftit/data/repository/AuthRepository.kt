package com.example.thriftit.data.repository

import android.app.Activity
import com.example.thriftit.data.local.dao.UserDao
import com.example.thriftit.data.mappers.toEntity
import com.example.thriftit.data.mappers.toFirestoreMap
import com.example.thriftit.data.mappers.toUser
import com.example.thriftit.domain.models.User
import com.example.thriftit.domain.util.Result
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
        private val userDao: UserDao,
    ) {
        private val usersCollection = firestore.collection("users")

        fun getCurrentUserId(): String? = auth.currentUser?.uid

        fun isUserLoggedIn(): Boolean = auth.currentUser != null

        fun sendVerificationCode(
            phoneNumber: String,
            activity: Activity,
            callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        ): Flow<Result<String>> =
            callbackFlow {
                trySend(Result.Loading)

                try {
                    val options =
                        PhoneAuthOptions
                            .newBuilder(auth)
                            .setPhoneNumber(phoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity)
                            .setCallbacks(
                                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                        trySend(Result.Success("auto_verified"))
                                        callbacks.onVerificationCompleted(credential)
                                    }

                                    override fun onVerificationFailed(exception: FirebaseException) {
                                        trySend(Result.Error(exception))
                                        callbacks.onVerificationFailed(exception)
                                    }

                                    override fun onCodeSent(
                                        verificationId: String,
                                        token: PhoneAuthProvider.ForceResendingToken,
                                    ) {
                                        trySend(Result.Success(verificationId))
                                        callbacks.onCodeSent(verificationId, token)
                                    }
                                },
                            ).build()

                    PhoneAuthProvider.verifyPhoneNumber(options)
                } catch (e: Exception) {
                    trySend(Result.Error(e))
                }

                awaitClose { }
            }

        suspend fun verifyOtpAndSignIn(
            verificationId: String,
            otp: String,
        ): Result<String> { // Return verificationId, NOT User
            return try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user ?: return Result.Error(Exception("User is null"))
                val user =
                    User(
                        uid = firebaseUser.uid,
                        phoneNumber = firebaseUser.phoneNumber ?: "",
                        displayName = null,
                        profileImageUrl = null,
                        location = null,
                        coordinates = null,
                        lastUpdated = System.currentTimeMillis(),
                    )

                // Save locally so Profile screen can read it
                userDao.insertUser(user.toEntity())
                // ✅ DON'T create minimal user - just sign in
                Result.Success(firebaseUser.uid) // Return UID only
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

        suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<User> {
            return try {
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser =
                    authResult.user
                        ?: return Result.Error(Exception("User is null"))

                val userDoc = usersCollection.document(firebaseUser.uid).get().await()

                val user =
                    if (userDoc.exists()) {
                        userDoc.data?.toUser()
                            ?: return Result.Error(Exception("Failed to parse user data"))
                    } else {
                        val newUser =
                            User(
                                uid = firebaseUser.uid,
                                phoneNumber = firebaseUser.phoneNumber ?: "",
                                displayName = null,
                                profileImageUrl = null,
                                location = null,
                                coordinates = null,
                                lastUpdated = System.currentTimeMillis(),
                            )

                        usersCollection.document(newUser.uid).set(newUser.toFirestoreMap()).await()
                        newUser
                    }

                userDao.insertUser(user.toEntity())
                Result.Success(user)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

        fun getCurrentUserProfile(): Flow<Result<User?>> =
            callbackFlow {
                val userId = getCurrentUserId()
                if (userId == null) {
                    trySend(Result.Success(null))
                    close()
                    return@callbackFlow
                }

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
                            } else {
                                trySend(Result.Success(null))
                            }
                        }

                awaitClose { registration.remove() }
            }

        suspend fun updateUserProfile(user: User): Result<Unit> =
            try {
                val updatedUser = user.copy(lastUpdated = System.currentTimeMillis())

                usersCollection
                    .document(user.uid)
                    .set(updatedUser.toFirestoreMap())
                    .await()

                userDao.updateUser(updatedUser.toEntity())

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }

        suspend fun signOut(): Result<Unit> =
            try {
                auth.signOut()
                userDao.deleteAllUsers()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }

        suspend fun deleteAccount(): Result<Unit> {
            return try {
                val userId =
                    getCurrentUserId()
                        ?: return Result.Error(Exception("User not logged in"))

                usersCollection.document(userId).delete().await()
                userDao.deleteUserById(userId)
                auth.currentUser?.delete()?.await()

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

        fun getCurrentUserPhoneNumber(): String = auth.currentUser?.phoneNumber.orEmpty()
    }
