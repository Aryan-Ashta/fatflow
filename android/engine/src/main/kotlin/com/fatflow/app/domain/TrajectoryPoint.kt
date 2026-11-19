package com.fatflow.app.domain

data class TrajectoryPoint(
    val week: Int,
    val weightLbs: Double,
    val bodyFatPct: Double,
    val calorieTarget: Int,
    val safeFloor: Int,
)