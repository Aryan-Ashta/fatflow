package com.fatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fatflow.app.domain.SessionKind
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "day_plans")
data class DayPlanEntity(
    @PrimaryKey val date: LocalDate,
    val sessionKind: SessionKind,
    val startTime: LocalTime? = null,
)