package com.kaaphi.yasla.model

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaaphi.ranking.addByRank
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


class ShoppingListState(val db: StoreDatabase) : ViewModel() {
    val dao = db.getStoreListDao()
    val list = mutableStateListOf<StoreItem>()
    val store = MutableStateFlow<Store?>(null)
    val itemNames = mutableSetOf<String>()
    val editItemState = MutableStateFlow<StoreItem?>(null)
    val errors = MutableSharedFlow<String>(
        replay = 10,
        extraBufferCapacity = 10
    )


    init {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val dbStore = dao.getStores().let {
                        if(it.isEmpty()) {
                            Log.i("ShoppingListState", "Creating default store")
                            dao.insertStore(Store(name = "default"))
                        } else {
                            it[0]
                        }
                    }

                    val data = dao.getStoreList(dbStore)
                    withContext(Dispatchers.Main) {
                        store.value = dbStore
                        data.forEach {
                            if(itemNames.add(it.name)) {
                                list.add(it)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("ShoppingListState", "failed to load", e)
                    errors.emit("Failed to load list data!")
                }
            }
        } catch (th: Throwable) {
            Log.e("ShoppingListState", "failed to init!", th)
        }
    }

    fun editItem(item: StoreItem?) {
        editItemState.value = item
    }

    fun moveItemInView(fromIdx: Int, toIdx: Int) = list.apply {
        add(toIdx, removeAt(fromIdx))
    }

    suspend fun updateRankAfterMove(toIdx: Int) = list.apply {
        val newRank = calculateRankFor(toIdx)
        val updatedItem = get(toIdx).copy(rank = newRank)
        set(toIdx, updatedItem)
        dao.updateStoreItems(updatedItem)
    }

    private suspend fun calculateRankFor(idx: Int) : String =
        calculateRankFor(idx-1, idx+1)

    private suspend fun calculateRankFor(beforeIdx: Int, afterIdx: Int) : String = with(list) {
        if(isEmpty()) {
            return initialRank()
        }

        val rankBefore = getRank(beforeIdx, StoreItem::rank)
        val rankAfter = getRank(afterIdx, StoreItem::rank)

        val proposedRank = rankBefore rankBetween rankAfter

        return if(dao.getItemByRank(store.value!!.id, proposedRank) != null) {
            Log.w("ShoppingListState","Item already has rank=$proposedRank, will calculate new rank!")
            val ranks = dao.getRanks(store.value!!.id, rankBefore, rankAfter)
            val rankIdx = ranks.binarySearch(proposedRank)
            check(rankIdx >= 0)
            ranks[rankIdx-1] rankBetween proposedRank
        } else {
            proposedRank
        }
    }

    private suspend fun rebalanceRanks() {
        TODO()
    }

    suspend fun updateItem(item: StoreItem, updateBlock: StoreItem.() -> StoreItem) {
        val newItem = item.updateBlock()

        require(newItem.name == item.name)

        //there must be a better way
        list[list.indexOf(item)] = newItem
        dao.updateStoreItems(newItem)
    }

    suspend fun deleteCheckedItems() {
        val toRemove = list.filter(StoreItem::isChecked)
        list.removeAll(toRemove)
        itemNames.removeAll(toRemove.map(StoreItem::name).toSet())
        dao.updateStoreItems(toRemove.map { it.copy(isInList = false, isChecked = false) })
    }

    suspend fun addItem(itemName: String) : StoreItem? {
        if(itemNames.add(itemName)) {
            val currentItem = dao.getItemByName(store.value!!.id, itemName)?.copy(isInList = true)

            if(currentItem != null) {
                dao.updateStoreItems(currentItem)
                list.addByRank(currentItem, StoreItem::rank)
                return currentItem
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

        } else {
            errors.emit("Item $itemName is already in the list!")
            return null
        }
    }
}

class ShoppingListStateFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShoppingListState(createDb(context = application)) as T
    }
}
