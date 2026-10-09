package com.jsundmer.ningwen.domain

/** INR 达标状态。 */
enum class InrStatus { OK, HIGH, LOW }

/** 到期提醒分类。 */
enum class ReminderBucket { TOMORROW, TODAY, NONE }

/**
 * 纯领域规则（无 Android 依赖，可单测）。
 * 所有"达标/到期/提醒"判断的唯一来源，UI 与通知都调用它，避免逻辑分散。
 */
object InrRules {
  const val DAY_MILLIS = 86_400_000L

  fun judge(value: Double, min: Double, max: Double): InrStatus = when {
    value < min -> InrStatus.LOW
    value > max -> InrStatus.HIGH
    else -> InrStatus.OK
  }

  /** 无记录返回 null。 */
  fun nextTestMillis(lastMeasuredAt: Long?, intervalDays: Int): Long? =
    lastMeasuredAt?.let { it + intervalDays * DAY_MILLIS }

  /** 距到期还剩几天；向上取整（今天到期含未来不足一天记 0）。无到期返回 null。 */
  fun daysUntilDue(dueMillis: Long?, nowMillis: Long): Long? =
    dueMillis?.let { Math.ceil((it - nowMillis).toDouble() / DAY_MILLIS).toLong() }

  /** 提醒分类：提前 1 天 / 当天 / 不提醒。 */
  fun dueBucket(daysUntilDue: Long?): ReminderBucket = when (daysUntilDue) {
    1L -> ReminderBucket.TOMORROW
    0L -> ReminderBucket.TODAY
    else -> ReminderBucket.NONE
  }
}
