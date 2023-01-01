package com.kaaphi.yasla.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Store::class, StoreItem::class], version = 1)
abstract class StoreDatabase : RoomDatabase() {
    abstract fun getStoreListDao(): StoreListDao
}

fun createDb(context: Context) =
    Room.databaseBuilder(context, StoreDatabase::class.java, "data.db")
        .build()
