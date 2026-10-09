package com.jsundmer.ningwen.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
  private const val WORK_NAME = "daily_reminder"

  /** 每日约 9:00 检查一次；UPDATE 策略保证配置/记录变化后重排为最新。 */
  fun schedule(context: Context) {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 9)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    val initialDelay = target.timeInMillis - now.timeInMillis

    val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
      .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
      .build()

    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
  }
}
