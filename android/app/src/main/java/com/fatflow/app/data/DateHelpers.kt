package com.fatflow.app.data

import com.fatflow.app.domain.SessionKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

fun mondayOf(date: LocalDate): LocalDate {
    return date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
}

fun defaultSession(date: LocalDate): Pair<SessionKind, LocalTime?> {
    return when (date.dayOfWeek) {
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY,
        -> SessionKind.LIFTING to LocalTime.of(17, 0)
        DayOfWeek.TUESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.SATURDAY,
        -> SessionKind.TENNIS to LocalTime.of(18, 0)
        else -> SessionKind.REST to null
    }
}