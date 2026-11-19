package com.fatflow.app.domain

import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MetabolicEngine {
    const val EMA_ALPHA: Double = 2.0 / 15.0
    const val KCAL_PER_LB: Double = 3500.0
    const val ACTIVITY_MULTIPLIER: Double = 1.72
    const val LB_TO_KG: Double = 0.45359237

    private const val HIGH_THOUSANDTHS = 1175L
    private const val MOD_THOUSANDTHS = 1000L
    private const val LOW_THOUSANDTHS = 825L

    fun fatMassLbs(weightLbs: Double, bodyFatPct: Double): Double {
        validateWeight(weightLbs)
        validateBodyFat(bodyFatPct)
        return weightLbs * (bodyFatPct / 100.0)
    }

    fun leanMassLbs(weightLbs: Double, bodyFatPct: Double): Double {
        validateWeight(weightLbs)
        validateBodyFat(bodyFatPct)
        return weightLbs - fatMassLbs(weightLbs, bodyFatPct)
    }

    fun maxFatReleaseKcal(fatMassLbs: Double, releaseRate: Int): Int {
        validateReleaseRate(releaseRate)
        return (fatMassLbs * releaseRate).roundToInt()
    }

    fun safeFloorKcal(leanMassLbs: Double): Int {
        return max(1500, (11.0 * leanMassLbs).roundToInt())
    }

    fun seedTdee(leanMassLbs: Double): Int {
        return ((370.0 + 21.6 * leanMassLbs * LB_TO_KG) * ACTIVITY_MULTIPLIER).roundToInt()
    }

    fun calorieTarget(tdee: Int, maxFatReleaseKcal: Int, safeFloorKcal: Int): Int {
        return max(safeFloorKcal, tdee - maxFatReleaseKcal)
    }

    fun nextEma(previous: Double?, sample: Double): Double {
        return if (previous == null) {
            sample
        } else {
            EMA_ALPHA * sample + (1.0 - EMA_ALPHA) * previous
        }
    }

    fun updateAdaptiveTdee(
        previousTdee: Int,
        observations: List<DayObservation>,
    ): AdaptiveTdeeResult {
        if (observations.isEmpty()) {
            return AdaptiveTdeeResult(previousTdee, null)
        }

        val sorted = observations.sortedBy { it.date }
        val intakes = sorted.mapNotNull { it.intakeKcal }
        val emaEnd = currentEma(sorted)
        if (intakes.size < 7) {
            return AdaptiveTdeeResult(previousTdee, emaEnd)
        }

        val weightPoints = sorted.filter { it.weightLbs != null }
        if (weightPoints.size < 2) {
            return AdaptiveTdeeResult(previousTdee, emaEnd)
        }

        val first = weightPoints.first()
        val last = weightPoints.last()
        val daysBetween =
            ChronoUnit.DAYS.between(first.date, last.date).coerceAtLeast(1)
        if (daysBetween < 7) {
            return AdaptiveTdeeResult(previousTdee, emaEnd)
        }

        if (weightPoints.all { it.weightLbs == weightPoints.first().weightLbs }) {
            return AdaptiveTdeeResult(previousTdee, emaEnd)
        }

        val emaAtEnd = emaAtDate(sorted, last.date) ?: last.weightLbs!!
        val avgIntake = intakes.sum().toDouble() / intakes.size
        val weightDelta = first.weightLbs!! - last.weightLbs!!
        val maintenanceEstimate = avgIntake + (weightDelta * KCAL_PER_LB / daysBetween)
        val intakeGap = (previousTdee - avgIntake).coerceAtLeast(1.0)
        val adaptWeight = (6.0 / 14.0) * daysBetween / (intakeGap / 48.0)
        val adapted = previousTdee + adaptWeight * (maintenanceEstimate - previousTdee)
        return AdaptiveTdeeResult(adapted.roundToInt(), emaAtEnd)
    }

    private fun currentEma(observations: List<DayObservation>): Double? {
        var ema: Double? = null
        for (obs in observations.sortedBy { it.date }) {
            val w = obs.weightLbs ?: continue
            ema = nextEma(ema, w)
        }
        return ema
    }

    private fun emaAtDate(observations: List<DayObservation>, target: java.time.LocalDate): Double? {
        var ema: Double? = null
        for (obs in observations.sortedBy { it.date }) {
            if (obs.date > target) break
            val w = obs.weightLbs ?: continue
            ema = nextEma(ema, w)
        }
        return ema
    }

    fun weeklyCalories(baseline: Int, dayTypes: List<DayType>): List<Int> {
        val n = dayTypes.size
        val weekTarget = baseline * n
        val reference = 1050L
        val quots = mutableListOf<Long>()
        val rems = mutableListOf<Long>()
        for (type in dayTypes) {
            val t = dayTypeThousandths(type)
            val raw = baseline.toLong() * t
            quots.add(raw / reference)
            rems.add(raw % reference)
        }
        val extra = weekTarget - quots.sum().toInt()
        return largestRemainder(quots, rems, extra)
    }

    fun macros(dayCalories: Int, leanMassLbs: Double, dayType: DayType): Macros {
        var protein = (1.05 * leanMassLbs).roundToInt()
        val fatPerLb = when (dayType) {
            DayType.HIGH_CARB -> 0.30
            DayType.MOD_CARB -> 0.38
            DayType.LOW_CARB -> 0.48
        }
        var fat = (leanMassLbs * fatPerLb).roundToInt()
        var carbKcal = dayCalories - protein * 4 - fat * 9
        if (carbKcal < 0) {
            val minFat = (0.25 * leanMassLbs).roundToInt()
            val excess = -carbKcal
            val fatCut = ceil(excess / 9.0).toInt()
            fat = max(minFat, fat - fatCut)
            carbKcal = dayCalories - protein * 4 - fat * 9
        }
        if (carbKcal < 0) {
            val excess = -carbKcal
            val proteinCut = ceil(excess / 4.0).toInt()
            protein = max(0, protein - proteinCut)
            carbKcal = dayCalories - protein * 4 - fat * 9
        }
        val carbGrams = max(0, carbKcal / 4)
        return Macros(protein, carbGrams, fat)
    }

    fun escalationSteps(startFatMassLbs: Double, rollingFatMassLbs: Double): Int {
        return floor((startFatMassLbs - rollingFatMassLbs) / 2.0).toInt().coerceAtLeast(0)
    }

    fun escalationPoolKcal(steps: Int, releaseRate: Int): Int {
        validateReleaseRate(releaseRate)
        return steps * 2 * releaseRate * 7
    }

    fun distributeTrainingBonus(dayTypes: List<DayType>, poolKcal: Int): List<Int> {
        val eligible = dayTypes.mapIndexed { index, type ->
            if (type == DayType.HIGH_CARB || type == DayType.MOD_CARB) index else -1
        }.filter { it >= 0 }
        if (poolKcal == 0 || eligible.isEmpty()) return dayTypes.map { 0 }

        val n = eligible.size
        val quot = poolKcal / n
        val extra = poolKcal % n
        val slotBonus = IntArray(n) { quot }
        for (i in 0 until extra) {
            slotBonus[i] += 1
        }
        return dayTypes.mapIndexed { index, type ->
            if (type == DayType.HIGH_CARB || type == DayType.MOD_CARB) {
                slotBonus[eligible.indexOf(index)]
            } else {
                0
            }
        }
    }

    fun dietBreakDue(
        anchorWeightLbs: Double,
        currentWeightLbs: Double,
        anchorBfPct: Double,
        currentBfPct: Double,
    ): Boolean {
        val weightDrop = anchorWeightLbs - currentWeightLbs
        val bfDrop = anchorBfPct - currentBfPct
        return weightDrop >= 24.0 || bfDrop >= 10.0
    }

    fun sessionToDayType(kind: SessionKind): DayType {
        return when (kind) {
            SessionKind.TENNIS, SessionKind.DOUBLE -> DayType.HIGH_CARB
            SessionKind.LIFTING -> DayType.MOD_CARB
            SessionKind.REST -> DayType.LOW_CARB
        }
    }

    fun preWorkoutCarbGrams(
        leanMassLbs: Double,
        kind: SessionKind,
        carbTarget: Int,
        carbLogged: Int,
    ): Int {
        val desired = when (kind) {
            SessionKind.TENNIS, SessionKind.DOUBLE -> (0.50 * leanMassLbs).roundToInt()
            SessionKind.LIFTING -> (0.30 * leanMassLbs).roundToInt()
            SessionKind.REST -> 0
        }
        return min(desired, max(0, carbTarget - carbLogged))
    }

    fun gaugeStatus(
        isDietBreak: Boolean,
        intakeKcal: Int,
        deficitKcal: Int,
        maxReleaseKcal: Int,
    ): GaugeStatus {
        if (isDietBreak) return GaugeStatus.DIET_BREAK
        if (intakeKcal == 0) return GaugeStatus.NO_INTAKE
        if (deficitKcal < 0) return GaugeStatus.OVER_TARGET
        if (deficitKcal <= maxReleaseKcal) return GaugeStatus.MUSCLE_SAFE
        return GaugeStatus.ABOVE_CEILING
    }

    fun largestRemainder(quotients: List<Long>, remainders: List<Long>, extra: Int): List<Int> {
        require(quotients.size == remainders.size)
        val result = quotients.map { it.toInt() }.toMutableList()
        if (extra <= 0) return result
        val order = remainders.indices.sortedWith(
            compareByDescending<Int> { remainders[it] }.thenBy { it },
        )
        var left = extra
        var idx = 0
        while (left > 0 && order.isNotEmpty()) {
            result[order[idx % order.size]] += 1
            left -= 1
            idx += 1
        }
        return result
    }

    private fun dayTypeThousandths(type: DayType): Long = when (type) {
        DayType.HIGH_CARB -> HIGH_THOUSANDTHS
        DayType.MOD_CARB -> MOD_THOUSANDTHS
        DayType.LOW_CARB -> LOW_THOUSANDTHS
    }

    private fun validateWeight(weightLbs: Double) {
        if (weightLbs < 80.0 || weightLbs > 500.0) {
            throw IllegalArgumentException("weight out of range")
        }
    }

    private fun validateBodyFat(bodyFatPct: Double) {
        if (bodyFatPct < 3.0 || bodyFatPct > 60.0) {
            throw IllegalArgumentException("body fat out of range")
        }
    }

    private fun validateReleaseRate(releaseRate: Int) {
        if (releaseRate < 22 || releaseRate > 31) {
            throw IllegalArgumentException("release rate out of range")
        }
    }
}