package com.kaaphi.yasla.data

import com.kaaphi.yasla.model.ListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSourceTest {

    @Test
    fun testListItemToShoppingListDataItem() {
        val itemData = ListItem("blob", null, false).toShoppingListItemData()
        assertEquals("blob", itemData.name)
        assertFalse(itemData.isChecked)
        assertTrue(itemData.quantity.isEmpty())
    }

    @Test
    fun testShoppingListItemDataToListItem() {
        val listItem = shoppingListItemData {
            name = "blob"
            isChecked = false
        }.toListItem()

        assertEquals(ListItem("blob", null, false), listItem)
    }
}