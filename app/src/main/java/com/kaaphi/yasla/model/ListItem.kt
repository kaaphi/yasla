package com.kaaphi.yasla.model

import android.app.Application
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaaphi.yasla.data.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListItem(
    val name: String,
    val quantity: String? = null,
    val isChecked: Boolean = false
) : Parcelable


class ShoppingListState(val application: Application) : ViewModel() {
    val list = mutableStateListOf<ListItem>()
    val itemNames = mutableSetOf<String>()
    val editItemState = MutableStateFlow<ListItem?>(null)
    val datasource = DataSource(context = application)
    val errors = MutableSharedFlow<String>(
        replay = 10,
        extraBufferCapacity = 10
    )


    init {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val data = datasource.loadList()
                    withContext(Dispatchers.Main) {
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

                //only start saving after we've loaded
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        snapshotFlow { list.toList() }
                            .flowOn(Dispatchers.Main)
                            .conflate()
                            .collect {
                                try {
                                    datasource.saveList(it)
                                } catch (e: Throwable) {
                                    Log.e("ShoppingListState", "failed to save!", e)
                                    errors.emit("Failed to save list data!")
                                }
                            }
                    } catch (e: Throwable) {
                        Log.e("ShoppingListState", "failed to collect state for saving", e)
                        errors.emit("Failed to initialize data saving!")
                    }
                }
            }
        } catch (th: Throwable) {
            Log.e("ShoppingListState", "failed to init!", th)
        }
    }

    fun editItem(item: ListItem?) {
        editItemState.value = item
    }

    fun moveItem(fromIdx: Int, toIdx: Int) = list.apply {
        add(toIdx, removeAt(fromIdx))
    }

    fun updateItem(item: ListItem, updateBlock: ListItem.() -> ListItem) {
        val newItem = item.updateBlock()

        require(newItem.name == item.name)

        //there must be a better way
        list[list.indexOf(item)] = newItem
    }

    fun deleteCheckedItems() {
        list.removeIf {
            if(it.isChecked) {
                itemNames.remove(it.name)
                true
            } else {
                false
            }
        }
    }

    suspend fun addItem(itemName: String) {
        if(itemNames.add(itemName)) {
            list.add(0, ListItem(itemName))
        } else {
            errors.emit("Item $itemName is already in the list!")
        }
    }
}

class ShoppingListStateFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShoppingListState(application) as T
    }
}