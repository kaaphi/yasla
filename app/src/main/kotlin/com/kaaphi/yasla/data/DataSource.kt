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

class DataSource(private val context: Context) {
    suspend fun loadList() =
        context.listDataStore.data.map { list ->
            list.itemsList.map {
                ListItem(it.name, it.quantity, it.isChecked)
            }
        }.first()

    suspend fun saveList(list: List<ListItem>) =
        context.listDataStore.updateData { _ ->
            shoppingListData {
                items.addAll(list.map {
                    shoppingListItemData {
                        name = it.name
                        isChecked = it.isChecked
                        it.quantity?.also(::quantity::set)
                    }
                })
            }
        }
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