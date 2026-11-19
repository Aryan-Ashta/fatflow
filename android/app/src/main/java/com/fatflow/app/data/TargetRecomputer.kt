package com.fatflow.app.data

import com.fatflow.app.data.local.ProfileStore
import com.fatflow.app.data.local.dao.BodyMetricDao
import com.fatflow.app.data.local.dao.DailyTargetDao
import com.fatflow.app.data.local.dao.DayPlanDao
import com.fatflow.app.data.local.dao.FoodLogDao
import com.fatflow.app.data.local.entity.DailyTargetEntity
import com.fatflow.app.data.local.entity.DayPlanEntity
import com.fatflow.app.domain.DayObservation
import com.fatflow.app.domain.DayType
import com.fatflow.app.domain.MetabolicEngine
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class TargetRecomputer(
    private val profileStore: ProfileStore,
    private val dailyTargetDao: DailyTargetDao,
    private val dayPlanDao: DayPlanDao,
    private val foodLogDao: FoodLogDao,
    private val bodyMetricDao: BodyMetricDao,
) {
    suspend fun recompute(today: LocalDate) {
        val profile = profileStore.profile.first()
        if (!profile.onboardingComplete) return

        closeDietBreakIfNeeded(today, profile)

        val latestMetric =
            bodyMetricDao.latest()
                ?: return
        val lbm = latestMetric.leanMassLbs.toDouble()
        val fm = latestMetric.fatMassLbs.toDouble()
        val bf = latestMetric.bodyFatPct.toDouble()

        val observations = buildObservations(today)
        val previousTdee =
            dailyTargetDao.latestTdeeBefore(today)
                ?: MetabolicEngine.seedTdee(lbm)
        val tdee = MetabolicEngine.updateAdaptiveTdee(previousTdee, observations).tdee

        val rollingFm = rollingFatMass(today, profile.startFatMassLbs.toDouble())
        val steps = MetabolicEngine.escalationSteps(profile.startFatMassLbs.toDouble(), rollingFm)

        var dietBreakStart = profile.dietBreakStart.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val breakDue =
            MetabolicEngine.dietBreakDue(
                profile.dietBreakAnchorWeightLbs.toDouble(),
                latestMetric.emaWeightLbs.toDouble(),
                profile.dietBreakAnchorBfPct.toDouble(),
                bf,
            )
        if (breakDue && dietBreakStart == null) {
            dietBreakStart = today
            profileStore.setDietBreakStart(today)
        }
        val activeBreakStart = dietBreakStart

        val weekStart = mondayOf(today)
        ensurePlans(weekStart, weekStart.plusDays(13))

        val baseline =
            MetabolicEngine.calorieTarget(
                tdee,
                MetabolicEngine.maxFatReleaseKcal(fm, profile.releaseRate),
                MetabolicEngine.safeFloorKcal(lbm),
            )
        val maxRelease = MetabolicEngine.maxFatReleaseKcal(fm, profile.releaseRate)

        val datesToFill = buildList {
            for (i in 0 until 7) add(weekStart.plusDays(i.toLong()))
            for (i in 1..6) {
                val extra = weekStart.plusDays(6 + i.toLong())
                if (extra > weekStart.plusDays(6)) add(extra)
            }
        }

        val weekDates = (0 until 7).map { weekStart.plusDays(it.toLong()) }
        val weekPlans = dayPlanDao.range(weekStart, weekStart.plusDays(6))
        val dayTypes = weekDates.map { date ->
            val existing = dailyTargetDao.get(date)
            val suggested =
                MetabolicEngine.sessionToDayType(
                    weekPlans.firstOrNull { it.date == date }?.sessionKind
                        ?: defaultSession(date).first,
                )
            if (existing?.dayTypeOverridden == true) existing.dayType else suggested
        }

        val breakFlags = weekDates.map { date ->
            val start = activeBreakStart
            start != null && !date.isBefore(start) && !date.isAfter(start.plusDays(6))
        }
        val nonBreakTypes = dayTypes.filterIndexed { index, _ -> !breakFlags[index] }
        val nonBreakCalories =
            if (nonBreakTypes.isEmpty()) {
                weekDates.map { tdee }
            } else {
                val split = MetabolicEngine.weeklyCalories(baseline, nonBreakTypes)
                val bonusPool = steps * 2 * profile.releaseRate * nonBreakTypes.size
                val bonus = MetabolicEngine.distributeTrainingBonus(nonBreakTypes, bonusPool)
                val merged = split.mapIndexed { i, cal -> cal + bonus[i] }
                var j = 0
                weekDates.mapIndexed { index, _ ->
                    if (breakFlags[index]) tdee else merged[j++]
                }
            }

        upsertTargetsForDates(
            datesToFill,
            weekDates,
            weekPlans,
            tdee,
            maxRelease,
            lbm,
            nonBreakCalories,
            activeBreakStart,
            baseline,
        )
    }

    private suspend fun upsertTargetsForDates(
        datesToFill: List<LocalDate>,
        weekDates: List<LocalDate>,
        weekPlans: List<DayPlanEntity>,
        tdee: Int,
        maxRelease: Int,
        lbm: Double,
        weekCalories: List<Int>,
        breakStart: LocalDate?,
        baseline: Int,
    ) {
        for (date in datesToFill) {
            val existing = dailyTargetDao.get(date)
            val suggested =
                MetabolicEngine.sessionToDayType(
                    weekPlans.firstOrNull { it.date == date }?.sessionKind
                        ?: dayPlanDao.range(date, date).firstOrNull()?.sessionKind
                        ?: defaultSession(date).first,
                )
            val dayType =
                if (existing?.dayTypeOverridden == true) {
                    existing.dayType
                } else {
                    suggested
                }
            val onBreak =
                breakStart != null &&
                    !date.isBefore(breakStart) &&
                    !date.isAfter(breakStart.plusDays(6))
            val calories =
                when {
                    onBreak -> tdee
                    date in weekDates -> weekCalories[weekDates.indexOf(date)]
                    else -> {
                        val type = dayType
                        MetabolicEngine.weeklyCalories(baseline, listOf(type)).first()
                    }
                }
            val macros = MetabolicEngine.macros(calories, lbm, dayType)
            dailyTargetDao.upsert(
                DailyTargetEntity(
                    date = date,
                    dayType = dayType,
                    dayTypeOverridden = existing?.dayTypeOverridden == true,
                    estimatedTdee = tdee,
                    maxFatReleaseKcal = maxRelease,
                    targetCalories = calories,
                    proteinGrams = macros.proteinGrams,
                    carbGrams = macros.carbGrams,
                    fatGrams = macros.fatGrams,
                    isDietBreak = onBreak,
                ),
            )
        }
    }

    private suspend fun rollingFatMass(today: LocalDate, fallback: Double): Double {
        val start = today.minusDays(6)
        val metrics = bodyMetricDao.range(start, today)
        return if (metrics.isEmpty()) fallback else metrics.map { it.fatMassLbs.toDouble() }.average()
    }

    private suspend fun buildObservations(today: LocalDate): List<DayObservation> {
        val start = today.minusDays(13)
        return (0..13).map { offset ->
            val date = start.plusDays(offset.toLong())
            val intake = foodLogDao.sumCalories(date)
            val weight = bodyMetricDao.range(date, date).firstOrNull()?.rawWeightLbs?.toDouble()
            DayObservation(date, weight, intake)
        }
    }

    private suspend fun ensurePlans(start: LocalDate, end: LocalDate) {
        var date = start
        while (!date.isAfter(end)) {
            val existing = dayPlanDao.range(date, date).firstOrNull()
            if (existing == null) {
                val priorWeek = dayPlanDao.range(date.minusDays(7), date.minusDays(7)).firstOrNull()
                val (kind, time) =
                    if (priorWeek != null) {
                        priorWeek.sessionKind to priorWeek.startTime
                    } else {
                        defaultSession(date)
                    }
                dayPlanDao.upsert(DayPlanEntity(date, kind, time))
            }
            date = date.plusDays(1)
        }
    }

    private suspend fun closeDietBreakIfNeeded(today: LocalDate, profile: com.fatflow.app.data.local.Profile) {
        if (profile.dietBreakStart.isBlank()) return
        val start = LocalDate.parse(profile.dietBreakStart)
        if (today == start.plusDays(7)) {
            val latest = bodyMetricDao.latest() ?: return
            profileStore.clearDietBreakStart()
            profileStore.updateDietBreakAnchors(
                latest.emaWeightLbs,
                latest.bodyFatPct,
            )
        }
    }
}