package com.fatflow.app.ui.labels

import com.fatflow.app.domain.DayType
import com.fatflow.app.domain.MetabolicEngine
import com.fatflow.app.domain.SessionKind

fun dayTypeLabel(dayType: DayType, session: SessionKind): String {
    val suggested = MetabolicEngine.sessionToDayType(session)
    if (dayType == suggested) {
        return when (session) {
            SessionKind.TENNIS -> "High Carb – Tennis Day"
            SessionKind.DOUBLE -> "High Carb – Double Session"
            SessionKind.LIFTING -> "Mod Carb – Lifting Day"
            SessionKind.REST -> "Low Carb – Rest"
        }
    }
    val typeName =
        when (dayType) {
            DayType.HIGH_CARB -> "High Carb"
            DayType.MOD_CARB -> "Mod Carb"
            DayType.LOW_CARB -> "Low Carb"
        }
    val sessionName =
        when (session) {
            SessionKind.TENNIS -> "Tennis"
            SessionKind.LIFTING -> "Lifting"
            SessionKind.DOUBLE -> "Double Session"
            SessionKind.REST -> "Rest"
        }
    return "$typeName – $sessionName"
}

fun gaugeCaption(status: com.fatflow.app.domain.GaugeStatus): String =
    when (status) {
        com.fatflow.app.domain.GaugeStatus.DIET_BREAK -> "Diet break"
        com.fatflow.app.domain.GaugeStatus.NO_INTAKE -> "No intake logged"
        com.fatflow.app.domain.GaugeStatus.OVER_TARGET -> "Over target"
        com.fatflow.app.domain.GaugeStatus.MUSCLE_SAFE -> "Muscle Safe"
        com.fatflow.app.domain.GaugeStatus.ABOVE_CEILING -> "Above fat-release ceiling"
    }

fun sessionDisplay(kind: SessionKind): String =
    when (kind) {
        SessionKind.TENNIS -> "Tennis"
        SessionKind.LIFTING -> "Lifting"
        SessionKind.DOUBLE -> "Double Session"
        SessionKind.REST -> "Rest"
    }

fun dayTypeName(type: DayType): String =
    when (type) {
        DayType.HIGH_CARB -> "High Carb"
        DayType.MOD_CARB -> "Mod Carb"
        DayType.LOW_CARB -> "Low Carb"
    }

fun preWorkoutLabel(
    kind: SessionKind,
    grams: Int,
    startTime: java.time.LocalTime?,
): String {
    if (kind == SessionKind.REST || grams == 0) return ""
    val session = sessionDisplay(kind)
    return if (startTime != null) {
        "Eat ${grams}g carbs 60–90 min before $session at $startTime"
    } else {
        "Eat ${grams}g carbs 60–90 min before $session. Set a start time."
    }
}