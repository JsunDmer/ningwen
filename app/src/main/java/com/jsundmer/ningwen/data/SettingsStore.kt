package com.jsundmer.ningwen.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
  val targetMin: Double = 2.0,
  val targetMax: Double = 3.0,
  val testIntervalDays: Int = 7,
  val reminderEnabled: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
  private object Keys {
    val MIN = doublePreferencesKey("targetMin")
    val MAX = doublePreferencesKey("targetMax")
    val INTERVAL = intPreferencesKey("testIntervalDays")
    val REMIND = booleanPreferencesKey("reminderEnabled")
  }

  val flow: Flow<Settings> = context.dataStore.data.map { p ->
    Settings(
      targetMin = p[Keys.MIN] ?: 2.0,
      targetMax = p[Keys.MAX] ?: 3.0,
      testIntervalDays = p[Keys.INTERVAL] ?: 7,
      reminderEnabled = p[Keys.REMIND] ?: true,
    )
  }

  suspend fun setTargetRange(min: Double, max: Double) {
    context.dataStore.edit { it[Keys.MIN] = min; it[Keys.MAX] = max }
  }

  suspend fun setInterval(days: Int) {
    context.dataStore.edit { it[Keys.INTERVAL] = days.coerceAtLeast(1) }
  }

  suspend fun setReminderEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.REMIND] = enabled }
  }
}
