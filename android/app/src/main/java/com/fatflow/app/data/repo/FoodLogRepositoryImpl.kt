package com.fatflow.app.data.repo

import android.content.Context
import com.fatflow.app.data.health.HealthConnectGateway
import com.fatflow.app.data.local.dao.FoodLogDao
import com.fatflow.app.data.local.entity.FoodLogEntity
import com.fatflow.app.widgets.FatFlowSmallWidgetContent
import com.fatflow.app.widgets.FatFlowWideWidgetContent
import androidx.glance.appwidget.updateAll
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class FoodLogRepositoryImpl(
    private val dao: FoodLogDao,
    private val context: Context,
    private val health: HealthConnectGateway,
) : FoodLogRepository {
    override fun observeDay(date: LocalDate): Flow<List<FoodLogEntity>> = dao.observeDay(date)

    override suspend fun log(entry: FoodLogEntity): Long {
        val id = dao.insert(entry)
        health.writeMeal(entry)
        FatFlowSmallWidgetContent().updateAll(context)
        FatFlowWideWidgetContent().updateAll(context)
        return id
    }

    override suspend fun repeatYesterdayPreWorkout(today: LocalDate): Int {
        val yesterday = today.minusDays(1)
        val rows = dao.preWorkoutOn(yesterday)
        rows.forEach { row ->
            dao.insert(
                row.copy(
                    id = 0,
                    date = today,
                    source = "REPEAT",
                ),
            )
        }
        return rows.size
    }
}