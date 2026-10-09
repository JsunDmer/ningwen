package com.jsundmer.ningwen.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jsundmer.ningwen.R

object Notifications {
  const val CHANNEL_ID = "reminder"
  private const val NOTIFICATION_ID = 1001

  /** 渠道需在发通知前建好；重复调用安全。 */
  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.notif_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply { description = context.getString(R.string.notif_channel_desc) }
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  /** 无通知权限时静默跳过（用户可只靠 App 内倒计时）。 */
  fun show(context: Context, title: String, text: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) return

    val n = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notify)
      .setContentTitle(title)
      .setContentText(text)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
  }
}
