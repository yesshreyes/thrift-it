package com.example.thriftit.domain.models

data class User(
    val uid: String,
    val phoneNumber: String,
    val displayName: String?,
    val profileImageUrl: String?,
    val location: String?,
    val coordinates: Coordinates? = null,
    val lastUpdated: Long,
    val stats: UserStats = UserStats(),
) {
    val initials: String
        get() =
            displayName
                ?.split(" ")
                ?.mapNotNull { it.firstOrNull()?.uppercase() }
                ?.take(2)
                ?.joinToString("")
                ?: phoneNumber.takeLast(2)
}

data class UserStats(
    val itemsListed: Int = 0,
    val itemsSold: Int = 0,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
)
