package com.example.thriftit.data.repository

import android.app.Activity
import com.example.thriftit.data.local.dao.UserDao
import com.example.thriftit.data.mappers.toDomain
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
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) {

    private val usersCollection = firestore.collection("users")

    // Get current user ID
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Send OTP to phone number
    suspend fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): Flow<Result<String>> = callbackFlow {
        trySend(Result.Loading)

        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-retrieval or instant verification
                        trySend(Result.Success("auto_verified"))
                        callbacks.onVerificationCompleted(credential)
                    }

                    override fun onVerificationFailed(exception: FirebaseException) {
                        trySend(Result.Error(exception))
                        callbacks.onVerificationFailed(exception)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        trySend(Result.Success(verificationId))
                        callbacks.onCodeSent(verificationId, token)
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            trySend(Result.Error(e))
        }

        awaitClose { }
    }

    // Verify OTP and sign in
    suspend fun verifyOtpAndSignIn(
        verificationId: String,
        otp: String
    ): Result<User> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.Error(Exception("User is null"))

            // Check if user exists in Firestore
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()

            val user = if (userDoc.exists()) {
                // User exists, fetch from Firestore
                userDoc.data?.toUser() ?: return Result.Error(Exception("Failed to parse user data"))
            } else {
                // New user, create profile
                val newUser = User(
                    uid = firebaseUser.uid,
                    phoneNumber = firebaseUser.phoneNumber ?: "",
                    displayName = null,
                    profileImageUrl = null,
                    location = null,
                    coordinates = null,
                    lastUpdated = System.currentTimeMillis()
                )

                // Save to Firestore
                usersCollection.document(newUser.uid).set(newUser.toFirestoreMap()).await()

                newUser
            }

            // Cache user locally
            userDao.insertUser(user.toEntity())

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Sign in with credential (for auto-verification)
    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<User> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.Error(Exception("User is null"))

            val userDoc = usersCollection.document(firebaseUser.uid).get().await()

            val user = if (userDoc.exists()) {
                userDoc.data?.toUser() ?: return Result.Error(Exception("Failed to parse user data"))
            } else {
                val newUser = User(
                    uid = firebaseUser.uid,
                    phoneNumber = firebaseUser.phoneNumber ?: "",
                    displayName = null,
                    profileImageUrl = null,
                    location = null,
                    coordinates = null,
                    lastUpdated = System.currentTimeMillis()
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

    // Get current user profile
    fun getCurrentUserProfile(): Flow<Result<User?>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Result.Success(null))
            close()
            return@callbackFlow
        }

        trySend(Result.Loading)

        val registration = usersCollection.document(userId)
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

    // Update user profile
    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            val updatedUser = user.copy(lastUpdated = System.currentTimeMillis())

            // Update Firestore
            usersCollection.document(user.uid)
                .set(updatedUser.toFirestoreMap())
                .await()

            // Update local database
            userDao.updateUser(updatedUser.toEntity())

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Sign out
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            userDao.deleteAllUsers()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Delete account
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(Exception("User not logged in"))

            // Delete from Firestore
            usersCollection.document(userId).delete().await()

            // Delete from local database
            userDao.deleteUserById(userId)

            // Delete Firebase Auth account
            auth.currentUser?.delete()?.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
