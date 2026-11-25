package com.bentafouzi.shopmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Product::class, Sale::class, Expense::class], version = 2)
abstract class ShopDatabase : RoomDatabase() {
    abstract fun dao(): ShopDao

    companion object {
        @Volatile private var INSTANCE: ShopDatabase? = null

        /** Ajoute la colonne code-barres sans effacer les produits deja enregistres. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): ShopDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                ShopDatabase::class.java,
                "shop_manager.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
        }
    }
}
