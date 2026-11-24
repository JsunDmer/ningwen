package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {

  @Test fun parse_latest_release_json_with_apk_asset() {
    val json = """
      {
        "tag_name":"v1.0.4",
        "html_url":"https://github.com/JsunDmer/ningwen/releases/tag/v1.0.4",
        "assets":[
          {"name":"app-release.apk","browser_download_url":"https://github.com/JsunDmer/ningwen/releases/download/v1.0.4/app-release.apk"}
        ]
      }
    """.trimIndent()
    val release = UpdateCheck.parseLatest(json)
    assertEquals("v1.0.4", release.tagName)
    assertEquals("https://github.com/JsunDmer/ningwen/releases/tag/v1.0.4", release.htmlUrl)
    assertEquals(
      "https://github.com/JsunDmer/ningwen/releases/download/v1.0.4/app-release.apk",
      release.apkUrl,
    )
  }

  @Test fun parse_latest_without_apk_asset() {
    val json = """{"tag_name":"v1.0.4","html_url":"https://example.com/r","assets":[]}"""
    assertNull(UpdateCheck.parseLatest(json).apkUrl)
  }

  @Test fun decide_update_when_remote_newer() {
    val release = LatestRelease(
      tagName = "v1.0.4",
      htmlUrl = "https://example.com/r",
      apkUrl = "https://example.com/a.apk",
    )
    val decision = UpdateCheck.decide(current = "1.0.3", release = release)
    assertTrue(decision is UpdateDecision.Available)
    decision as UpdateDecision.Available
    assertEquals("1.0.4", decision.version)
    assertEquals("https://example.com/a.apk", decision.apkUrl)
  }

  @Test fun decide_up_to_date_when_same_or_older() {
    val same = LatestRelease("v1.0.3", "https://x", null)
    val older = LatestRelease("v1.0.2", "https://x", null)
    assertEquals(UpdateDecision.UpToDate, UpdateCheck.decide("1.0.3", same))
    assertEquals(UpdateDecision.UpToDate, UpdateCheck.decide("1.0.4", older))
  }
}
