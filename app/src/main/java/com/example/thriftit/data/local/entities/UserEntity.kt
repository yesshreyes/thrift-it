package com.example.thriftit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "display_name")
    val displayName: String?,
    @ColumnInfo(name = "profile_image_url")
    val profileImageUrl: String?,
    @ColumnInfo(name = "location")
    val location: String?,
    @ColumnInfo(name = "latitude")
    val latitude: Double?,
    @ColumnInfo(name = "longitude")
    val longitude: Double?,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
)
