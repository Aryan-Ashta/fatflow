package com.fatflow.app.work

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fatflow.app.data.local.ProfileStore
import com.fatflow.app.data.local.entity.DayPlanEntity
import com.fatflow.app.domain.MetabolicEngine
import com.fatflow.app.domain.SessionKind
import com.fatflow.app.ui.labels.sessionDisplay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

object ReminderScheduler {
    suspend fun scheduleForPlan(
        context: Context,
        profileStore: ProfileStore,
        plan: DayPlanEntity,
        carbTarget: Int,
        carbLogged: Int,
        leanLbs: Double,
    ) {
        val name = "carb_reminder_${plan.date}"
        WorkManager.getInstance(context).cancelUniqueWork(name)
        val profile = profileStore.profile.first()
        if (!profile.notificationsEnabled) return
        if (plan.sessionKind == SessionKind.REST || plan.startTime == null) return
        val start = LocalDateTime.of(plan.date, plan.startTime)
        val trigger = start.minusMinutes(75)
        val delay = Duration.between(LocalDateTime.now(), trigger)
        if (delay.isNegative) return
        val grams =
            MetabolicEngine.preWorkoutCarbGrams(
                leanLbs,
                plan.sessionKind,
                carbTarget,
                carbLogged,
            )
        val data =
            Data.Builder()
                .putInt(CarbReminderWorker.KEY_GRAMS, grams)
                .putString(CarbReminderWorker.KEY_SESSION, sessionDisplay(plan.sessionKind))
                .putString(CarbReminderWorker.KEY_TIME, plan.startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                .build()
        val request =
            OneTimeWorkRequestBuilder<CarbReminderWorker>()
                .setInitialDelay(delay)
                .setInputData(data)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, androidx.work.ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, date: LocalDate) {
        WorkManager.getInstance(context).cancelUniqueWork("carb_reminder_$date")
    }
}