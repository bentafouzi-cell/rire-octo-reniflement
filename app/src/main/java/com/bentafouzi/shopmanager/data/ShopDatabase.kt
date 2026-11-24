package com.bentafouzi.shopmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Product::class, Sale::class, Expense::class], version = 1)
abstract class ShopDatabase : RoomDatabase() {
    abstract fun dao(): ShopDao

    companion object {
        @Volatile private var INSTANCE: ShopDatabase? = null

        fun get(context: Context): ShopDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                ShopDatabase::class.java,
                "shop_manager.db"
            ).build().also { INSTANCE = it }
        }
    }
}
