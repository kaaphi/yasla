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

    @Query("SELECT * FROM StoreItem WHERE storeItem_storeId = :storeId AND isInList = 1")
    suspend fun getStoreList(storeId: Long) : List<StoreItem>

    suspend fun getStoreList(store: Store) : List<StoreItem> =
        getStoreList(store.id)

    @Insert
    suspend fun insertStoreAndReturnId(store: Store): Long

    suspend fun insertStore(store: Store): Store =
        store.copy(id = insertStoreAndReturnId(store))


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