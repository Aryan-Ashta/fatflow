package com.fatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey val date: LocalDate,
    val morningWeightLbs: Float,
    val bodyFatPct: Float? = null,
    val waistInches: Float? = null,
    val energyScore: Int,
)