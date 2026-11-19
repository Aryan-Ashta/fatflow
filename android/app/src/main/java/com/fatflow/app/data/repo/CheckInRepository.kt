package com.fatflow.app.data.repo

import com.fatflow.app.data.local.entity.CheckInEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface CheckInRepository {
    suspend fun save(
        date: LocalDate,
        weightLbs: Double,
        bodyFatPct: Double?,
        waistInches: Double?,
        energyScore: Int,
    )

    fun observeLatest(): Flow<CheckInEntity?>
}