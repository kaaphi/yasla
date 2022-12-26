package com.kaaphi.yasla.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.kaaphi.yasla.model.ListItem
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class DataSource(private val dataStore: DataStore<ShoppingListData>) {
    constructor(context: Context): this(context.listDataStore)

    suspend fun loadList() =
        dataStore.data.map(ShoppingListData::toListItemList).first()

    suspend fun saveList(list: List<ListItem>) =
        dataStore.updateData {
            list.toShoppingListData()
        }
}

fun ShoppingListData.toListItemList() : List<ListItem> =
    itemsList.map(ShoppingListItemData::toListItem)

fun ShoppingListItemData.toListItem() : ListItem =
    ListItem(name, quantity.ifEmpty { null }, isChecked)

fun ListItem.toShoppingListItemData() : ShoppingListItemData =
    shoppingListItemData {
        name = this@toShoppingListItemData.name
        isChecked = this@toShoppingListItemData.isChecked
        this@toShoppingListItemData.quantity?.also {
            quantity = it
        }
    }

fun List<ListItem>.toShoppingListData() =
    shoppingListData {
        items.addAll(map(ListItem::toShoppingListItemData))
    }


object ListDataSerializer : Serializer<ShoppingListData> {
    override val defaultValue: ShoppingListData
        get() = ShoppingListData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ShoppingListData {
        try {
            return ShoppingListData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: ShoppingListData,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.listDataStore: DataStore<ShoppingListData> by dataStore(
    fileName = "list_data.pb",
    serializer = ListDataSerializer
)