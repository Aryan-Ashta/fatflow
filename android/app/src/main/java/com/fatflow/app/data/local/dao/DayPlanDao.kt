package com.fatflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fatflow.app.data.local.entity.DayPlanEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DayPlanDao {
    @Query("SELECT * FROM day_plans WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayPlanEntity>>

    @Query("SELECT * FROM day_plans WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun range(start: LocalDate, end: LocalDate): List<DayPlanEntity>

    @Upsert
    suspend fun upsert(entity: DayPlanEntity)
}