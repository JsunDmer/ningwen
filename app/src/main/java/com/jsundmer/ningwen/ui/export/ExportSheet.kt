package com.jsundmer.ningwen.ui.export

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.jsundmer.ningwen.data.Record
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(records: List<Record>, onDismiss: () -> Unit) {
  val context = LocalContext.current
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("导出数据", fontSize = 18.sp)
      Text("生成 CSV 文件，可通过系统分享给医生或家人。", fontSize = 13.sp)
      Button(
        onClick = { shareCsv(context, records) },
        modifier = Modifier.fillMaxWidth(),
      ) { Text("生成 CSV 并分享") }
      TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
    }
  }
}

private fun shareCsv(context: Context, records: List<Record>) {
  val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
  val header = "日期,INR,用量(片),备注"
  val body = records.sortedBy { it.measuredAt }.joinToString("\n") { r ->
    val dose = r.doseTabs?.toString() ?: ""
    val note = r.note?.replace(",", "，") ?: "" // 避免逗号破坏 CSV 列
    "${fmt.format(Date(r.measuredAt))},${r.value},$dose,$note"
  }
  val dir = File(context.cacheDir, "export").apply { mkdirs() }
  val file = File(dir, "ningwen.csv")
  file.writeText(header + "\n" + body)

  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/csv"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  context.startActivity(Intent.createChooser(intent, "分享数据"))
}
