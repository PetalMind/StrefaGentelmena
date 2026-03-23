package com.strefagentelmena.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Dwie niezależne prace okresowe (co 24 h), wyrównane do 6:00 i 22:00 w strefie czasowej urządzenia.
 */
object ScheduledBackupScheduler {

    private const val UNIQUE_MORNING = "rtdb_scheduled_backup_06_00"
    private const val UNIQUE_EVENING = "rtdb_scheduled_backup_22_00"

    fun scheduleTwiceDaily(context: Context) {
        val wm = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val morning = PeriodicWorkRequestBuilder<ScheduledDatabaseBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(msUntilLocalTime(6, 0), TimeUnit.MILLISECONDS)
            .build()

        val evening = PeriodicWorkRequestBuilder<ScheduledDatabaseBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(msUntilLocalTime(22, 0), TimeUnit.MILLISECONDS)
            .build()

        wm.enqueueUniquePeriodicWork(UNIQUE_MORNING, ExistingPeriodicWorkPolicy.KEEP, morning)
        wm.enqueueUniquePeriodicWork(UNIQUE_EVENING, ExistingPeriodicWorkPolicy.KEEP, evening)
    }

    private fun msUntilLocalTime(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return ChronoUnit.MILLIS.between(now, next).coerceAtLeast(1L)
    }
}
