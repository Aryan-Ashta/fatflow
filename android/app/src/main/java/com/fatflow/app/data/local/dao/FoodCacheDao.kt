package com.fatflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fatflow.app.data.local.entity.FoodCacheEntity

@Dao
interface FoodCacheDao {
    @Query("SELECT * FROM food_cache WHERE barcode = :code LIMIT 1")
    suspend fun byBarcode(code: String): FoodCacheEntity?

    @Query(
        "SELECT * FROM food_cache WHERE name LIKE '%' || :query || '%' LIMIT 8",
    )
    suspend fun searchName(query: String): List<FoodCacheEntity>

    @Insert
    suspend fun insert(entity: FoodCacheEntity): Long
}