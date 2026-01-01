package com.example.thriftit.data.mappers

import com.example.thriftit.data.local.entities.UserEntity
import com.example.thriftit.domain.models.Coordinates
import com.example.thriftit.domain.models.User

// Entity to Domain
fun UserEntity.toDomain(): User =
    User(
        uid = this.uid,
        phoneNumber = this.phoneNumber,
        displayName = this.displayName,
        profileImageUrl = this.profileImageUrl,
        location = this.location,
        coordinates =
            if (this.latitude != null && this.longitude != null) {
                Coordinates(this.latitude, this.longitude)
            } else {
                null
            },
        lastUpdated = this.lastUpdated,
    )

// Domain to Entity
fun User.toEntity(): UserEntity =
    UserEntity(
        uid = this.uid,
        phoneNumber = this.phoneNumber,
        displayName = this.displayName,
        profileImageUrl = this.profileImageUrl,
        location = this.location,
        latitude = this.coordinates?.latitude,
        longitude = this.coordinates?.longitude,
        lastUpdated = this.lastUpdated,
        isSynced = false,
    )

// Firestore Document to Domain
fun Map<String, Any?>.toUser(): User? {
    return try {
        User(
            uid = this["uid"] as? String ?: return null,
            phoneNumber = this["phoneNumber"] as? String ?: return null,
            displayName = this["displayName"] as? String,
            profileImageUrl = this["profileImageUrl"] as? String,
            location = this["location"] as? String,
            coordinates =
                if (this["latitude"] != null && this["longitude"] != null) {
                    Coordinates(
                        latitude = (this["latitude"] as Number).toDouble(),
                        longitude = (this["longitude"] as Number).toDouble(),
                    )
                } else {
                    null
                },
            lastUpdated = (this["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        )
    } catch (e: Exception) {
        null
    }
}

// Domain to Firestore Map
fun User.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "uid" to uid,
        "phoneNumber" to phoneNumber,
        "displayName" to displayName,
        "profileImageUrl" to profileImageUrl,
        "location" to location,
        "latitude" to coordinates?.latitude,
        "longitude" to coordinates?.longitude,
        "lastUpdated" to lastUpdated,
    )
