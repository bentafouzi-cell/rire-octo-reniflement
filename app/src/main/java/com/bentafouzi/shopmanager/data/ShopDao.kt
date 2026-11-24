package com.bentafouzi.shopmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM products ORDER BY name")
    fun observeProducts(): Flow<List<Product>>

    @Insert
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET quantity = quantity - :quantity WHERE id = :id AND quantity >= :quantity")
    suspend fun decreaseStock(id: Long, quantity: Int): Int

    @Insert
    suspend fun insertSale(sale: Sale)

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun observeSales(): Flow<List<Sale>>

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeExpenses(): Flow<List<Expense>>
}
