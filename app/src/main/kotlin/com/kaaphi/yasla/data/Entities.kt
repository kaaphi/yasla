package com.kaaphi.yasla.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Store(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "store_id")
  val id: Long = 0,
  @ColumnInfo(name= "store_name")
  val name: String,
)

@Entity
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
)

