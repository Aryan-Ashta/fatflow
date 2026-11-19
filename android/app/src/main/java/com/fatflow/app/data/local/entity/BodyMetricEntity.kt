package com.fatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey val date: LocalDate,
    val rawWeightLbs: Float,
    val emaWeightLbs: Float,
    val bodyFatPct: Float,
    val leanMassLbs: Float,
    val fatMassLbs: Float,
)