package com.fatflow.app.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object TrajectoryProjector {
    fun project(
        leanMassLbs: Double,
        fatMassLbs: Double,
        tdee: Int,
        releaseRate: Int,
        adherence: Double,
        goalWeightLbs: Double,
        maxWeeks: Int = 104,
    ): List<TrajectoryPoint> {
        var fat = fatMassLbs
        val lean = leanMassLbs
        var goalFat = goalWeightLbs - lean
        if (goalFat <= 0) {
            goalFat = lean * 0.131 / 0.869
        }
        val points = mutableListOf<TrajectoryPoint>()
        val initialWeight = lean + fat
        val initialBf = fat / initialWeight * 100.0
        val initialRelease = MetabolicEngine.maxFatReleaseKcal(fat, releaseRate)
        val initialFloor = MetabolicEngine.safeFloorKcal(lean)
        val initialTarget = MetabolicEngine.calorieTarget(tdee, initialRelease, initialFloor)
        points.add(
            TrajectoryPoint(
                week = 0,
                weightLbs = initialWeight,
                bodyFatPct = initialBf,
                calorieTarget = initialTarget,
                safeFloor = initialFloor,
            ),
        )
        var week = 1
        while (week <= maxWeeks) {
            val release = fat * releaseRate
            val floor = max(1500.0, 11.0 * lean)
            val target = max(floor, tdee - release).roundToInt()
            val intake = target * adherence
            val deficit = tdee - intake
            val lossLbs = min(deficit, release).coerceAtLeast(0.0) * 7.0 / MetabolicEngine.KCAL_PER_LB
            if (lossLbs <= 0.05 || fat <= goalFat) break
            fat = max(goalFat, fat - lossLbs)
            val weight = lean + fat
            val bf = fat / weight * 100.0
            val maxRelease = MetabolicEngine.maxFatReleaseKcal(fat, releaseRate)
            val safeFloor = MetabolicEngine.safeFloorKcal(lean)
            val calorieTarget = MetabolicEngine.calorieTarget(tdee, maxRelease, safeFloor)
            points.add(
                TrajectoryPoint(
                    week = week,
                    weightLbs = weight,
                    bodyFatPct = bf,
                    calorieTarget = calorieTarget,
                    safeFloor = safeFloor,
                ),
            )
            week++
        }
        return points
    }
}