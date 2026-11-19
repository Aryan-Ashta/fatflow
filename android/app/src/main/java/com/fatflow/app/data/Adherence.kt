package com.fatflow.app.data

import com.fatflow.app.data.local.dao.DailyTargetDao
import com.fatflow.app.data.local.dao.FoodLogDao
import java.time.LocalDate

suspend fun computeAdherence(
    today: LocalDate,
    foodLogDao: FoodLogDao,
    dailyTargetDao: DailyTargetDao,
): Double {
    val start = today.minusDays(13)
    val targets = dailyTargetDao.range(start, today)
    var daysWithBoth = 0
    var intakeSum = 0
    var targetSum = 0
    for (offset in 0..13) {
        val date = start.plusDays(offset.toLong())
        val target = targets.firstOrNull { it.date == date }?.targetCalories ?: continue
        val intake = foodLogDao.sumCalories(date) ?: continue
        daysWithBoth++
        intakeSum += intake
        targetSum += target
    }
    if (daysWithBoth < 7) return 1.0
    val ratio = intakeSum.toDouble() / targetSum.toDouble()
    return ratio.coerceIn(0.5, 1.5)
}