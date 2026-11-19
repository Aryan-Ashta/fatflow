package com.fatflow.app.data.repo

import com.fatflow.app.data.local.entity.FoodLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface FoodLogRepository {
    fun observeDay(date: LocalDate): Flow<List<FoodLogEntity>>

    suspend fun log(entry: FoodLogEntity): Long

    suspend fun repeatYesterdayPreWorkout(today: LocalDate): Int
}