package com.fatflow.app.data.repo

import com.fatflow.app.data.TargetRecomputer
import com.fatflow.app.data.local.dao.BodyMetricDao
import com.fatflow.app.data.local.entity.BodyMetricEntity
import com.fatflow.app.domain.MetabolicEngine
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class BodyMetricRepositoryImpl(
    private val dao: BodyMetricDao,
    private val recomputer: TargetRecomputer,
) : BodyMetricRepository {
    override fun observeLatest(): Flow<BodyMetricEntity?> = dao.observeLatest()

    override suspend fun record(
        date: LocalDate,
        weightLbs: Double,
        bodyFatPct: Double?,
    ): BodyMetricEntity {
        val bf =
            bodyFatPct
                ?: dao.latest()?.bodyFatPct?.toDouble()
                ?: throw IllegalStateException("Body fat required for first metric")
        val prior = dao.latest()
        val ema =
            MetabolicEngine.nextEma(
                prior?.emaWeightLbs?.toDouble(),
                weightLbs,
            )
        val fat = MetabolicEngine.fatMassLbs(weightLbs, bf)
        val lean = MetabolicEngine.leanMassLbs(weightLbs, bf)
        val entity =
            BodyMetricEntity(
                date = date,
                rawWeightLbs = weightLbs.toFloat(),
                emaWeightLbs = ema.toFloat(),
                bodyFatPct = bf.toFloat(),
                leanMassLbs = lean.toFloat(),
                fatMassLbs = fat.toFloat(),
            )
        dao.upsert(entity)
        recomputer.recompute(date)
        return entity
    }
}