package com.fatflow.app.data.repo

import android.content.Context
import com.fatflow.app.data.TargetRecomputer
import com.fatflow.app.data.defaultSession
import com.fatflow.app.data.local.FatFlowDatabase
import com.fatflow.app.data.local.ProfileStore
import com.fatflow.app.data.local.dao.DayPlanDao
import com.fatflow.app.data.local.entity.DayPlanEntity
import com.fatflow.app.domain.SessionKind
import com.fatflow.app.work.ReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

class WeekPlanRepositoryImpl(
    private val context: Context,
    private val dao: DayPlanDao,
    private val recomputer: TargetRecomputer,
    private val database: FatFlowDatabase,
    private val profileStore: ProfileStore,
) : WeekPlanRepository {
    override fun observeWeek(start: LocalDate): Flow<List<DayPlanEntity>> =
        dao.observeRange(start, start.plusDays(6))

    override suspend fun setSession(date: LocalDate, kind: SessionKind) {
        val existing = dao.range(date, date).firstOrNull()
        val startTime =
            when (kind) {
                SessionKind.REST -> null
                else ->
                    existing?.startTime ?: defaultSession(date).second
                        ?: LocalTime.of(17, 0)
            }
        val entity = DayPlanEntity(date, kind, startTime)
        dao.upsert(entity)
        recomputer.recompute(LocalDate.now())
        scheduleReminder(date, entity)
    }

    override suspend fun setStartTime(date: LocalDate, time: LocalTime) {
        val existing =
            dao.range(date, date).firstOrNull()
                ?: DayPlanEntity(date, SessionKind.REST, time)
        val entity = existing.copy(startTime = time)
        dao.upsert(entity)
        scheduleReminder(date, entity)
    }

    private suspend fun scheduleReminder(date: LocalDate, plan: DayPlanEntity) {
        if (date != LocalDate.now()) return
        val target = database.dailyTargetDao().get(date) ?: return
        val metric = database.bodyMetricDao().latest() ?: return
        val logged =
            database.foodLogDao().forDay(date).sumOf { it.carbGrams.toDouble() }.toInt()
        ReminderScheduler.scheduleForPlan(
            context,
            profileStore,
            plan,
            target.carbGrams,
            logged,
            metric.leanMassLbs.toDouble(),
        )
    }
}