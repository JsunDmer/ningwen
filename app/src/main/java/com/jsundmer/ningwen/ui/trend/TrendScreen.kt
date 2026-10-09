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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsundmer.ningwen.data.Record
import com.jsundmer.ningwen.data.Settings
import com.jsundmer.ningwen.domain.InrRules
import com.jsundmer.ningwen.ui.theme.Brand
import com.jsundmer.ningwen.ui.theme.Ok
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

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
          LineChart(data, settings)
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
  val measurer = rememberTextMeasurer()
  val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val axisColor = MaterialTheme.colorScheme.outline
  val dateFmt = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }

  Canvas(Modifier.fillMaxWidth().height(220.dp)) {
    val w = size.width
    val h = size.height
    val padLeft = 40f
    val padRight = 10f
    val padTop = 12f
    val padBottom = 28f
    val plotW = w - padLeft - padRight
    val plotH = h - padTop - padBottom

    // y 轴范围：目标范围上下各留 0.5，至少 1.0 跨度
    val lo = minOf(settings.targetMin, data.minOf { it.value }) - 0.5
    val hi = maxOf(settings.targetMax, data.maxOf { it.value }) + 0.5
    val span = (hi - lo).coerceAtLeast(1.0)

    fun y(v: Double) = padTop + plotH - ((v - lo) / span * plotH).toFloat()
    fun x(i: Int) = if (data.size == 1) padLeft + plotW / 2 else padLeft + i * plotW / (data.size - 1)

    // 目标带
    drawRect(
      color = Ok.copy(alpha = 0.15f),
      topLeft = Offset(padLeft, y(settings.targetMax)),
      size = Size(plotW, (y(settings.targetMin) - y(settings.targetMax))),
    )

    val step = niceStep(span / 4)
    var tick = ceil(lo / step) * step
    while (tick <= hi + 1e-9) {
      val ty = y(tick)
      drawLine(gridColor, Offset(padLeft, ty), Offset(w - padRight, ty), strokeWidth = 1f)
      val layout = measurer.measure(String.format(Locale.getDefault(), "%.1f", tick), labelStyle)
      drawText(layout, topLeft = Offset(padLeft - layout.size.width - 6f, ty - layout.size.height / 2f))
      tick += step
    }

    drawLine(axisColor, Offset(padLeft, padTop), Offset(padLeft, padTop + plotH), strokeWidth = 1.5f)
    drawLine(axisColor, Offset(padLeft, padTop + plotH), Offset(w - padRight, padTop + plotH), strokeWidth = 1.5f)

    // 折线
    val path = Path()
    data.forEachIndexed { i, r ->
      val px = x(i); val py = y(r.value)
      if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(path, color = Brand, style = Stroke(width = 4f))
    // 点
    data.forEachIndexed { i, r -> drawCircle(color = Brand, radius = 5f, center = Offset(x(i), y(r.value))) }

    val labelIndices = if (data.size <= 4) data.indices.toList() else listOf(0, data.size / 2, data.size - 1)
    labelIndices.distinct().forEach { i ->
      val layout = measurer.measure(dateFmt.format(Date(data[i].measuredAt)), labelStyle)
      val cx = (x(i) - layout.size.width / 2f).coerceIn(padLeft, w - padRight - layout.size.width)
      drawText(layout, topLeft = Offset(cx, h - padBottom + 6f))
    }
  }
}

private fun niceStep(raw: Double): Double {
  if (raw <= 0.0 || !raw.isFinite()) return 1.0
  val base = 10.0.pow(floor(log10(raw)))
  val n = raw / base
  val m = when {
    n <= 1.0 -> 1.0
    n <= 2.0 -> 2.0
    n <= 5.0 -> 5.0
    else -> 10.0
  }
  return m * base
}
