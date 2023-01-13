package com.kaaphi.yasla.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreDatabaseTest {
    private lateinit var storeListDao: StoreListDao
    private lateinit var db: StoreDatabase
    private lateinit var store: Store

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, StoreDatabase::class.java).build()
        storeListDao = db.getStoreListDao()

        store = storeListDao.insertStore(Store(name = "my store"))
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testBasicInsertUpdateAndGet() = runBlocking {
        val items = (1..5).map {
            storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "item $it", rank = "rank$it"))
        }

        assertEquals(items, storeListDao.getStoreList(store))

        storeListDao.updateStoreItems(items[0].copy(isInList = false))

        assertEquals(items.drop(1), storeListDao.getStoreList(store))
    }

    @Test
    fun testDuplicateNameInsert(): Unit = runBlocking {
        storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "my item", rank = "rank1"))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                storeListDao.insertStoreItem(
                    StoreItem(storeId = store.id, name = "my item", rank = "rank1")
                )
            }
        }
    }

    @Test
    fun testDuplicateRankUpdate(): Unit = runBlocking {
        storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "my item1", rank = "rank1"))
        val item = storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "my item2", rank = "rank2"))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                storeListDao.updateStoreItems(item.copy(rank = "rank1"))
            }
        }
    }

    @Test
    fun testGetFirst(): Unit = runBlocking {
        storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "my item2", rank = "rank2"))
        val item = storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "my item1", rank = "rank1"))
        storeListDao.insertStoreItem(StoreItem(storeId = store.id, name = "my item3", rank = "rank3"))

        assertEquals(item, storeListDao.getFirstItemByRank(store.id))
    }
}