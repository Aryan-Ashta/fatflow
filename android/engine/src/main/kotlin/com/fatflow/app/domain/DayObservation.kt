package com.fatflow.app.domain

import java.time.LocalDate

data class DayObservation(
    val date: LocalDate,
    val weightLbs: Double?,
    val intakeKcal: Int?,
)