package com.jsundmer.ningwen

import android.app.Application
import com.jsundmer.ningwen.notify.Notifications
import com.jsundmer.ningwen.notify.ReminderScheduler

/**
 * 应用入口：在进程启动时创建通知渠道并排定每日提醒检查。
 * 放在 Application 而非 Activity，是为了在冷启动/后台重启时都保证提醒被重新排定。
 */
class NingwenApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Notifications.ensureChannel(this)
    ReminderScheduler.schedule(this)
  }
}
