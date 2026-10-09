package com.jsundmer.ningwen.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jsundmer.ningwen.data.Repository
import com.jsundmer.ningwen.domain.InrRules
import com.jsundmer.ningwen.domain.ReminderBucket
import kotlinx.coroutines.flow.first

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
  override suspend fun doWork(): Result {
    val repo = Repository(applicationContext)
    val settings = repo.settings.first()
    if (!settings.reminderEnabled) return Result.success()

    val last = repo.latestRecord() ?: return Result.success()
    val due = InrRules.nextTestMillis(last.measuredAt, settings.testIntervalDays)
    val days = InrRules.daysUntilDue(due, System.currentTimeMillis())

    when (InrRules.dueBucket(days)) {
      ReminderBucket.TOMORROW -> Notifications.show(
        applicationContext, "明天该测 INR 了", "距下次测量还有 1 天，记得安排时间。",
      )
      ReminderBucket.TODAY -> Notifications.show(
        applicationContext, "今天该测 INR 了", "到测量日了，测完记得记录一下。",
      )
      ReminderBucket.NONE -> Unit
    }
    return Result.success()
  }
}
