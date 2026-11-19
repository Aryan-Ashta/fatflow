package com.fatflow.app.ui.food

import com.fatflow.app.di.AppContainer
import com.fatflow.app.domain.SessionKind
import java.time.LocalDate
import java.time.LocalDateTime

suspend fun defaultIsPreWorkout(container: AppContainer): Boolean {
    val today = LocalDate.now()
    val plan = container.database.dayPlanDao().range(today, today).firstOrNull() ?: return false
    if (plan.sessionKind == SessionKind.REST) return false
    val start = plan.startTime ?: return true
    return LocalDateTime.now().toLocalTime().isBefore(start)
}