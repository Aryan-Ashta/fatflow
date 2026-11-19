package com.fatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fatflow.app.domain.DayType
import java.time.LocalDate

@Entity(tableName = "daily_targets")
data class DailyTargetEntity(
    @PrimaryKey val date: LocalDate,
    val dayType: DayType,
    val dayTypeOverridden: Boolean = false,
    val estimatedTdee: Int,
    val maxFatReleaseKcal: Int,
    val targetCalories: Int,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int,
    val isDietBreak: Boolean = false,
)