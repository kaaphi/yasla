package com.kaaphi.yasla.model

import android.app.Application
import android.os.Parcelable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaaphi.yasla.data.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListItem(
    val name: String,
    val quantity: String? = null,
    val isChecked: Boolean = false
) : Parcelable


class ShoppingListState(val application: Application) : ViewModel() {
    val list = mutableStateListOf<ListItem>()
    val editItemState = MutableStateFlow<ListItem?>(null)
    val datasource = DataSource(context = application)


    init {
        viewModelScope.launch(Dispatchers.IO) {
            list.addAll(datasource.loadList())
        }

        viewModelScope.launch(Dispatchers.IO) {
            snapshotFlow { list.toList() }.flowOn(Dispatchers.IO).collect {
                datasource.saveList(it)
            }
        }
    }

    fun editItem(item: ListItem?) {
        editItemState.value = item
    }

    fun moveItem(fromIdx: Int, toIdx: Int) = list.apply {
        add(toIdx, removeAt(fromIdx))
    }

    fun updateItem(item: ListItem, updateBlock: ListItem.() -> ListItem) {
        //there must be a better way
        list[list.indexOf(item)] = item.updateBlock()
    }

    fun deleteCheckedItems() {
        list.removeIf(ListItem::isChecked)
    }

    fun addItem(itemName: String) {
        list.add(0, ListItem(itemName))
    }
}

class ShoppingListStateFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShoppingListState(application) as T
    }
}