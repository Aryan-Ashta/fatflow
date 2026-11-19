package com.fatflow.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fatflow.app.di.AppContainer
import com.fatflow.app.work.MorningTargetWorker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FatFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        runBlocking {
            if (container.profileStore.profile.first().onboardingComplete) {
                enqueueMorningWork()
            }
        }
    }

    fun enqueueMorningWork() {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), LocalTime.of(4, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)
        val request =
            PeriodicWorkRequestBuilder<MorningTargetWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "morning_target",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}