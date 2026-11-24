package com.jsundmer.ningwen.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class LatestRelease(
  val tagName: String,
  val htmlUrl: String,
  val apkUrl: String?,
)

sealed class UpdateDecision {
  data object UpToDate : UpdateDecision()
  data class Available(
    val version: String,
    val htmlUrl: String,
    val apkUrl: String?,
  ) : UpdateDecision()
}

object UpdateCheck {
  private val json = Json { ignoreUnknownKeys = true }

  fun parseLatest(raw: String): LatestRelease {
    val dto = json.decodeFromString(GithubReleaseDto.serializer(), raw)
    val apkUrl = dto.assets
      .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank() }
      ?.browserDownloadUrl
    return LatestRelease(tagName = dto.tagName, htmlUrl = dto.htmlUrl, apkUrl = apkUrl)
  }

  fun decide(current: String, release: LatestRelease): UpdateDecision {
    val remote = VersionNames.normalize(release.tagName)
    return if (VersionNames.compare(remote, current) > 0) {
      UpdateDecision.Available(
        version = remote,
        htmlUrl = release.htmlUrl,
        apkUrl = release.apkUrl,
      )
    } else {
      UpdateDecision.UpToDate
    }
  }
}

@Serializable
private data class GithubReleaseDto(
  @SerialName("tag_name") val tagName: String,
  @SerialName("html_url") val htmlUrl: String,
  val assets: List<GithubAssetDto> = emptyList(),
)

@Serializable
private data class GithubAssetDto(
  val name: String,
  @SerialName("browser_download_url") val browserDownloadUrl: String,
)
