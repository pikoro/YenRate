package net.owh.yenrate.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.owh.yenrate.data.YenRatePreferences
import net.owh.yenrate.data.YenRateRepository
import net.owh.yenrate.widget.YenRateWidgetProvider
import java.util.concurrent.TimeUnit

class YenRateUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return YenRateRepository.fetchJpyRate().fold(
            onSuccess = { rate ->
                YenRatePreferences.saveRate(context, rate)
                YenRateWidgetProvider.updateAllWidgets(context)
                net.owh.yenrate.widget.YenRateCompactWidgetProvider.updateAllWidgets(context)
                Result.success()
            },
            onFailure = {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        )
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "yen_rate_hourly_update"

        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<YenRateUpdateWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun enqueueImmediateWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<YenRateUpdateWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
