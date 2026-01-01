package com.example.thriftit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "price")
    val price: Double,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "condition")
    val condition: String,
    @ColumnInfo(name = "image_urls")
    val imageUrls: String, // Store as comma-separated string
    @ColumnInfo(name = "seller_id")
    val sellerId: String,
    @ColumnInfo(name = "location")
    val location: String,
    @ColumnInfo(name = "latitude")
    val latitude: Double?,
    @ColumnInfo(name = "longitude")
    val longitude: Double?,
    @ColumnInfo(name = "is_available")
    val isAvailable: Boolean = true,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
)
