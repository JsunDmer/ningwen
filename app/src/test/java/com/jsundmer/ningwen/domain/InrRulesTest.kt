package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InrRulesTest {

  private val DAY = 86_400_000L

  @Test fun judge_low_when_below_min() {
    assertEquals(InrStatus.LOW, InrRules.judge(1.8, 2.0, 3.0))
  }

  @Test fun judge_ok_when_in_range_inclusive() {
    assertEquals(InrStatus.OK, InrRules.judge(2.0, 2.0, 3.0))
    assertEquals(InrStatus.OK, InrRules.judge(3.0, 2.0, 3.0))
  }

  @Test fun judge_high_when_above_max() {
    assertEquals(InrStatus.HIGH, InrRules.judge(3.4, 2.0, 3.0))
  }

  @Test fun nextTestMillis_adds_interval_days() {
    val last = 1_000_000_000_000L
    assertEquals(last + 7 * DAY, InrRules.nextTestMillis(last, 7))
  }

  @Test fun daysUntilDue_rounds_up() {
    val now = 1_000_000_000_000L
    val due = now + 3 * DAY + 1 // 3 天零 1ms → 向上取整为 4
    assertEquals(4L, InrRules.daysUntilDue(due, now))
  }

  @Test fun daysUntilDue_null_when_no_due() {
    assertNull(InrRules.daysUntilDue(null, 0L))
  }

  @Test fun dueBucket_classifies_reminder_days() {
    assertEquals(ReminderBucket.TOMORROW, InrRules.dueBucket(1))
    assertEquals(ReminderBucket.TODAY, InrRules.dueBucket(0))
    assertEquals(ReminderBucket.NONE, InrRules.dueBucket(5))
    assertEquals(ReminderBucket.NONE, InrRules.dueBucket(-2))
  }
}
