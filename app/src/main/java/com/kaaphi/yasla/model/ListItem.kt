package com.kaaphi.yasla.model

import android.os.Parcelable
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.kaaphi.yasla.data.DataSource
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListItem(
    val position: Int,
    val name: String,
    val isChecked: Boolean = false
) : Parcelable


class ShoppingListState : ViewModel() {
    val list = mutableStateListOf<ListItem>()

    init {
        //TODO, this needs to be async and actual load from a real place
        list.addAll(DataSource().loadList())
    }

    fun dragItem(fromIdx: Int, toIdx: Int) = list.apply {
        add(toIdx, removeAt(fromIdx))
    }

    fun endDragItem(startIdx: Int, endIdx: Int) {
        for(idx in startIdx..endIdx) {
            updateItem(idx, list[idx].copy(position = idx))
        }
    }

    fun updateItem(idx: Int, newItem: ListItem) {
        list[idx] = newItem
    }

    fun deleteCheckedItems() {
        list.removeIf(ListItem::isChecked)
    }
}