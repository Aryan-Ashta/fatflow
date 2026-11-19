package com.fatflow.app.data.repo

import com.fatflow.app.data.local.dao.CheckInDao
import com.fatflow.app.data.local.entity.CheckInEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class CheckInRepositoryImpl(
    private val dao: CheckInDao,
    private val bodyMetrics: BodyMetricRepository,
) : CheckInRepository {
    override suspend fun save(
        date: LocalDate,
        weightLbs: Double,
        bodyFatPct: Double?,
        waistInches: Double?,
        energyScore: Int,
    ) {
        require(energyScore in 1..5)
        dao.upsert(
            CheckInEntity(
                date = date,
                morningWeightLbs = weightLbs.toFloat(),
                bodyFatPct = bodyFatPct?.toFloat(),
                waistInches = waistInches?.toFloat(),
                energyScore = energyScore,
            ),
        )
        bodyMetrics.record(date, weightLbs, bodyFatPct)
    }

    override fun observeLatest(): Flow<CheckInEntity?> = dao.latest()
}