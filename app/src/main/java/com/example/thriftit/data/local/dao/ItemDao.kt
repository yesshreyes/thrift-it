package com.example.thriftit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thriftit.data.local.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE is_available = 1")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE seller_id = :sellerId")
    fun getItemsBySeller(sellerId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE category = :category AND is_available = 1")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM items WHERE is_synced = 0")
    suspend fun getUnsyncedItems(): List<ItemEntity>

    @Query("UPDATE items SET is_synced = 1 WHERE id = :itemId")
    suspend fun markItemAsSynced(itemId: String)
}
