package com.kaaphi.yasla.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StoreListDao {
    @Query("SELECT * FROM Store")
    suspend fun getStores(): List<Store>

    @Query("SELECT * FROM StoreItem WHERE storeItem_storeId = :storeId AND isInList = 1 ORDER BY rank")
    suspend fun getStoreList(storeId: Long) : List<StoreItem>

    @Query("SELECT * FROM StoreItem WHERE storeItem_storeId = :storeId ORDER BY rank")
    suspend fun getStoreItems(storeId: Long) : List<StoreItem>

    suspend fun getStoreList(store: Store) : List<StoreItem> =
        getStoreList(store.id)

    @Insert
    suspend fun insertStoreAndReturnId(store: Store): Long

    suspend fun insertStore(store: Store): Store =
        store.copy(id = insertStoreAndReturnId(store))

    @Query("SELECT * FROM StoreItem WHERE storeItem_storeId = :storeId AND storeItem_name = :name")
    suspend fun getItemByName(storeId: Long, name: String) : StoreItem?

    @Query("SELECT * FROM StoreItem WHERE storeItem_storeId = :storeId AND rank = :rank")
    suspend fun getItemByRank(storeId: Long, rank: String) : StoreItem?

    @Query("SELECT * FROM StoreItem WHERE storeItem_storeId = :storeId ORDER BY rank")
    suspend fun getFirstItemByRank(storeId: Long) : StoreItem?

    @Query("SELECT rank FROM StoreItem " +
            "WHERE storeItem_storeId = :storeId AND rank >= :startRank AND rank <= :endRank " +
            "ORDER BY rank")
    suspend fun getRanks(storeId: Long, startRank: String, endRank: String) : List<String>

    @Insert
    suspend fun insertStoreItemAndReturnId(item: StoreItem) : Long

    suspend fun insertStoreItem(item: StoreItem) : StoreItem =
        item.copy(id = insertStoreItemAndReturnId(item))

    @Update
    suspend fun updateStoreItems(vararg items: StoreItem)

    @Update
    suspend fun updateStoreItems(items: List<StoreItem>)

    @Delete
    suspend fun deleteStoreItems(vararg items: StoreItem)
}