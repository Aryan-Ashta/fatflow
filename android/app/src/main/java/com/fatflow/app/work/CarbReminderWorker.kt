package com.fatflow.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fatflow.app.R

class CarbReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val grams = inputData.getInt(KEY_GRAMS, 0)
        val session = inputData.getString(KEY_SESSION).orEmpty()
        val time = inputData.getString(KEY_TIME).orEmpty()
        if (grams == 0) return Result.success()
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL, "Pre-workout fuel", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Pre-workout fuel")
                .setContentText("Eat ${grams}g carbs before $session at $time.")
                .build()
        manager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL = "fuel"
        const val KEY_GRAMS = "grams"
        const val KEY_SESSION = "session"
        const val KEY_TIME = "time"
        private const val NOTIFICATION_ID = 1001
    }
}