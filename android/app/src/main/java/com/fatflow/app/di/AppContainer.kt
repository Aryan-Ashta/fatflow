package com.fatflow.app.di

import android.content.Context
import androidx.room.Room
import com.fatflow.app.data.TargetRecomputer
import com.fatflow.app.data.health.HealthConnectGateway
import com.fatflow.app.data.local.FatFlowDatabase
import com.fatflow.app.data.local.ProfileStore
import com.fatflow.app.data.repo.BodyMetricRepository
import com.fatflow.app.data.repo.BodyMetricRepositoryImpl
import com.fatflow.app.data.repo.CheckInRepository
import com.fatflow.app.data.repo.CheckInRepositoryImpl
import com.fatflow.app.data.repo.FoodLogRepository
import com.fatflow.app.data.repo.FoodLogRepositoryImpl
import com.fatflow.app.data.repo.FoodSearchRepository
import com.fatflow.app.data.repo.FoodSearchRepositoryImpl
import com.fatflow.app.data.repo.TargetRepository
import com.fatflow.app.data.repo.TargetRepositoryImpl
import com.fatflow.app.data.repo.WeekPlanRepository
import com.fatflow.app.data.repo.WeekPlanRepositoryImpl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val database: FatFlowDatabase =
        Room.databaseBuilder(context, FatFlowDatabase::class.java, "fatflow.db")
            .fallbackToDestructiveMigration()
            .build()

    val profileStore = ProfileStore(context)

    private val okhttp =
        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .build()

    val recomputer =
        TargetRecomputer(
            profileStore,
            database.dailyTargetDao(),
            database.dayPlanDao(),
            database.foodLogDao(),
            database.bodyMetricDao(),
        )

    val targets: TargetRepository =
        TargetRepositoryImpl(database.dailyTargetDao(), recomputer)

    val bodyMetrics: BodyMetricRepository =
        BodyMetricRepositoryImpl(database.bodyMetricDao(), recomputer)

    val checkIns: CheckInRepository =
        CheckInRepositoryImpl(database.checkInDao(), bodyMetrics)

    val weekPlan: WeekPlanRepository =
        WeekPlanRepositoryImpl(
            context.applicationContext,
            database.dayPlanDao(),
            recomputer,
            database,
            profileStore,
        )

    val health = HealthConnectGateway(context, weekPlan, bodyMetrics)

    val foodLogs: FoodLogRepository =
        FoodLogRepositoryImpl(database.foodLogDao(), context.applicationContext, health)

    val foodSearch: FoodSearchRepository =
        FoodSearchRepositoryImpl(database.foodCacheDao(), profileStore, okhttp)
}