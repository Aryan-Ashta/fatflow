package com.fatflow.app.data.repo

import com.fatflow.app.data.TargetRecomputer
import com.fatflow.app.data.local.dao.DailyTargetDao
import com.fatflow.app.data.local.entity.DailyTargetEntity
import com.fatflow.app.domain.DayType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class TargetRepositoryImpl(
    private val dao: DailyTargetDao,
    private val recomputer: TargetRecomputer,
) : TargetRepository {
    override fun observe(date: LocalDate): Flow<DailyTargetEntity?> = dao.observe(date)

    override fun observeWeek(start: LocalDate): Flow<List<DailyTargetEntity>> =
        dao.observeRange(start, start.plusDays(6))

    override suspend fun overrideDayType(date: LocalDate) {
        val existing = dao.get(date) ?: return
        val next =
            when (existing.dayType) {
                DayType.HIGH_CARB -> DayType.MOD_CARB
                DayType.MOD_CARB -> DayType.LOW_CARB
                DayType.LOW_CARB -> DayType.HIGH_CARB
            }
        dao.upsert(existing.copy(dayType = next, dayTypeOverridden = true))
        recomputer.recompute(date)
    }
}