package com.jsundmer.ningwen.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsundmer.ningwen.BuildConfig
import com.jsundmer.ningwen.data.GithubUpdateClient
import com.jsundmer.ningwen.data.Settings
import com.jsundmer.ningwen.domain.UpdateCheck
import com.jsundmer.ningwen.domain.UpdateDecision
import com.jsundmer.ningwen.ui.update.ApkInstaller
import java.io.File
import kotlinx.coroutines.launch

private sealed class UpdateDialog {
  data object Checking : UpdateDialog()
  data object Downloading : UpdateDialog()
  data object UpToDate : UpdateDialog()
  data object Failed : UpdateDialog()
  data object NeedPermission : UpdateDialog()
  data class Available(
    val version: String,
    val htmlUrl: String,
    val apkUrl: String?,
  ) : UpdateDialog()
}

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
  var updateDialog by remember { mutableStateOf<UpdateDialog?>(null) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val client = remember { GithubUpdateClient() }

  fun commitRange() {
    val min = minText.toDoubleOrNull()
    val max = maxText.toDoubleOrNull()
    if (min != null && max != null && min < max) onRange(min, max)
  }
  fun commitInterval() {
    val d = intervalText.toIntOrNull()
    if (d != null && d >= 1) onInterval(d)
  }

  fun checkUpdate() {
    if (updateDialog is UpdateDialog.Checking || updateDialog is UpdateDialog.Downloading) return
    updateDialog = UpdateDialog.Checking
    scope.launch {
      updateDialog = try {
        val latest = client.fetchLatest()
        when (val decision = UpdateCheck.decide(BuildConfig.VERSION_NAME, latest)) {
          UpdateDecision.UpToDate -> UpdateDialog.UpToDate
          is UpdateDecision.Available -> UpdateDialog.Available(
            version = decision.version,
            htmlUrl = decision.htmlUrl,
            apkUrl = decision.apkUrl,
          )
        }
      } catch (_: Exception) {
        UpdateDialog.Failed
      }
    }
  }

  fun startDownload(version: String, apkUrl: String) {
    if (!ApkInstaller.canInstallPackages(context)) {
      updateDialog = UpdateDialog.NeedPermission
      return
    }
    updateDialog = UpdateDialog.Downloading
    scope.launch {
      try {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(dir, "ningwen-$version.apk")
        client.downloadApk(apkUrl, apk)
        updateDialog = null
        ApkInstaller.install(context, apk)
      } catch (_: Exception) {
        updateDialog = UpdateDialog.Failed
      }
    }
  }

  val updateLabel = when (updateDialog) {
    is UpdateDialog.Checking -> "正在检查…"
    is UpdateDialog.Downloading -> "正在下载…"
    else -> "检查更新（当前 ${BuildConfig.VERSION_NAME}）"
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
    CardRowClickable(label = updateLabel, onClick = ::checkUpdate)
    Text(
      "数据仅保存在本机，不上传、无账号、无广告。检查更新时会访问 GitHub。\n更新请直接覆盖安装，不要先卸载，否则记录会丢失。\n本工具仅用于记录，不构成医疗建议。",
      fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
  }

  when (val dialog = updateDialog) {
    null, is UpdateDialog.Checking, is UpdateDialog.Downloading -> Unit
    UpdateDialog.UpToDate -> AlertDialog(
      onDismissRequest = { updateDialog = null },
      title = { Text("检查更新") },
      text = { Text("已是最新版本（${BuildConfig.VERSION_NAME}）") },
      confirmButton = { TextButton(onClick = { updateDialog = null }) { Text("好的") } },
    )
    UpdateDialog.Failed -> AlertDialog(
      onDismissRequest = { updateDialog = null },
      title = { Text("检查更新") },
      text = { Text("检查或下载失败，请稍后重试") },
      confirmButton = { TextButton(onClick = { updateDialog = null }) { Text("好的") } },
    )
    UpdateDialog.NeedPermission -> AlertDialog(
      onDismissRequest = { updateDialog = null },
      title = { Text("需要安装权限") },
      text = { Text("请允许凝稳安装应用，以便覆盖更新并保留记录。") },
      confirmButton = {
        TextButton(onClick = {
          ApkInstaller.openUnknownSourcesSettings(context)
          updateDialog = null
        }) { Text("去设置") }
      },
      dismissButton = { TextButton(onClick = { updateDialog = null }) { Text("取消") } },
    )
    is UpdateDialog.Available -> AlertDialog(
      onDismissRequest = { updateDialog = null },
      title = { Text("发现新版本 ${dialog.version}") },
      text = {
        Text(
          if (dialog.apkUrl != null) {
            "将下载并覆盖安装，不会卸载应用，记录会保留。请在系统安装页确认更新。"
          } else {
            "未找到安装包，将打开发布页手动下载。请选择覆盖安装，不要卸载。"
          },
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val apkUrl = dialog.apkUrl
          if (apkUrl != null) {
            startDownload(dialog.version, apkUrl)
          } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dialog.htmlUrl)))
            updateDialog = null
          }
        }) { Text(if (dialog.apkUrl != null) "下载并安装" else "去下载") }
      },
      dismissButton = { TextButton(onClick = { updateDialog = null }) { Text("稍后再说") } },
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
  Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
      Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
