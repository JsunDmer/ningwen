package com.jsundmer.ningwen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsundmer.ningwen.data.Settings

@Composable
fun SettingsScreen(
  settings: Settings,
  onRange: (Double, Double) -> Unit,
  onInterval: (Int) -> Unit,
  onReminder: (Boolean) -> Unit,
  onExport: () -> Unit,
) {
  var minText by remember(settings.targetMin) { mutableStateOf(settings.targetMin.toString()) }
  var maxText by remember(settings.targetMax) { mutableStateOf(settings.targetMax.toString()) }
  var intervalText by remember(settings.testIntervalDays) { mutableStateOf(settings.testIntervalDays.toString()) }

  fun commitRange() {
    val min = minText.toDoubleOrNull()
    val max = maxText.toDoubleOrNull()
    if (min != null && max != null && min < max) onRange(min, max)
  }
  fun commitInterval() {
    val d = intervalText.toIntOrNull()
    if (d != null && d >= 1) onInterval(d)
  }

  Column(
    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    CardRow("目标范围（INR）") {
      OutlinedTextField(minText, { minText = it; commitRange() }, label = { Text("下限") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.width(96.dp))
      Text("–")
      OutlinedTextField(maxText, { maxText = it; commitRange() }, label = { Text("上限") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.width(96.dp))
    }
    CardRow("下次测量间隔（天）") {
      OutlinedTextField(intervalText, { intervalText = it; commitInterval() }, label = { Text("天") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(120.dp))
    }
    CardRow("测量提醒") {
      Switch(checked = settings.reminderEnabled, onCheckedChange = onReminder)
    }
    CardRowClickable("导出 / 分享数据", onClick = onExport)
    Text(
      "数据仅保存在本机，不上传、无账号、无广告。\n本工具仅用于记录，不构成医疗建议。",
      fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
  }
}

@Composable
private fun CardRow(label: String, content: @Composable () -> Unit) {
  Card(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
      content()
    }
  }
}

@Composable
private fun CardRowClickable(label: String, onClick: () -> Unit) {
  Card(Modifier.fillMaxWidth(), onClick = onClick) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
      Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
