package com.jsundmer.ningwen.ui.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsundmer.ningwen.data.Record
import com.jsundmer.ningwen.data.Settings
import com.jsundmer.ningwen.domain.InrRules
import com.jsundmer.ningwen.ui.theme.Brand
import com.jsundmer.ningwen.ui.theme.Ok

// 折线图范围（毫秒）。Infinity 表示全部。
private val RANGES = listOf("90 天" to 90L * InrRules.DAY_MILLIS, "全部" to Long.MAX_VALUE)

@Composable
fun TrendScreen(records: List<Record>, settings: Settings) {
  var rangeIndex by remember { mutableStateOf(0) }
  val range = RANGES[rangeIndex].second
  val cutoff = if (range == Long.MAX_VALUE) Long.MIN_VALUE else System.currentTimeMillis() - range
  val data = records.filter { it.measuredAt >= cutoff }.sortedBy { it.measuredAt } // 时间升序

  Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      RANGES.forEachIndexed { i, (label, _) ->
        FilterChip(selected = rangeIndex == i, onClick = { rangeIndex = i }, label = { Text(label, fontSize = 12.sp) })
      }
    }
    val okCount = data.count { InrRules.judge(it.value, settings.targetMin, settings.targetMax) == com.jsundmer.ningwen.domain.InrStatus.OK }
    val rate = if (data.isEmpty()) 0 else (okCount * 100 / data.size)
    Card(Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Stat("平均", if (data.isEmpty()) "-" else String.format(java.util.Locale.getDefault(), "%.1f", data.map { it.value }.average()))
          Stat("最高", if (data.isEmpty()) "-" else String.format(java.util.Locale.getDefault(), "%.1f", data.maxOf { it.value }))
          Stat("最低", if (data.isEmpty()) "-" else String.format(java.util.Locale.getDefault(), "%.1f", data.minOf { it.value }))
          Stat("达标率", "$rate%")
        }
        if (data.isNotEmpty()) {
          Column(Modifier.fillMaxWidth().height(200.dp)) {
            LineChart(data, settings)
          }
        } else {
          Text("该范围内暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
      }
    }
  }
}

@Composable
private fun Stat(label: String, value: String) {
  Column {
    Text(value, fontSize = 18.sp)
    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun LineChart(data: List<Record>, settings: Settings) {
  Canvas(Modifier.fillMaxWidth().height(200.dp)) {
    val w = size.width
    val h = size.height
    val pad = 24f
    // y 轴范围：目标范围上下各留 0.5，至少 1.0 跨度
    val lo = minOf(settings.targetMin, data.minOf { it.value }) - 0.5
    val hi = maxOf(settings.targetMax, data.maxOf { it.value }) + 0.5
    val span = (hi - lo).coerceAtLeast(1.0)

    fun y(v: Double) = (h - pad) - ((v - lo) / span * (h - 2 * pad)).toFloat()
    fun x(i: Int) = if (data.size == 1) w / 2 else pad + i * (w - 2 * pad) / (data.size - 1)

    // 目标带
    drawRect(
      color = Ok.copy(alpha = 0.15f),
      topLeft = Offset(0f, y(settings.targetMax)),
      size = androidx.compose.ui.geometry.Size(w, (y(settings.targetMin) - y(settings.targetMax))),
    )
    // 折线
    val path = Path()
    data.forEachIndexed { i, r ->
      val px = x(i); val py = y(r.value)
      if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(path, color = Brand, style = Stroke(width = 4f))
    // 点
    data.forEachIndexed { i, r -> drawCircle(color = Brand, radius = 5f, center = Offset(x(i), y(r.value))) }
  }
}
