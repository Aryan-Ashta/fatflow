package com.fatflow.app.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import com.fatflow.app.data.local.entity.FoodLogEntity
import com.fatflow.app.data.repo.BodyMetricRepository
import com.fatflow.app.data.repo.WeekPlanRepository
import com.fatflow.app.domain.SessionKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class HealthSdkStatus {
    SDK_AVAILABLE,
    SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
    SDK_UNAVAILABLE,
}

class HealthConnectGateway(
    private val context: Context,
    private val weekPlan: WeekPlanRepository,
    private val bodyMetrics: BodyMetricRepository,
) {
    private val client: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    fun availability(): HealthSdkStatus {
        val status = HealthConnectClient.getSdkStatus(context)
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> HealthSdkStatus.SDK_AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthSdkStatus.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
            else -> HealthSdkStatus.SDK_UNAVAILABLE
        }
    }

    suspend fun importToday(today: LocalDate) {
        val hc = client ?: return
        if (availability() != HealthSdkStatus.SDK_AVAILABLE) return
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).toInstant()
        val end = Instant.now()
        runCatching {
            val weights =
                hc.readRecords(
                    ReadRecordsRequest(WeightRecord::class, timeRangeFilter = timeRange(start, end)),
                )
            val bodyFat =
                hc.readRecords(
                    ReadRecordsRequest(BodyFatRecord::class, timeRangeFilter = timeRange(start, end)),
                )
            val weight = weights.records.maxByOrNull { it.time }
            if (weight != null) {
                val bf =
                    bodyFat.records.maxByOrNull { it.time }?.percentage?.value
                        ?.let { if (it <= 1.0) it * 100 else it }
                val lbs = weight.weight.inKilograms * 2.2046226218
                bodyMetrics.record(today, lbs, bf)
            }
            val sessions =
                hc.readRecords(
                    ReadRecordsRequest(
                        ExerciseSessionRecord::class,
                        timeRangeFilter = timeRange(start, end),
                    ),
                )
            val hasLift =
                sessions.records.any {
                    it.exerciseType in
                        setOf(
                            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
                            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
                        )
                }
            val hasTennis =
                sessions.records.any {
                    it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_TENNIS
                }
            when {
                hasTennis && hasLift -> weekPlan.setSession(today, SessionKind.DOUBLE)
                hasTennis -> weekPlan.setSession(today, SessionKind.TENNIS)
                hasLift -> weekPlan.setSession(today, SessionKind.LIFTING)
            }
        }.onFailure { Log.w("FatFlow", "Health import failed", it) }
    }

    suspend fun writeMeal(log: FoodLogEntity) {
        val hc = client ?: return
        val zone = ZoneId.systemDefault()
        runCatching {
            hc.insertRecords(
                listOf(
                    NutritionRecord(
                        name = log.mealName,
                        energy = androidx.health.connect.client.units.Energy.kilocalories(log.calories.toDouble()),
                        protein = androidx.health.connect.client.units.Mass.grams(log.proteinGrams.toDouble()),
                        totalCarbohydrate = androidx.health.connect.client.units.Mass.grams(log.carbGrams.toDouble()),
                        totalFat = androidx.health.connect.client.units.Mass.grams(log.fatGrams.toDouble()),
                        startTime = Instant.now(),
                        endTime = Instant.now(),
                        startZoneOffset = zone.rules.getOffset(Instant.now()),
                        endZoneOffset = zone.rules.getOffset(Instant.now()),
                        metadata =
                            androidx.health.connect.client.records.metadata.Metadata.manualEntry(),
                    ),
                ),
            )
        }.onFailure { Log.w("FatFlow", "Nutrition write failed", it) }
    }

    private fun timeRange(start: Instant, end: Instant) =
        androidx.health.connect.client.time.TimeRangeFilter.between(start, end)

    companion object {
        fun readPermissions() =
            setOf(
                HealthPermission.getReadPermission(WeightRecord::class),
                HealthPermission.getReadPermission(BodyFatRecord::class),
                HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                HealthPermission.getWritePermission(NutritionRecord::class),
            )
    }
}