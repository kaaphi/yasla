package com.kaaphi.yasla.data

import com.kaaphi.yasla.model.ListItem

class DataSource {
    fun loadList() =
        List(50) { ListItem(it, "List Item $it") }
}