package com.jsundmer.ningwen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsundmer.ningwen.data.Record
import com.jsundmer.ningwen.data.Repository
import com.jsundmer.ningwen.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NingwenViewModel(app: Application) : AndroidViewModel(app) {
  private val repo = Repository(app)

  val records: StateFlow<List<Record>> =
    repo.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val settings: StateFlow<Settings> =
    repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

  fun add(value: Double, measuredAt: Long, doseTabs: Double?, note: String?) = viewModelScope.launch {
    repo.addRecord(Record(value = value, measuredAt = measuredAt, doseTabs = doseTabs, note = note))
  }

  fun delete(record: Record) = viewModelScope.launch { repo.deleteRecord(record) }

  fun updateTargetRange(min: Double, max: Double) = viewModelScope.launch { repo.setTargetRange(min, max) }
  fun updateInterval(days: Int) = viewModelScope.launch { repo.setInterval(days) }
  fun updateReminder(enabled: Boolean) = viewModelScope.launch { repo.setReminderEnabled(enabled) }
}
