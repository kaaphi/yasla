package com.kaaphi.yasla.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.kaaphi.yasla.model.ListItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream

class DataSource(private val context: Context) {
    suspend fun loadList() =
        context.listDataStore.data.map { list ->
            list.itemsList.map {
                ListItem(it.name, it.quantity, it.isChecked)
            }
        }.first()

    suspend fun saveList(list: List<ListItem>) =
        context.listDataStore.updateData { _ ->
            ShoppingListData.newBuilder()
                .addAllItems(list.map {
                    val builder =  ShoppingListItemData.newBuilder()
                        .setName(it.name)
                        .setIsChecked(it.isChecked)
                    it.quantity?.also(builder::setQuantity)
                    builder.build()
                })
                .build()
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