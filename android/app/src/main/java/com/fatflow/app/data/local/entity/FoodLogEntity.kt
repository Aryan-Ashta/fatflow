package com.fatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "food_logs",
    indices = [Index(value = ["date"])],
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mealName: String,
    val isPreWorkout: Boolean = false,
    val calories: Int,
    val proteinGrams: Float,
    val carbGrams: Float,
    val fatGrams: Float,
    val source: String,
    val barcode: String? = null,
    val grams: Float? = null,
)