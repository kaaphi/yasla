package com.kaaphi.yasla.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreDatabaseTest {
    private lateinit var storeListDao: StoreListDao
    private lateinit var db: StoreDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, StoreDatabase::class.java).build()
        storeListDao = db.getStoreListDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun test() = runBlocking {
        val store = storeListDao.insertStore(Store(name = "my store"))

        val items = (1..5).map {
            storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "item $it", rank = "none"))
        }

        assertEquals(items, storeListDao.getStoreList(store))

        storeListDao.updateStoreItems(items[0].copy(isInList = false))

        assertEquals(items.drop(1), storeListDao.getStoreList(store))
    }
}