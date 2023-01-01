package com.kaaphi.yasla.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
data class Store(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "store_id")
  val id: Long = 0,
  @ColumnInfo(name= "store_name")
  val name: String,
)

@Entity(indices = [
  Index(value = ["storeItem_storeId", "rank"], unique = true),
  Index(value = ["storeItem_storeId", "storeItem_name"], unique = true),
])
@Parcelize
data class StoreItem(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "storeItem_id")
  val id: Long = 0,
  @ColumnInfo(name = "storeItem_storeId")
  val storeId: Long,
  @ColumnInfo(name = "storeItem_name")
  val name: String,
  val quantity: String? = null,
  val rank: String,
  val isInList: Boolean = true,
  val isChecked: Boolean = false,

) : Parcelable

