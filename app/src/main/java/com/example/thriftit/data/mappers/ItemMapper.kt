package com.example.thriftit.data.mappers

import com.example.thriftit.data.local.entities.ItemEntity
import com.example.thriftit.domain.models.Coordinates
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.models.ItemCondition

// Entity to Domain
fun ItemEntity.toDomain(): Item =
    Item(
        id = this.id,
        title = this.title,
        description = this.description,
        price = this.price,
        category = ItemCategory.fromString(this.category),
        condition = ItemCondition.fromString(this.condition),
        imageUrls = this.imageUrls.split(",").filter { it.isNotEmpty() },
        sellerId = this.sellerId,
        location = this.location,
        coordinates =
            if (this.latitude != null && this.longitude != null) {
                Coordinates(this.latitude, this.longitude)
            } else {
                null
            },
        isAvailable = this.isAvailable,
    )

// Domain to Entity
fun Item.toEntity(): ItemEntity =
    ItemEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        price = this.price,
        category = this.category.name,
        condition = this.condition.name,
        imageUrls = this.imageUrls.joinToString(","),
        sellerId = this.sellerId,
        location = this.location,
        latitude = this.coordinates?.latitude,
        longitude = this.coordinates?.longitude,
        isAvailable = this.isAvailable,
        isSynced = false,
    )

// List extensions
fun List<ItemEntity>.toDomainList(): List<Item> = map { it.toDomain() }

fun List<Item>.toEntityList(): List<ItemEntity> = map { it.toEntity() }

// Firestore Document to Domain
fun Map<String, Any?>.toItem(documentId: String): Item? {
    return try {
        val imageUrls =
            when (val raw = this["imageUrls"]) {
                is List<*> -> raw.mapNotNull { it as? String }
                is String -> listOf(raw)
                else -> emptyList()
            }
        Item(
            id = documentId,
            title = this["title"] as? String ?: return null,
            description = this["description"] as? String ?: "",
            price = (this["price"] as? Number)?.toDouble() ?: 0.0,
            category = ItemCategory.fromString(this["category"] as? String ?: "OTHER"),
            condition = ItemCondition.fromString(this["condition"] as? String ?: "GOOD"),
            imageUrls = imageUrls,
            sellerId = this["sellerId"] as? String ?: return null,
            sellerName = this["sellerName"] as? String,
            location = this["location"] as? String ?: "",
            coordinates =
                if (this["latitude"] != null && this["longitude"] != null) {
                    Coordinates(
                        latitude = (this["latitude"] as Number).toDouble(),
                        longitude = (this["longitude"] as Number).toDouble(),
                    )
                } else {
                    null
                },
            isAvailable = this["isAvailable"] as? Boolean ?: true,
        )
    } catch (e: Exception) {
        null
    }
}

// Domain to Firestore Map
fun Item.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "title" to title,
        "description" to description,
        "price" to price,
        "category" to category.name,
        "condition" to condition.name,
        "imageUrls" to imageUrls,
        "sellerId" to sellerId,
        "location" to location,
        "latitude" to coordinates?.latitude,
        "longitude" to coordinates?.longitude,
        "isAvailable" to isAvailable,
    )
