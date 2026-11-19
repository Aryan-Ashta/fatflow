package com.fatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_cache",
    indices = [Index(value = ["barcode"])],
)
data class FoodCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String? = null,
    val source: String,
    val name: String,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbPer100g: Float,
    val fatPer100g: Float,
)