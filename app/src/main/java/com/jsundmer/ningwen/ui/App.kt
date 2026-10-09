package com.jsundmer.ningwen.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsundmer.ningwen.data.Record
import com.jsundmer.ningwen.ui.export.ExportSheet
import com.jsundmer.ningwen.ui.home.HomeScreen
import com.jsundmer.ningwen.ui.record.RecordSheet
import com.jsundmer.ningwen.ui.settings.SettingsScreen
import com.jsundmer.ningwen.ui.trend.TrendScreen

enum class Tab(val label: String) { HOME("首页"), TREND("趋势"), SETTINGS("我的") }

private fun tabIcon(tab: Tab): ImageVector = when (tab) {
  Tab.HOME -> Icons.Filled.Home
  Tab.TREND -> Icons.Filled.ShowChart
  Tab.SETTINGS -> Icons.Filled.Settings
}

@Composable
fun App(vm: NingwenViewModel = viewModel()) {
  val records by vm.records.collectAsStateWithLifecycle()
  val settings by vm.settings.collectAsStateWithLifecycle()
  var tab by remember { mutableStateOf(Tab.HOME) }
  var showRecord by remember { mutableStateOf(false) }
  var showExport by remember { mutableStateOf(false) }
  var pendingDelete by remember { mutableStateOf<Record?>(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showRecord = true },
        containerColor = MaterialTheme.colorScheme.primary,
      ) { Icon(Icons.Filled.Add, contentDescription = "记录") }
    },
    bottomBar = {
      NavigationBar {
        Tab.entries.forEach { t ->
          NavigationBarItem(
            selected = tab == t,
            onClick = { tab = t },
            icon = { Icon(tabIcon(t), contentDescription = t.label) },
            label = { Text(t.label) },
          )
        }
      }
    },
  ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize()) {
      when (tab) {
        Tab.HOME -> HomeScreen(records, settings, onDelete = { pendingDelete = it })
        Tab.TREND -> TrendScreen(records, settings)
        Tab.SETTINGS -> SettingsScreen(
          settings = settings,
          onRange = vm::updateTargetRange,
          onInterval = vm::updateInterval,
          onReminder = vm::updateReminder,
          onExport = { showExport = true },
        )
      }
    }
  }

  if (showRecord) {
    RecordSheet(
      lastDose = records.firstOrNull { it.doseTabs != null }?.doseTabs,
      onDismiss = { showRecord = false },
      onSubmit = { value, at, dose, note ->
        vm.add(value, at, dose, note); showRecord = false
      },
    )
  }

  if (showExport) ExportSheet(records, onDismiss = { showExport = false })

  pendingDelete?.let { r ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除这条记录？") },
      text = { Text("${r.value} · ${r.note ?: "无备注"}") },
      confirmButton = {
        TextButton(onClick = { vm.delete(r); pendingDelete = null }) { Text("删除") }
      },
      dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
    )
  }
}
