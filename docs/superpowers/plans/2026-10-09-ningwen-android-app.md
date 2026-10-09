# 凝稳 · 安卓 App 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把「凝稳」INR 记录 App 从高保真原型落地为可安装的安卓原生应用，并通过 GitHub Actions 云端构建、发布 Release APK。

**Architecture:** 单模块 Kotlin + Jetpack Compose（Material 3）。数据分两层：记录用 Room（SQLite），设置用 DataStore。领域规则 `InrRules` 为纯 Kotlin 函数（可单测）。提醒用 WorkManager 每日本地检查到期。UI 按页面拆分为 Home / Record / Trend / Settings / Export，底部 3 Tab + FAB 状态机切换（不引 Navigation）。

**Tech Stack:** Kotlin 2.0.20、Compose BOM 2024.09.02、AGP 8.5.2、Gradle 8.7、JDK 17、Room 2.6.1(KSP)、DataStore 1.1.1、WorkManager 2.9.1、kotlinx-serialization 1.7.1。

> 上游：[技术设计 spec](../specs/2026-10-09-ningwen-android-app-design.md) · [产品设计 spec](../specs/2026-10-09-inr-tracker-app-design.md) · [HTML 原型](../../prototype/)

**重要前提：** 本机**未安装 Android SDK**，无法本地编译。所有编译/测试由 GitHub Actions 完成。因此验证步骤以"push → 看 CI"为主；`./gradlew` 命令标注为"仅 CI 可跑"。

---

## 文件结构（先锁定，再拆任务）

```
ningwen/  (仓库根，remote JsunDmer/ningwen)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.jar
├── .gitignore
├── README.md
├── .github/workflows/android-release.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── test/java/com/jsundmer/ningwen/domain/InrRulesTest.kt
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/jsundmer/ningwen/
│           │   ├── NingwenApp.kt
│           │   ├── MainActivity.kt
│           │   ├── domain/InrRules.kt
│           │   ├── data/Record.kt
│           │   ├── data/RecordDao.kt
│           │   ├── data/NingwenDatabase.kt
│           │   ├── data/SettingsStore.kt
│           │   ├── data/Repository.kt
│           │   ├── notify/Notifications.kt
│           │   ├── notify/ReminderWorker.kt
│           │   └── ui/AppState.kt
│           │   └── ui/App.kt
│           │   └── ui/home/HomeScreen.kt
│           │   └── ui/record/RecordSheet.kt
│           │   └── ui/trend/TrendScreen.kt
│           │   └── ui/settings/SettingsScreen.kt
│           │   └── ui/export/ExportSheet.kt
│           │   └── ui/theme/{Color,Type,Theme}.kt
│           └── res/
│               ├── values/{strings.xml,themes.xml,colors.xml}
│               ├── xml/file_paths.xml
│               └── mipmap-anydpi-v26/ic_launcher.xml (+ adaptive icons)
├── prototype/   (现有 HTML)
└── docs/superpowers/  (specs + plans)
```

---

## Task 1: 仓库与 Gradle 骨架

**Files:**
- Create: `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 初始化 git 仓库与远程**

```bash
cd /Users/sunjian/Downloads/inr-tracker
git init -b main
printf '%s\n' '# 原型与文档' '' > /dev/null
git remote add origin https://github.com/JsunDmer/ningwen.git 2>/dev/null || true
```

- [ ] **Step 2: 写 `.gitignore`**

```gitignore
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build
/app/build
/captures
.externalNativeBuild
.cxx
*.jks
```

- [ ] **Step 3: 写 `settings.gradle.kts`**

```kotlin
pluginManagement {
  repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories { google(); mavenCentral() }
}
rootProject.name = "ningwen"
include(":app")
```

- [ ] **Step 4: 写 `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
coreKtx = "1.13.1"
lifecycle = "2.8.6"
activityCompose = "1.9.2"
composeBom = "2024.09.02"
room = "2.6.1"
datastore = "1.1.1"
work = "2.9.1"
serialization = "1.7.1"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 5: 写根 `build.gradle.kts`**

```kotlin
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 6: 写 `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 7: 写 `gradle/wrapper/gradle-wrapper.properties`**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 8: 写 `app/build.gradle.kts`**

```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.jsundmer.ningwen"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.jsundmer.ningwen"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0.0"
  }

  signingConfigs {
    create("release") {
      // 仅当 CI 注入了 keystore 环境变量时才配置，本地/无密钥构建自动降级
      val ksPath = System.getenv("KEYSTORE_PATH")
      if (ksPath != null) {
        storeFile = file(ksPath)
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (System.getenv("KEYSTORE_PATH") != null) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { compose = true }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.kotlinx.serialization.json)
  debugImplementation(libs.androidx.ui.tooling)
  testImplementation(libs.junit)
}
```

- [ ] **Step 9: 写 `app/proguard-rules.pro`（空规则占位）**

```proguard
# 保留 Room 生成的实现（KSP 生成，一般无需额外规则）
-keep class * extends androidx.room.RoomDatabase
```

- [ ] **Step 10: 写 `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

  <application
    android:name=".NingwenApp"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.Ningwen">

    <activity
      android:name=".MainActivity"
      android:exported="true"
      android:label="@string/app_name"
      android:theme="@style/Theme.Ningwen">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>

    <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="${applicationId}.fileprovider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
    </provider>
  </application>
</manifest>
```

- [ ] **Step 11: 生成 Gradle Wrapper jar**

本机无 Android SDK 但装了 gradle 8.x；用 `gradle wrapper --gradle-version 8.7` 生成 `gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar`。

Run: `cd /Users/sunjian/Downloads/inr-tracker && gradle wrapper --gradle-version 8.7`
Expected: 生成 4 个 wrapper 文件。

- [ ] **Step 12: 提交**

```bash
git add -A
git commit -m "chore: scaffold Android Gradle project"
```

---

## Task 2: 领域规则 `InrRules`（纯函数 + 单测）

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/domain/InrRules.kt`
- Test: `app/src/test/java/com/jsundmer/ningwen/domain/InrRulesTest.kt`

- [ ] **Step 1: 写失败测试 `InrRulesTest.kt`**

```kotlin
package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InrRulesTest {

  private val DAY = 86_400_000L

  @Test fun judge_low_when_below_min() {
    assertEquals(InrStatus.LOW, InrRules.judge(1.8, 2.0, 3.0))
  }

  @Test fun judge_ok_when_in_range_inclusive() {
    assertEquals(InrStatus.OK, InrRules.judge(2.0, 2.0, 3.0))
    assertEquals(InrStatus.OK, InrRules.judge(3.0, 2.0, 3.0))
  }

  @Test fun judge_high_when_above_max() {
    assertEquals(InrStatus.HIGH, InrRules.judge(3.4, 2.0, 3.0))
  }

  @Test fun nextTestMillis_adds_interval_days() {
    val last = 1_000_000_000_000L
    assertEquals(last + 7 * DAY, InrRules.nextTestMillis(last, 7))
  }

  @Test fun daysUntilDue_rounds_up() {
    val now = 1_000_000_000_000L
    val due = now + 3 * DAY + 1 // 3 天零 1ms → 向上取整为 4
    assertEquals(4L, InrRules.daysUntilDue(due, now))
  }

  @Test fun daysUntilDue_null_when_no_due() {
    assertNull(InrRules.daysUntilDue(null, 0L))
  }

  @Test fun dueBucket_classifies_reminder_days() {
    assertEquals(ReminderBucket.TOMORROW, InrRules.dueBucket(1))
    assertEquals(ReminderBucket.TODAY, InrRules.dueBucket(0))
    assertEquals(ReminderBucket.NONE, InrRules.dueBucket(5))
    assertEquals(ReminderBucket.NONE, InrRules.dueBucket(-2))
  }
}
```

- [ ] **Step 2: 运行测试确认失败（仅 CI）**

Run（CI）: `./gradlew :app:testDebugUnitTest --tests "com.jsundmer.ningwen.domain.InrRulesTest"`
Expected: 编译失败（`InrRules`/`InrStatus` 未定义）。

- [ ] **Step 3: 实现 `InrRules.kt`**

```kotlin
package com.jsundmer.ningwen.domain

/** INR 达标状态。 */
enum class InrStatus { OK, HIGH, LOW }

/** 到期提醒分类。 */
enum class ReminderBucket { TOMORROW, TODAY, NONE }

/**
 * 纯领域规则（无 Android 依赖，可单测）。
 * 所有"达标/到期/提醒"判断的唯一来源，UI 与通知都调用它，避免逻辑分散。
 */
object InrRules {
  const val DAY_MILLIS = 86_400_000L

  fun judge(value: Double, min: Double, max: Double): InrStatus = when {
    value < min -> InrStatus.LOW
    value > max -> InrStatus.HIGH
    else -> InrStatus.OK
  }

  /** 无记录返回 null。 */
  fun nextTestMillis(lastMeasuredAt: Long?, intervalDays: Int): Long? =
    lastMeasuredAt?.let { it + intervalDays * DAY_MILLIS }

  /** 距到期还剩几天；向上取整（今天到期含未来不足一天记 0）。无到期返回 null。 */
  fun daysUntilDue(dueMillis: Long?, nowMillis: Long): Long? =
    dueMillis?.let { Math.ceil((it - nowMillis).toDouble() / DAY_MILLIS).toLong() }

  /** 提醒分类：提前 1 天 / 当天 / 不提醒。 */
  fun dueBucket(daysUntilDue: Long?): ReminderBucket = when (daysUntilDue) {
    1L -> ReminderBucket.TOMORROW
    0L -> ReminderBucket.TODAY
    else -> ReminderBucket.NONE
  }
}
```

- [ ] **Step 4: 运行测试确认通过（仅 CI）**

Run（CI）: `./gradlew :app:testDebugUnitTest --tests "com.jsundmer.ningwen.domain.InrRulesTest"`
Expected: 全部 PASS（7 个用例）。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/domain/InrRules.kt app/src/test/java/com/jsundmer/ningwen/domain/InrRulesTest.kt
git commit -m "feat(domain): add INR rules with unit tests"
```

---

## Task 3: 数据层（Room + DataStore + Repository）

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/data/Record.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/data/RecordDao.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/data/NingwenDatabase.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/data/SettingsStore.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/data/Repository.kt`

- [ ] **Step 1: 写 `Record.kt`**

```kotlin
package com.jsundmer.ningwen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val value: Double,
  val measuredAt: Long,
  val doseTabs: Double? = null,
  val note: String? = null,
)
```

- [ ] **Step 2: 写 `RecordDao.kt`**

```kotlin
package com.jsundmer.ningwen.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
  @Query("SELECT * FROM records ORDER BY measuredAt DESC")
  fun observeAll(): Flow<List<Record>>

  @Query("SELECT * FROM records ORDER BY measuredAt DESC LIMIT 1")
  suspend fun latest(): Record?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(record: Record): Long

  @Delete
  suspend fun delete(record: Record)
}
```

- [ ] **Step 3: 写 `NingwenDatabase.kt`**

```kotlin
package com.jsundmer.ningwen.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Record::class], version = 1, exportSchema = false)
abstract class NingwenDatabase : RoomDatabase() {
  abstract fun recordDao(): RecordDao

  companion object {
    @Volatile private var instance: NingwenDatabase? = null

    fun get(context: Context): NingwenDatabase = instance ?: synchronized(this) {
      instance ?: Room.databaseBuilder(
        context.applicationContext,
        NingwenDatabase::class.java,
        "ningwen.db",
      ).build().also { instance = it }
    }
  }
}
```

- [ ] **Step 4: 写 `SettingsStore.kt`**

```kotlin
package com.jsundmer.ningwen.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
  val targetMin: Double = 2.0,
  val targetMax: Double = 3.0,
  val testIntervalDays: Int = 7,
  val reminderEnabled: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
  private object Keys {
    val MIN = doublePreferencesKey("targetMin")
    val MAX = doublePreferencesKey("targetMax")
    val INTERVAL = intPreferencesKey("testIntervalDays")
    val REMIND = booleanPreferencesKey("reminderEnabled")
  }

  val flow: Flow<Settings> = context.dataStore.data.map { p ->
    Settings(
      targetMin = p[Keys.MIN] ?: 2.0,
      targetMax = p[Keys.MAX] ?: 3.0,
      testIntervalDays = p[Keys.INTERVAL] ?: 7,
      reminderEnabled = p[Keys.REMIND] ?: true,
    )
  }

  suspend fun setTargetRange(min: Double, max: Double) {
    context.dataStore.edit { it[Keys.MIN] = min; it[Keys.MAX] = max }
  }

  suspend fun setInterval(days: Int) {
    context.dataStore.edit { it[Keys.INTERVAL] = days.coerceAtLeast(1) }
  }

  suspend fun setReminderEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.REMIND] = enabled }
  }
}
```

- [ ] **Step 5: 写 `Repository.kt`**

```kotlin
package com.jsundmer.ningwen.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** UI 唯一数据入口：聚合记录与设置，隐藏 Room/DataStore 细节。 */
class Repository(context: Context) {
  private val dao = NingwenDatabase.get(context).recordDao()
  private val settingsStore = SettingsStore(context)

  val records: Flow<List<Record>> = dao.observeAll()
  val settings: Flow<Settings> = settingsStore.flow

  suspend fun addRecord(record: Record) = dao.insert(record)
  suspend fun deleteRecord(record: Record) = dao.delete(record)
  suspend fun latestRecord(): Record? = dao.latest()

  suspend fun setTargetRange(min: Double, max: Double) = settingsStore.setTargetRange(min, max)
  suspend fun setInterval(days: Int) = settingsStore.setInterval(days)
  suspend fun setReminderEnabled(enabled: Boolean) = settingsStore.setReminderEnabled(enabled)
}
```

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/data/
git commit -m "feat(data): add Room, DataStore settings and repository"
```

---

## Task 4: 主题（沿用原型配色）

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/theme/Color.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/theme/Type.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/theme/Theme.kt`

- [ ] **Step 1: 写 `Color.kt`**

```kotlin
package com.jsundmer.ningwen.ui.theme

import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF2F6FED)
val Ok = Color(0xFF17A673)
val Warn = Color(0xFFE8890C)
val Bad = Color(0xFFE5484D)
val Bg = Color(0xFFF4F6F9)
val Card = Color(0xFFFFFFFF)
val TextMain = Color(0xFF1B1F27)
val TextSub = Color(0xFF6B7280)
val Line = Color(0xFFE3E7EE)
```

- [ ] **Step 2: 写 `Type.kt`**

```kotlin
package com.jsundmer.ningwen.ui.theme

import androidx.compose.material3.Typography

val NingwenTypography = Typography()
```

- [ ] **Step 3: 写 `Theme.kt`**

```kotlin
package com.jsundmer.ningwen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
  primary = Brand,
  background = Bg,
  surface = Card,
  onBackground = TextMain,
  onSurface = TextMain,
)

@Composable
fun NingwenTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColors,
    typography = NingwenTypography,
    content = content,
  )
}
```

- [ ] **Step 4: 写 `res/values/{strings.xml,themes.xml,colors.xml}` 与 `res/xml/file_paths.xml`**

`strings.xml`
```xml
<resources>
  <string name="app_name">凝稳</string>
</resources>
```

`themes.xml`
```xml
<resources>
  <style name="Theme.Ningwen" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`colors.xml`
```xml
<resources>
  <color name="brand">#2F6FED</color>
</resources>
```

`res/xml/file_paths.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
  <cache-path name="export" path="export/" />
</paths>
```

- [ ] **Step 5: 加启动图标（自适应）**

创建 `res/mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
  <background android:drawable="@color/brand" />
  <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
```
并放置任意前景图 `res/mipmap-xxxhdpi/ic_launcher_foreground.png`（纯色圆形占位即可）。

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/ui/theme/ app/src/main/res/
git commit -m "feat(ui): add theme and resources"
```

---

## Task 5: 通知与到期提醒（WorkManager）

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/notify/Notifications.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/notify/ReminderWorker.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/notify/ReminderScheduler.kt`
- Modify: `app/src/main/res/values/strings.xml`（加通知渠道文案）
- Create: `app/src/main/res/drawable/ic_notify.xml`

- [ ] **Step 1: 补通知渠道文案到 `strings.xml`**

```xml
<resources>
  <string name="app_name">凝稳</string>
  <string name="notif_channel_name">测量提醒</string>
  <string name="notif_channel_desc">到期前一天与当天提醒测量 INR</string>
</resources>
```

- [ ] **Step 2: 写 `res/drawable/ic_notify.xml`（简单矢量铃铛）**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
  android:width="24dp" android:height="24dp"
  android:viewportWidth="24" android:viewportHeight="24">
  <path android:fillColor="#FFFFFF"
    android:pathData="M12,22a2.5,2.5 0 0,0 2.45,-2h-4.9A2.5,2.5 0 0,0 12,22zM18,16v-5a6,6 0 0,0 -5,-5.91V4a1,1 0 1,0 -2,0v1.09A6,6 0 0,0 6,11v5l-1.7,1.7A1,1 0 0,0 5,19h14a1,1 0 0,0 0.7,-1.7z"/>
</vector>
```

- [ ] **Step 3: 写 `Notifications.kt`**

```kotlin
package com.jsundmer.ningwen.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jsundmer.ningwen.R

object Notifications {
  const val CHANNEL_ID = "reminder"
  private const val NOTIFICATION_ID = 1001

  /** 渠道需在发通知前建好；重复调用安全。 */
  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.notif_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply { description = context.getString(R.string.notif_channel_desc) }
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  /** 无通知权限时静默跳过（用户可只靠 App 内倒计时）。 */
  fun show(context: Context, title: String, text: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) return

    val n = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notify)
      .setContentTitle(title)
      .setContentText(text)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
  }
}
```

- [ ] **Step 4: 写 `ReminderWorker.kt`**

```kotlin
package com.jsundmer.ningwen.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jsundmer.ningwen.data.Repository
import com.jsundmer.ningwen.domain.InrRules
import com.jsundmer.ningwen.domain.ReminderBucket
import kotlinx.coroutines.flow.first

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
  override suspend fun doWork(): Result {
    val repo = Repository(applicationContext)
    val settings = repo.settings.first()
    if (!settings.reminderEnabled) return Result.success()

    val last = repo.latestRecord() ?: return Result.success()
    val due = InrRules.nextTestMillis(last.measuredAt, settings.testIntervalDays)
    val days = InrRules.daysUntilDue(due, System.currentTimeMillis())

    when (InrRules.dueBucket(days)) {
      ReminderBucket.TOMORROW -> Notifications.show(
        applicationContext, "明天该测 INR 了", "距下次测量还有 1 天，记得安排时间。",
      )
      ReminderBucket.TODAY -> Notifications.show(
        applicationContext, "今天该测 INR 了", "到测量日了，测完记得记录一下。",
      )
      ReminderBucket.NONE -> Unit
    }
    return Result.success()
  }
}
```

- [ ] **Step 5: 写 `ReminderScheduler.kt`**

```kotlin
package com.jsundmer.ningwen.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
  private const val WORK_NAME = "daily_reminder"

  /** 每日约 9:00 检查一次；UPDATE 策略保证配置/记录变化后重排为最新。 */
  fun schedule(context: Context) {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 9)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    val initialDelay = target.timeInMillis - now.timeInMillis

    val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
      .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
      .build()

    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
  }
}
```

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/notify/ app/src/main/res/
git commit -m "feat(notify): add reminder worker and scheduler"
```

---

## Task 6: ViewModel 与 App 骨架

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/NingwenViewModel.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/App.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/MainActivity.kt`

- [ ] **Step 1: 写 `NingwenViewModel.kt`**

```kotlin
package com.jsundmer.ningwen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsundmer.ningwen.data.Record
import com.jsundmer.ningwen.data.Repository
import com.jsundmer.ningwen.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NingwenViewModel(app: Application) : AndroidViewModel(app) {
  private val repo = Repository(app)

  val records: StateFlow<List<Record>> =
    repo.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val settings: StateFlow<Settings> =
    repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

  fun add(value: Double, measuredAt: Long, doseTabs: Double?, note: String?) = viewModelScope.launch {
    repo.addRecord(Record(value = value, measuredAt = measuredAt, doseTabs = doseTabs, note = note))
  }

  fun delete(record: Record) = viewModelScope.launch { repo.deleteRecord(record) }

  fun updateTargetRange(min: Double, max: Double) = viewModelScope.launch { repo.setTargetRange(min, max) }
  fun updateInterval(days: Int) = viewModelScope.launch { repo.setInterval(days) }
  fun updateReminder(enabled: Boolean) = viewModelScope.launch { repo.setReminderEnabled(enabled) }
}
```

- [ ] **Step 2: 写 `App.kt`**

```kotlin
package com.jsundmer.ningwen.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsundmer.ningwen.data.Record
import com.jsundmer.ningwen.ui.export.ExportSheet
import com.jsundmer.ningwen.ui.home.HomeScreen
import com.jsundmer.ningwen.ui.record.RecordSheet
import com.jsundmer.ningwen.ui.settings.SettingsScreen
import com.jsundmer.ningwen.ui.trend.TrendScreen

enum class Tab(val label: String) { HOME("首页"), TREND("趋势"), SETTINGS("我的") }

private fun tabIcon(tab: Tab): ImageVector = when (tab) {
  Tab.HOME -> Icons.Filled.Home
  Tab.TREND -> Icons.Filled.ShowChart
  Tab.SETTINGS -> Icons.Filled.Settings
}

@Composable
fun App(vm: NingwenViewModel = viewModel()) {
  val records by vm.records.collectAsStateWithLifecycle()
  val settings by vm.settings.collectAsStateWithLifecycle()
  var tab by remember { mutableStateOf(Tab.HOME) }
  var showRecord by remember { mutableStateOf(false) }
  var showExport by remember { mutableStateOf(false) }
  var pendingDelete by remember { mutableStateOf<Record?>(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showRecord = true },
        containerColor = MaterialTheme.colorScheme.primary,
      ) { Icon(Icons.Filled.Add, contentDescription = "记录") }
    },
    bottomBar = {
      NavigationBar {
        Tab.entries.forEach { t ->
          NavigationBarItem(
            selected = tab == t,
            onClick = { tab = t },
            icon = { Icon(tabIcon(t), contentDescription = t.label) },
            label = { Text(t.label) },
          )
        }
      }
    },
  ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize()) {
      when (tab) {
        Tab.HOME -> HomeScreen(records, settings, onDelete = { pendingDelete = it })
        Tab.TREND -> TrendScreen(records, settings)
        Tab.SETTINGS -> SettingsScreen(
          settings = settings,
          onRange = vm::updateTargetRange,
          onInterval = vm::updateInterval,
          onReminder = vm::updateReminder,
          onExport = { showExport = true },
        )
      }
    }
  }

  if (showRecord) {
    RecordSheet(
      lastDose = records.firstOrNull { it.doseTabs != null }?.doseTabs,
      onDismiss = { showRecord = false },
      onSubmit = { value, at, dose, note ->
        vm.add(value, at, dose, note); showRecord = false
      },
    )
  }

  if (showExport) ExportSheet(records, onDismiss = { showExport = false })

  pendingDelete?.let { r ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除这条记录？") },
      text = { Text("${r.value} · ${r.note ?: "无备注"}") },
      confirmButton = {
        TextButton(onClick = { vm.delete(r); pendingDelete = null }) { Text("删除") }
      },
      dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
    )
  }
}
```

- [ ] **Step 3: 写 `MainActivity.kt`**

```kotlin
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
```

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/ui/NingwenViewModel.kt app/src/main/java/com/jsundmer/ningwen/ui/App.kt app/src/main/java/com/jsundmer/ningwen/MainActivity.kt
git commit -m "feat(ui): add viewmodel, app shell and main activity"
```

---

## Task 7: 首页 `HomeScreen`

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/home/HomeScreen.kt`

- [ ] **Step 1: 写 `HomeScreen.kt`**

```kotlin
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
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/ui/home/HomeScreen.kt
git commit -m "feat(ui): add home screen"
```

---

## Task 8: 记录弹层 `RecordSheet`

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/record/RecordSheet.kt`

- [ ] **Step 1: 写 `RecordSheet.kt`**

```kotlin
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
```

> 注：`Theme` 已全局提供，`MaterialTheme` 需 `import androidx.compose.material3.MaterialTheme`（补上）。

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/ui/record/RecordSheet.kt
git commit -m "feat(ui): add record bottom sheet"
```

---

## Task 9: 趋势页 `TrendScreen`（Canvas 折线 + 目标带）

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/trend/TrendScreen.kt`

- [ ] **Step 1: 写 `TrendScreen.kt`**

```kotlin
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
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/ui/trend/TrendScreen.kt
git commit -m "feat(ui): add trend screen with canvas chart"
```

---

## Task 10: 设置页 `SettingsScreen`

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: 写 `SettingsScreen.kt`**

```kotlin
package com.jsundmer.ningwen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsundmer.ningwen.data.Settings

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

  fun commitRange() {
    val min = minText.toDoubleOrNull()
    val max = maxText.toDoubleOrNull()
    if (min != null && max != null && min < max) onRange(min, max)
  }
  fun commitInterval() {
    val d = intervalText.toIntOrNull()
    if (d != null && d >= 1) onInterval(d)
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
    Text(
      "数据仅保存在本机，不上传、无账号、无广告。\n本工具仅用于记录，不构成医疗建议。",
      fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
  Card(Modifier.fillMaxWidth(), onClick = onClick) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
      Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/ui/settings/SettingsScreen.kt
git commit -m "feat(ui): add settings screen"
```

---

## Task 11: 导出 `ExportSheet`

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/export/ExportSheet.kt`

- [ ] **Step 1: 写 `ExportSheet.kt`**

```kotlin
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
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/ui/export/ExportSheet.kt
git commit -m "feat(ui): add CSV export sheet"
```

---

## Task 12: 应用入口接线 `NingwenApp`

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/NingwenApp.kt`

- [ ] **Step 1: 写 `NingwenApp.kt`**

```kotlin
package com.jsundmer.ningwen

import android.app.Application
import com.jsundmer.ningwen.notify.Notifications
import com.jsundmer.ningwen.notify.ReminderScheduler

class NingwenApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Notifications.ensureChannel(this)
    ReminderScheduler.schedule(this)
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/jsundmer/ningwen/NingwenApp.kt
git commit -m "feat: wire application entry (channel + scheduler)"
```

---

## Task 13: CI/CD（GitHub Actions）与首个 Release

**Files:**
- Create: `.github/workflows/android-release.yml`

- [ ] **Step 1: 写 workflow**

```yaml
name: Android Release

on:
  push:
    tags: ['v*']
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      - name: Prepare signing
        run: |
          if [ -n "${{ secrets.KEYSTORE_BASE64 }}" ]; then
            echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > "$GITHUB_WORKSPACE/ningwen.jks"
            echo "KEYSTORE_PATH=$GITHUB_WORKSPACE/ningwen.jks" >> "$GITHUB_ENV"
          fi

      - name: Test & build
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew :app:testDebugUnitTest :app:assembleRelease --stacktrace

      - name: Attach APK to Release
        if: startsWith(github.ref, 'refs/tags/v')
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/release/*.apk

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: ningwen-apk
          path: app/build/outputs/apk/release/*.apk
```

- [ ] **Step 2: 生成签名密钥并写入 Secrets**

```bash
cd /Users/sunjian/Downloads/inr-tracker
keytool -genkeypair -v -keystore ningwen.jks -alias ningwen \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass ningwen123 -keypass ningwen123 \
  -dname "CN=ningwen, OU=personal, O=ningwen, L=SH, S=SH, C=CN"

base64 -i ningwen.jks | tr -d '\n' > /tmp/ks.b64
gh secret set KEYSTORE_BASE64 < /tmp/ks.b64
gh secret set KEYSTORE_PASSWORD --body "ningwen123"
gh secret set KEY_ALIAS --body "ningwen"
gh secret set KEY_PASSWORD --body "ningwen123"
```

> `ningwen.jks` 已被 `.gitignore` 忽略；**务必本地留一份备份**（丢了无法覆盖升级）。

- [ ] **Step 3: 首次推送并触发构建**

```bash
git add -A
git commit -m "ci: add android release workflow"
git push -u origin main
gh workflow run "Android Release"
gh run watch
```

- [ ] **Step 4: 打 tag 发首个 Release**

```bash
git tag v1.0.0
git push origin v1.0.0
```

Expected: Actions 绿 → `https://github.com/JsunDmer/ningwen/releases` 出现 `v1.0.0` 与 APK。
若 CI 报错：按日志修编译问题后重跑（预期 1~2 轮）。

- [ ] **Step 5: 提交**

```bash
git add .github/workflows/android-release.yml
git commit -m "ci: refine release workflow" || true
```

---

## Task 14: README

**Files:**
- Create: `README.md`

- [ ] **Step 1: 写 `README.md`**

```markdown
# 凝稳 · INR 记录

一款简单实用的安卓 INR 记录 App：记录、趋势、达标判断、到期提醒、CSV 导出。单用户、纯本地、离线、无账号、无广告。

## 下载安装
打开 [Releases](https://github.com/JsunDmer/ningwen/releases)，下载最新 `app-release.apk`，
在手机设置里允许“安装未知来源应用”后安装。

## 构建（云端）
推 `v*` tag 即触发 GitHub Actions 构建 release 签名 APK 并发布到 Releases。
签名密钥存于仓库 Secrets（`KEYSTORE_BASE64/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD`）。

## 开发
- 技术栈：Kotlin + Jetpack Compose + Room + DataStore + WorkManager
- 结构见 `docs/superpowers/specs/2026-10-09-ningwen-android-app-design.md`
- 原型见 `prototype/`

> 本工具仅用于记录，不构成医疗建议。
```

- [ ] **Step 2: 提交**

```bash
git add README.md
git commit -m "docs: add readme"
git push
```

---

## Self-Review（写计划后自查）

**1. Spec 覆盖**：技术栈/版本→Task1；领域规则→Task2；数据层→Task3；主题→Task4；提醒→Task5、Task12；UI 骨架→Task6；页面（首页/记录/趋势/设置/导出）→Task7–11；CI/CD+签名+Release→Task13；README→Task14。产品 spec 的"首页倒计时/达标判断/趋势目标带/导出/空状态/无云"均有对应任务。**无遗漏**。

**2. 占位符扫描**：无 "TBD/TODO/后续补充"；每个步骤含实际代码与命令。

**3. 类型/命名一致性**：`Record(value, measuredAt, doseTabs, note)`；`Settings(targetMin,targetMax,testIntervalDays,reminderEnabled)`；`InrRules.judge/nextTestMillis/daysUntilDue/dueBucket`；`Repository.addRecord/deleteRecord/latestRecord/setTargetRange/setInterval/setReminderEnabled`；`NingwenViewModel.add/delete/updateTargetRange/updateInterval/updateReminder` —— 各任务间一致。

**4. 已修正**：移除 Task9 多餘 import；Task8 补 `MaterialTheme` import。


