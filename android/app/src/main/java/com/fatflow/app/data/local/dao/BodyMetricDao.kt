package com.fatflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fatflow.app.data.local.entity.BodyMetricEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY date DESC LIMIT 1")
    suspend fun latest(): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics WHERE date = :date LIMIT 1")
    suspend fun onDate(date: LocalDate): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<BodyMetricEntity?>

    @Query("SELECT * FROM body_metrics WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun range(start: LocalDate, end: LocalDate): List<BodyMetricEntity>

    @Upsert
    suspend fun upsert(entity: BodyMetricEntity)
}