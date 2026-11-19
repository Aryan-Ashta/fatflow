package com.fatflow.app.data.repo

import com.fatflow.app.data.local.entity.DailyTargetEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TargetRepository {
    fun observe(date: LocalDate): Flow<DailyTargetEntity?>

    fun observeWeek(start: LocalDate): Flow<List<DailyTargetEntity>>

    suspend fun overrideDayType(date: LocalDate)
}