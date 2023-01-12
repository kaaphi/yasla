package com.kaaphi.yasla

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.kaaphi.yasla.data.StoreItem
import com.kaaphi.yasla.ui.theme.YaslaTheme
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShoppingListComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun myTest() {
        val item = mutableStateOf(StoreItem(storeId = 0, name = "my item", rank = "none"))
        composeTestRule.setContent {
            YaslaTheme {
                ListItemCheckbox(item = item.value, onCheckedChange = {
                    item.value = item.value.copy(isChecked = it)
                },
                    onEditItemClicked = {},
                    onDeleteItemClicked = {},
                )
            }
        }

        composeTestRule.onNode(hasTextExactly("my item"), useUnmergedTree = true).performClick()
        assertTrue(item.value.isChecked)

        composeTestRule.onNode(hasTestTag("CheckItem"), useUnmergedTree = true).performClick()
        assertFalse(item.value.isChecked)

        composeTestRule.onNode(hasTestTag("CheckSpacer"), useUnmergedTree = true).performClick()
        assertTrue(item.value.isChecked)
    }
}