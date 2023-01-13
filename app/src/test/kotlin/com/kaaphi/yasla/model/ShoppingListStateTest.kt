package com.kaaphi.yasla.model

import android.util.Log
import androidx.room.withTransaction
import com.kaaphi.ranking.generateRanks
import com.kaaphi.yasla.data.Store
import com.kaaphi.yasla.data.StoreDatabase
import com.kaaphi.yasla.data.StoreItem
import com.kaaphi.yasla.data.StoreListDao
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.equalityMatcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

private const val DEFAULT_STORE_ID = 1L

class ShoppingListStateTest : FunSpec({
    val db = mockk<StoreDatabase>()
    val dao = InMemoryDao()
    lateinit var state : ShoppingListState


    suspend fun listShouldBe(storeId: Long, vararg itemNames: String) {
        state.list.shouldHaveItems(*itemNames)
        dao.shouldHaveList(storeId, *itemNames)
    }

    suspend fun listShouldBe(vararg itemNames: String) =
        listShouldBe(DEFAULT_STORE_ID, *itemNames)


    beforeSpec {
        mockkStatic(Log::class)
        every { Log.d(any(), any())} answers (MockKAnswerScope<Int,Int>::logMock)
        every { Log.i(any(), any())} answers (MockKAnswerScope<Int,Int>::logMock)
        every { Log.e(any(), any(), any()) } answers (MockKAnswerScope<Int,Int>::logMock)

        mockkStatic("androidx.room.RoomDatabaseKt")

        val slot = slot<suspend () -> Unit>()

        every { db.getStoreListDao() } returns dao
        coEvery { db.withTransaction(capture(slot)) } coAnswers  {
            slot.captured()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    beforeEach {
        Dispatchers.setMain(StandardTestDispatcher(coroutineContext[TestCoroutineScheduler]!!))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    afterEach {
        Dispatchers.resetMain()
    }

    context("start empty") {
        beforeEach {
            dao.reset()
            state = ShoppingListState(db)
            state.init()
        }

        test("add one item") {
            state.addItem("first item")

            listShouldBe("first item")
        }

        test("empty list") {
            listShouldBe()
        }
    }

    context("start with three items, none in list") {
        beforeEach {
            dao.reset()
            dao.insertStore(Store(name = "default"))
            listOf("one", "two", "three")
                .zip(generateRanks(3).asIterable())
                .map { StoreItem(storeId = 1, name = it.first, rank = it.second, isInList = false) }
                .forEach { dao.insertStoreItem(it) }
            state = ShoppingListState(db)
            state.init()
        }

        test("add one new item") {
            state.addItem("new item")

            listShouldBe("new item")

            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("new item", "one", "two", "three")
        }

        test("add one existing item") {
            state.addItem("two")

            listShouldBe("two")
            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("one", "two", "three")
        }

        test("init") {
            listShouldBe()
        }
    }

    context("start with three items, all in list") {
        beforeEach {
            dao.reset()
            dao.insertStore(Store(name = "default"))
            listOf("one", "two", "three")
                .zip(generateRanks(3).asIterable())
                .map { StoreItem(storeId = 1, name = it.first, rank = it.second, isInList = true) }
                .forEach { dao.insertStoreItem(it) }
            state = ShoppingListState(db)
            state.init()
        }

        test("add one new item") {
            state.addItem("new item")

            listShouldBe("new item", "one", "two", "three")
            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("new item", "one", "two", "three")
        }

        test("update item") {
            val one = state.list[0]
            state.updateItem(one) {
                copy(quantity = "test quantity")
            }

            state.list[0].quantity shouldBe "test quantity"
            dao.getItemByName(DEFAULT_STORE_ID, "one")?.quantity shouldBe "test quantity"
        }

        test("remove checked items") {
            val two = state.list[1]
            state.updateItem(two) {
                copy(isChecked = true)
            }

            state.deleteCheckedItems()

            listShouldBe("one", "three")
            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("one", "two", "three")
        }

        test("undo permanently delete") {
            val action = state.deleteItem(state.list[1])

            state.list.shouldHaveItems("one", "three")
            dao.shouldHaveList(DEFAULT_STORE_ID, "one", "two", "three")
            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("one", "two", "three")

            action.second()

            listShouldBe("one", "two", "three")
            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("one", "two", "three")
        }

        test("finalize permanently delete") {
            val action = state.deleteItem(state.list[1])

            state.list.shouldHaveItems("one", "three")
            dao.shouldHaveList(DEFAULT_STORE_ID, "one", "two", "three")
            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("one", "two", "three")

            action.first()

            listShouldBe("one", "three")
            dao.getStoreItems(DEFAULT_STORE_ID).shouldHaveItems("one", "three")
        }

        listOf(
            Triple(0, 1, arrayOf("two", "one", "three")),
            Triple(1, 0, arrayOf("two", "one", "three")),
            Triple(0, 2, arrayOf("two", "three", "one")),
            Triple(2, 0, arrayOf("three", "one", "two")),
            Triple(1, 2, arrayOf("one", "three", "two")),
            Triple(2, 1, arrayOf("one", "three", "two"))
        ).forEach { (fromIdx, toIdx, expected) ->
            test("move in view, then finalize $fromIdx -> $toIdx") {
                state.moveItemInView(fromIdx, toIdx)

                state.list.shouldHaveItems(*expected)
                dao.shouldHaveList(DEFAULT_STORE_ID, "one", "two", "three")

                state.updateRankAfterMove(toIdx)
                listShouldBe(*expected)
            }
        }

        test("init") {
            listShouldBe("one", "two", "three")
        }
    }
}) {
    init {
        coroutineTestScope = true
    }


}

fun haveItems(vararg items: String) = Matcher<List<StoreItem>> { value ->
    equalityMatcher(items.toList()).test(value.map(StoreItem::name))
}

fun List<StoreItem>.shouldHaveItems(vararg items: String) =
    this should haveItems(*items)

suspend fun InMemoryDao.shouldHaveList(storeId: Long, vararg items: String) =
    this.getStoreList(storeId) should haveItems(*items)

suspend fun InMemoryDao.shouldHaveItems(storeId: Long, vararg items: String) =
    this.getStoreItems(storeId) should haveItems(*items)


fun MockKAnswerScope<Int,Int>.logMock(it: Call) : Int {
    println("${it.invocation.method} ${it.invocation.args}")
    it.invocation.args.filterIsInstance<Throwable>().forEach(Throwable::printStackTrace)

    return 0
}

class InMemoryDao : StoreListDao {
    val storeId = AtomicLong(0)
    val itemId = AtomicLong(0)
    val stores = mutableMapOf<Long, Store>()
    val items = mutableMapOf<Long, MutableList<StoreItem>>()

    fun reset() {
        storeId.set(0)
        itemId.set(0)
        stores.clear()
        items.clear()
    }

    override suspend fun getStores(): List<Store> =
        stores.values.toList()

    override suspend fun getStoreList(storeId: Long): List<StoreItem> =
        items[storeId]?.filter(StoreItem::isInList)?.sortedBy(StoreItem::rank) ?: emptyList()

    override suspend fun getStoreItems(storeId: Long): List<StoreItem> =
        items[storeId]?.sortedBy(StoreItem::rank) ?: emptyList()

    override suspend fun getFirstItemByRank(storeId: Long): StoreItem? =
        getStoreItems(storeId).firstOrNull()


    override suspend fun insertStoreAndReturnId(store: Store): Long =
        storeId.incrementAndGet().also {
            stores[it] = store.copy(id = it)
            items[it] = mutableListOf()
        }

    override suspend fun getItemByName(storeId: Long, name: String): StoreItem? =
        items[storeId]?.find { it.name == name }

    override suspend fun getItemByRank(storeId: Long, rank: String): StoreItem? =
        items[storeId]?.find { it.rank == rank }

    override suspend fun getRanks(storeId: Long, startRank: String, endRank: String): List<String> =
        items[storeId]?.map { it.rank }?.filter { it in startRank..endRank }?.sorted() ?: emptyList()

    override suspend fun insertStoreItemAndReturnId(item: StoreItem): Long =
        itemId.incrementAndGet().also {
            items[item.storeId]!!.add(item.copy(id = it))
        }

    override suspend fun updateStoreItems(vararg items: StoreItem) {
        items.forEach(this::updateStoreItem)
    }

    override suspend fun updateStoreItems(items: List<StoreItem>) {
        items.forEach(this::updateStoreItem)
    }

    private fun updateStoreItem(item: StoreItem) {
        items[item.storeId]?.removeIf { it.id == item.id}
        items[item.storeId]?.add(item)
    }

    override suspend fun deleteStoreItems(vararg items: StoreItem) {
        items.forEach { item ->
            this.items[item.storeId]?.remove(item)
        }
    }

}