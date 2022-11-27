package com.kaaphi.yasla.model

import android.os.Parcelable
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.kaaphi.yasla.data.DataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListItem(
    val name: String,
    val isChecked: Boolean = false
) : Parcelable


class ShoppingListState : ViewModel() {
    val list = mutableStateListOf<ListItem>()

    init {
        list.addAll(DataSource().loadList())
    }

    fun moveItem(fromIdx: Int, toIdx: Int) = list.apply {
        add(toIdx, removeAt(fromIdx))
    }
}