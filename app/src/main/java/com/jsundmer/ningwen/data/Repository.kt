package com.jsundmer.ningwen.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** UI 唯一数据入口：聚合记录与设置，隐藏 Room/DataStore 细节。 */
class Repository(context: Context) {
  private val dao = NingwenDatabase.get(context).recordDao()
  private val settingsStore = SettingsStore(context)

  val records: Flow<List<Record>> = dao.observeAll()
  val settings: Flow<Settings> = settingsStore.flow

  suspend fun addRecord(record: Record) = dao.insert(record)
  suspend fun deleteRecord(record: Record) = dao.delete(record)
  suspend fun latestRecord(): Record? = dao.latest()

  suspend fun setTargetRange(min: Double, max: Double) = settingsStore.setTargetRange(min, max)
  suspend fun setInterval(days: Int) = settingsStore.setInterval(days)
  suspend fun setReminderEnabled(enabled: Boolean) = settingsStore.setReminderEnabled(enabled)
}
