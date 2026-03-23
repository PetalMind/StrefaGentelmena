package com.strefagentelmena.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.strefagentelmena.functions.fireBase.FirebaseRealtimeDatabaseBackup

/**
 * Wykonuje eksport RTDB do Storage — uruchamiany przez [ScheduledBackupScheduler].
 */
class ScheduledDatabaseBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = FirebaseRealtimeDatabaseBackup.exportFullDatabaseJsonToStorage()
        return if (result.success) Result.success() else Result.retry()
    }
}
