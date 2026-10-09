package com.jsundmer.ningwen

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.jsundmer.ningwen.ui.App
import com.jsundmer.ningwen.ui.theme.NingwenTheme

class MainActivity : ComponentActivity() {
  private val requestNotif =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 拒绝也能用 */ }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    setContent { NingwenTheme { App() } }
  }
}
