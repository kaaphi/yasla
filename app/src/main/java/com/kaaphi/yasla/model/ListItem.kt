package com.kaaphi.yasla.model

import android.os.Parcelable
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.kaaphi.yasla.data.DataSource
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListItem(
    val name: String,
    val isChecked: Boolean = false
) : Parcelable


class ShoppingListState : ViewModel() {
    val list = mutableStateListOf<ListItem>()

    init {
        //TODO, this needs to be async and actual load from a real place
        list.addAll(DataSource().loadList())
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