package com.fatflow.app.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fatflow.app.FatFlowApp
import com.fatflow.app.widgets.FatFlowSmallWidgetContent
import com.fatflow.app.widgets.FatFlowWideWidgetContent
import java.time.LocalDate

class MorningTargetWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as FatFlowApp
        val container = app.container
        val today = LocalDate.now()
        runCatching { container.health.importToday(today) }
        container.recomputer.recompute(today)
        FatFlowSmallWidgetContent().updateAll(applicationContext)
        FatFlowWideWidgetContent().updateAll(applicationContext)
        return Result.success()
    }
}