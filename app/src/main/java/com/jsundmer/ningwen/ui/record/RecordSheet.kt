package com.jsundmer.ningwen.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(
  lastDose: Double?,
  onDismiss: () -> Unit,
  onSubmit: (value: Double, measuredAt: Long, doseTabs: Double?, note: String?) -> Unit,
) {
  var valueText by remember { mutableStateOf("") }
  var doseText by remember { mutableStateOf(lastDose?.toString() ?: "") }
  var note by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  val chips = listOf("出血/淤青", "饮食变化", "漏服", "感冒/感染")

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
      Text("记录 INR", fontSize = 18.sp)
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = valueText,
        onValueChange = { valueText = it; error = null },
        label = { Text("INR 值") },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
      )
      error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = doseText,
        onValueChange = { doseText = it }, // 步进 0.25，用户可填 1 / 1.25 / 1.5
        label = { Text("华法林用量（片，可选）") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(12.dp))
      Text("快捷标签", fontSize = 13.sp)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { c ->
          FilterChip(
            selected = note.contains(c),
            onClick = { note = if (note.contains(c)) note.replace(c, "").trim() else (note + " " + c).trim() },
            label = { Text(c, fontSize = 12.sp) },
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("备注（可选）") },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
        Button(
          onClick = {
            val v = valueText.toDoubleOrNull()
            if (v == null || v < 0.5 || v > 5.0) {
              error = "请输入 0.5–5.0 之间的数值"
            } else {
              onSubmit(v, System.currentTimeMillis(), doseText.toDoubleOrNull(), note.ifBlank { null })
            }
          },
          modifier = Modifier.weight(1f),
        ) { Text("保存") }
      }
    }
  }
}
