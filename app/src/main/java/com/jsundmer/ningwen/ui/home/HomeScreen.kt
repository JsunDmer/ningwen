package com.jsundmer.ningwen.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsundmer.ningwen.data.Record
import com.jsundmer.ningwen.data.Settings
import com.jsundmer.ningwen.domain.InrRules
import com.jsundmer.ningwen.domain.InrStatus
import com.jsundmer.ningwen.ui.theme.Bad
import com.jsundmer.ningwen.ui.theme.Ok
import com.jsundmer.ningwen.ui.theme.Warn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

private fun statusColor(status: InrStatus): Color = when (status) {
  InrStatus.OK -> Ok
  InrStatus.HIGH -> Warn
  InrStatus.LOW -> Bad
}

private fun statusText(status: InrStatus): String = when (status) {
  InrStatus.OK -> "达标"
  InrStatus.HIGH -> "偏高"
  InrStatus.LOW -> "偏低"
}

@Composable
fun HomeScreen(records: List<Record>, settings: Settings, onDelete: (Record) -> Unit) {
  val latest = records.firstOrNull()
  LazyColumn(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { HeroCard(latest, settings, records.getOrNull(1)) }
    item { Text("最近记录", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    if (records.isEmpty()) {
      item { Text("还没有记录，点右下角 + 添加第一条。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else {
      items(records.take(5), key = { it.id }) { r ->
        RecordRow(r, settings, onClick = { onDelete(r) })
      }
    }
  }
}

@Composable
private fun HeroCard(latest: Record?, settings: Settings, prev: Record?) {
  Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp)) {
      if (latest == null) {
        Text("暂无记录", fontSize = 22.sp, fontWeight = FontWeight.Bold)
      } else {
        val status = InrRules.judge(latest.value, settings.targetMin, settings.targetMax)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            String.format(Locale.getDefault(), "%.1f", latest.value),
            fontSize = 44.sp, fontWeight = FontWeight.Bold,
          )
          Spacer(Modifier.padding(6.dp))
          Text(
            statusText(status),
            color = Color.White, fontSize = 13.sp,
            modifier = Modifier
              .background(statusColor(status), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        prev?.let {
          val delta = latest.value - it.value
          val arrow = if (delta >= 0) "↑" else "↓"
          Text(
            "较上次 $arrow ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(delta))}",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
          )
        }
        Spacer(Modifier.height(4.dp))
        Text(fmt.format(Date(latest.measuredAt)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
      }
      Spacer(Modifier.height(10.dp))
      val due = InrRules.nextTestMillis(latest?.measuredAt, settings.testIntervalDays)
      val days = InrRules.daysUntilDue(due, System.currentTimeMillis())
      val dueText = when {
        days == null -> "暂无记录"
        days >= 0 -> "距下次测量还有 $days 天"
        else -> "已超过测量时间"
      }
      Text(dueText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
      Text(
        "目标 ${settings.targetMin}–${settings.targetMax}",
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
      )
    }
  }
}

@Composable
private fun RecordRow(record: Record, settings: Settings, onClick: () -> Unit) {
  val status = InrRules.judge(record.value, settings.targetMin, settings.targetMax)
  Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(
        String.format(Locale.getDefault(), "%.1f", record.value),
        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = statusColor(status),
      )
      Spacer(Modifier.padding(8.dp))
      Column(Modifier.weight(1f)) {
        Text(fmt.format(Date(record.measuredAt)), fontSize = 14.sp)
        record.note?.takeIf { it.isNotBlank() }?.let {
          Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      record.doseTabs?.let {
        Text("${it} 片", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
