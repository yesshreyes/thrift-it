package com.example.thriftit.domain.models

data class Item(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: ItemCategory,
    val condition: ItemCondition,
    val imageUrls: List<String>,
    val sellerId: String,
    val sellerName: String? = null,
    val location: String,
    val coordinates: Coordinates? = null,
    val isAvailable: Boolean = true,
    val distance: Double? = null,
) {
    val formattedPrice: String
        get() = "₹${String.format("%,.2f", price)}"
}

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

enum class ItemCategory(
    val displayName: String,
) {
    ELECTRONICS("Electronics"),
    CLOTHING("Clothing"),
    FURNITURE("Furniture"),
    BOOKS("Books"),
    SPORTS("Sports & Fitness"),
    HOME_APPLIANCES("Home Appliances"),
    TOYS("Toys & Games"),
    VEHICLES("Vehicles"),
    ACCESSORIES("Accessories"),
    OTHER("Other"),
    ;

    companion object {
        fun fromString(value: String): ItemCategory =
            entries.find {
                it.name.equals(value, ignoreCase = true)
            } ?: OTHER
    }
}

enum class ItemCondition(
    val displayName: String,
) {
    NEW("New"),
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor"),
    ;

    companion object {
        fun fromString(value: String): ItemCondition =
            entries.find {
                it.name.equals(value, ignoreCase = true)
            } ?: GOOD
    }
}
