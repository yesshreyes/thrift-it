package com.example.thriftit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thriftit.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE uid = :userId")
    suspend fun getUserByIdSuspend(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE uid = :userId")
    suspend fun deleteUserById(userId: String)

    @Query(
        "UPDATE users SET location = :location, latitude = :latitude, longitude = :longitude, last_updated = :timestamp WHERE uid = :userId",
    )
    suspend fun updateUserLocation(
        userId: String,
        location: String,
        latitude: Double?,
        longitude: Double?,
        timestamp: Long,
    )

    @Query("SELECT * FROM users WHERE is_synced = 0")
    suspend fun getUnsyncedUsers(): List<UserEntity>

    @Query("UPDATE users SET is_synced = 1 WHERE uid = :userId")
    suspend fun markUserAsSynced(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
