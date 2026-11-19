package com.fatflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fatflow.app.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Upsert
    suspend fun upsert(entity: CheckInEntity)

    @Query("SELECT * FROM check_ins ORDER BY date DESC LIMIT 1")
    fun latest(): Flow<CheckInEntity?>
}