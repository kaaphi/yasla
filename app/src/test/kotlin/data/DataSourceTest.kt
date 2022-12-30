package data

import com.kaaphi.yasla.data.shoppingListItemData
import com.kaaphi.yasla.data.toListItem
import com.kaaphi.yasla.data.toShoppingListItemData
import com.kaaphi.yasla.model.ListItem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beEmpty

class DataSourceTest: FunSpec({
    test("ListItemToShoppingListDataItem") {
        val itemData = ListItem("blob", null, false).toShoppingListItemData()
        itemData.name shouldBe "blob"
        itemData.isChecked shouldBe false
        itemData.quantity should beEmpty()
    }

    test("ShoppingListItemDataToListItem") {
        val listItem = shoppingListItemData {
            name = "blob"
            isChecked = false
        }.toListItem()

        listItem shouldBe ListItem("blob", null, false)
    }
})