package com.jsundmer.ningwen.data

import com.jsundmer.ningwen.domain.LatestRelease
import com.jsundmer.ningwen.domain.UpdateCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class GithubUpdateClient(
  private val latestUrl: String = "https://api.github.com/repos/JsunDmer/ningwen/releases/latest",
) {
  suspend fun fetchLatest(): LatestRelease = withContext(Dispatchers.IO) {
    val body = httpGet(latestUrl, accept = "application/vnd.github+json")
    UpdateCheck.parseLatest(body)
  }

  /** 下载 APK 到指定文件；跟随重定向。 */
  suspend fun downloadApk(apkUrl: String, dest: File) = withContext(Dispatchers.IO) {
    dest.parentFile?.mkdirs()
    var url = apkUrl
    var redirects = 0
    while (true) {
      val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = false
        connectTimeout = 15_000
        readTimeout = 60_000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "ningwen-android")
        setRequestProperty("Accept", "application/octet-stream")
      }
      try {
        val code = conn.responseCode
        when (code) {
          in 200..299 -> {
            conn.inputStream.use { input ->
              dest.outputStream().use { output -> input.copyTo(output) }
            }
            return@withContext
          }
          301, 302, 303, 307, 308 -> {
            val next = conn.getHeaderField("Location") ?: error("redirect without Location")
            url = next
            redirects++
            if (redirects > 5) error("too many redirects")
          }
          else -> error("HTTP $code")
        }
      } finally {
        conn.disconnect()
      }
    }
  }

  private fun httpGet(url: String, accept: String): String {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
      connectTimeout = 8_000
      readTimeout = 8_000
      requestMethod = "GET"
      setRequestProperty("Accept", accept)
      setRequestProperty("User-Agent", "ningwen-android")
    }
    try {
      val code = conn.responseCode
      val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
        ?.bufferedReader()
        ?.use { it.readText() }
        .orEmpty()
      if (code !in 200..299) error("HTTP $code")
      return body
    } finally {
      conn.disconnect()
    }
  }
}
