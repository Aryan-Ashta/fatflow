package com.fatflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fatflow.app.data.local.entity.DailyTargetEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTargetDao {
    @Query("SELECT * FROM daily_targets WHERE date = :date")
    fun observe(date: LocalDate): Flow<DailyTargetEntity?>

    @Query("SELECT * FROM daily_targets WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DailyTargetEntity>>

    @Query("SELECT * FROM daily_targets WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun range(start: LocalDate, end: LocalDate): List<DailyTargetEntity>

    @Upsert
    suspend fun upsert(entity: DailyTargetEntity)

    @Query("SELECT * FROM daily_targets WHERE date = :date")
    suspend fun get(date: LocalDate): DailyTargetEntity?

    @Query(
        "SELECT estimatedTdee FROM daily_targets WHERE date < :date ORDER BY date DESC LIMIT 1",
    )
    suspend fun latestTdeeBefore(date: LocalDate): Int?
}