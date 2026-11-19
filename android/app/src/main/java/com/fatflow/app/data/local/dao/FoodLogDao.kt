package com.fatflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fatflow.app.data.local.entity.FoodLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY id")
    fun observeDay(date: LocalDate): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY id")
    suspend fun forDay(date: LocalDate): List<FoodLogEntity>

    @Insert
    suspend fun insert(entry: FoodLogEntity): Long

    @Query("SELECT * FROM food_logs WHERE date = :date AND isPreWorkout = 1")
    suspend fun preWorkoutOn(date: LocalDate): List<FoodLogEntity>

    @Query("SELECT SUM(calories) FROM food_logs WHERE date = :date")
    suspend fun sumCalories(date: LocalDate): Int?
}