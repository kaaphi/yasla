package com.kaaphi.yasla.model

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.kaaphi.ranking.addByRank
import com.kaaphi.ranking.generateRanks
import com.kaaphi.ranking.getRank
import com.kaaphi.ranking.initialRank
import com.kaaphi.ranking.rankBetween
import com.kaaphi.yasla.data.Store
import com.kaaphi.yasla.data.StoreDatabase
import com.kaaphi.yasla.data.StoreItem
import com.kaaphi.yasla.data.createDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val MAX_RANK_LENGTH = 15

class ShoppingListState(val db: StoreDatabase) : ViewModel() {
    val dao = db.getStoreListDao()
    val isRebalancing = MutableStateFlow(false)
    val list = mutableStateListOf<StoreItem>()
    val store = MutableStateFlow<Store?>(null)
    val errors = MutableSharedFlow<String>(
        replay = 10,
        extraBufferCapacity = 10
    )

    internal suspend fun init() {
        try {
            val dbStore = dao.getStores().let {
                if(it.isEmpty()) {
                    Log.i("ShoppingListState", "Creating default store")
                    dao.insertStore(Store(name = "default"))
                } else {
                    it[0]
                }
            }

            withContext(Dispatchers.Main) {
                store.value = dbStore
            }

            rebalanceRanks()
        } catch (e: Throwable) {
            Log.e("ShoppingListState", "failed to load", e)
            errors.emit("Failed to load list data!")
        }
    }

    fun moveItemInView(fromIdx: Int, toIdx: Int) = list.apply {
        add(toIdx, removeAt(fromIdx))
    }

    suspend fun updateRankAfterMove(toIdx: Int) = list.apply {
        val newRank = calculateRankFor(toIdx)
        val updatedItem = get(toIdx).copy(rank = newRank)
        set(toIdx, updatedItem)
        dao.updateStoreItems(updatedItem)

        checkRebalanceRanks(updatedItem)
    }

    private suspend fun calculateRankFor(idx: Int) : String =
        calculateRankFor(idx-1, idx+1)

    private suspend fun calculateRankFor(beforeIdx: Int, afterIdx: Int) : String = with(list) {
        if(isEmpty()) {
            val firstItemRank = dao.getFirstItemByRank(store.value!!.id)
                ?.rank

            return if(firstItemRank == null) {
                initialRank()
            } else {
                "0" rankBetween firstItemRank
            }
        }

        val rankBefore = getRank(beforeIdx, StoreItem::rank)
        val rankAfter = getRank(afterIdx, StoreItem::rank)

        val proposedRank = rankBefore rankBetween rankAfter

        return if(dao.getItemByRank(store.value!!.id, proposedRank) != null) {
            Log.w("ShoppingListState","Item already has rank=$proposedRank, will calculate new rank!")
            val ranks = dao.getRanks(store.value!!.id, rankBefore, rankAfter)
            val rankIdx = ranks.binarySearch(proposedRank)
            check(rankIdx >= 0)
            if(rankIdx == 0) {
                "0" rankBetween proposedRank
            } else {
                ranks[rankIdx - 1] rankBetween proposedRank
            }
        } else {
            proposedRank
        }
    }

    private suspend fun checkRebalanceRanks(item: StoreItem) {
        if(item.rank.length > MAX_RANK_LENGTH) {
            rebalanceRanks()
        }
    }

    private suspend fun rebalanceRanks() {
        try {
            isRebalancing.value = true
            db.withTransaction {
                withContext(Dispatchers.Main) {
                    list.clear()
                }
                Log.i("ShoppingListState", "Start rebalance transaction")

                var data = dao.getStoreItems(store.value!!.id)
                data = generateRanks(data.size, startPaddingDivisor = 4, endPaddingDivisor = 10).zip(data.asSequence())
                    .map { (newRank, item) ->  item.copy(rank = newRank) }
                    .toList()
                dao.updateStoreItems(data)

                withContext(Dispatchers.Main) {
                    data
                        .filter { it.isInList }
                        .forEach {
                        list.add(it)
                    }
                }
                Log.i("ShoppingListState", "End rebalance transaction")
            }
        } finally {
            isRebalancing.value = false
        }
    }

    suspend fun updateItem(item: StoreItem, updateBlock: StoreItem.() -> StoreItem) {
        val newItem = item.updateBlock()
        if(item == newItem) {
            //no change, just return
            return
        }

        //check to see if we are about to insert a duplicate name
        if(item.name != newItem.name && dao.getItemByName(store.value!!.id, newItem.name) != null) {
            errors.emit("Item with name ${newItem.name} already exists!")
            return
        }

        //there must be a better way
        list[list.indexOf(item)] = newItem
        dao.updateStoreItems(newItem)
    }

    /**
     * Initiates a two-stage permanent delete action. Immediately when called, the item is removed
     * from the UI list, and then a pair of completion actions are returned. The first action will
     * finalize the delete action by removing the item from storage. The second action will NOT
     * remove the item from storage and will re-add the item to the UI list.
     */
    suspend fun deleteItem(item: StoreItem) : Pair<suspend () -> Unit, suspend () -> Unit> {
        list.remove(item)
        return Pair(
            {dao.deleteStoreItems(item)},
            {list.addByRank(item, StoreItem::rank)}
        )
    }

    suspend fun deleteCheckedItems() {
        val toRemove = list.filter(StoreItem::isChecked)
        list.removeAll(toRemove)
        dao.updateStoreItems(toRemove.map { it.copy(isInList = false, isChecked = false) })
    }

    suspend fun addItem(itemName: String) : StoreItem? {
        val currentItem = dao.getItemByName(store.value!!.id, itemName)

        if(currentItem?.isInList == true && list.contains(currentItem)) {
            errors.emit("Item $itemName is already in the list!")
            return currentItem
        } else if(currentItem != null) {
            return currentItem.copy(isInList = true).also { item ->
                dao.updateStoreItems(item)
                list.addByRank(item, StoreItem::rank)
            }
        } else {
            val rank = calculateRankFor(-1, 0)
            val item = dao.insertStoreItem(
                StoreItem(
                    storeId = store.value!!.id,
                    name = itemName,
                    rank = rank
                )
            )
            list.add(0, item)
            return item
        }
    }
}

class ShoppingListStateFactory(private val application: Application) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShoppingListState(createDb(context = application)).apply {
            viewModelScope.launch(Dispatchers.IO) {
                init()
            }
        } as T
//        try {
//            init()
//        } catch (th: Throwable) {
//            Log.e("ShoppingListState", "failed to init!", th)
//        }
    }
}