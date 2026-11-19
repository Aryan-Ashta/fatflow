package com.fatflow.app.data.repo

import com.fatflow.app.data.local.entity.DayPlanEntity
import com.fatflow.app.domain.SessionKind
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface WeekPlanRepository {
    fun observeWeek(start: LocalDate): Flow<List<DayPlanEntity>>

    suspend fun setSession(date: LocalDate, kind: SessionKind)

    suspend fun setStartTime(date: LocalDate, time: LocalTime)
}