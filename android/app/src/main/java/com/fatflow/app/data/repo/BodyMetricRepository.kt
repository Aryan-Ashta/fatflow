package com.fatflow.app.data.repo

import com.fatflow.app.data.local.entity.BodyMetricEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface BodyMetricRepository {
    fun observeLatest(): Flow<BodyMetricEntity?>

    suspend fun record(date: LocalDate, weightLbs: Double, bodyFatPct: Double?): BodyMetricEntity
}