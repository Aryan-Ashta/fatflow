package com.fatflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fatflow.app.data.local.dao.BodyMetricDao
import com.fatflow.app.data.local.dao.CheckInDao
import com.fatflow.app.data.local.dao.DailyTargetDao
import com.fatflow.app.data.local.dao.DayPlanDao
import com.fatflow.app.data.local.dao.FoodCacheDao
import com.fatflow.app.data.local.dao.FoodLogDao
import com.fatflow.app.data.local.entity.BodyMetricEntity
import com.fatflow.app.data.local.entity.CheckInEntity
import com.fatflow.app.data.local.entity.DailyTargetEntity
import com.fatflow.app.data.local.entity.DayPlanEntity
import com.fatflow.app.data.local.entity.FoodCacheEntity
import com.fatflow.app.data.local.entity.FoodLogEntity

@Database(
    entities = [
        DailyTargetEntity::class,
        BodyMetricEntity::class,
        FoodLogEntity::class,
        DayPlanEntity::class,
        CheckInEntity::class,
        FoodCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FatFlowDatabase : RoomDatabase() {
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun dayPlanDao(): DayPlanDao
    abstract fun checkInDao(): CheckInDao
    abstract fun foodCacheDao(): FoodCacheDao
}