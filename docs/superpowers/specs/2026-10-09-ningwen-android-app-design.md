# 凝稳 · 安卓 App 技术设计（Implementation Spec）

> 日期：2026-10-09
> 状态：待用户评审
> 上游：[2026-10-09-inr-tracker-app-design.md](./2026-10-09-inr-tracker-app-design.md)（产品/交互设计，已定稿）+ `prototype/`（HTML 高保真原型）
> 本 spec 只讲"怎么实现 + 怎么用 GitHub 部署"，产品行为一律以上游设计为准。

---

## 1. 目标与范围

**目标**：交付一款可安装的安卓 App「凝稳」，功能与现有 HTML 原型一致，通过 GitHub Actions 云端构建并发布 Release APK。

**范围内**
- 单用户、纯本地、离线优先、无账号、无广告、无云同步。
- 首页概览、记录（新增/删除）、趋势折线图、设置、CSV 导出/分享。
- 到期提醒（提前 1 天 + 当天，各一次）。
- GitHub Actions 自动出 release 签名 APK，挂到 GitHub Releases。

**范围外（YAGNI）**
- iOS、多用户、云同步、登录、用药剂量调整建议、第三方库图表、Play 商店上架。

---

## 2. 技术栈与版本（全部锁版本，保证 CI 可复现）

| 项 | 选型 |
|---|---|
| 语言 | Kotlin 2.0.20 |
| UI | Jetpack Compose（Material 3，Compose BOM 2024.09.02） |
| Compose 编译器 | `org.jetbrains.kotlin.plugin.compose` 2.0.20 |
| 构建 | Gradle 8.7（wrapper）+ AGP 8.5.2 |
| JDK | 17（本地与 CI 一致，Actions 用 temurin 17） |
| 数据 | Room 2.6.1（KSP 2.0.20-1.0.25） |
| 设置 | DataStore(Preferences) 1.1.1 |
| 提醒 | WorkManager 2.9.1 |
| 序列化 | kotlinx-serialization-json 1.7.1（导出） |
| 其它 | activity-compose 1.9.2、lifecycle-runtime-compose 2.8.6、navigation 不引入（页面切换用状态机） |
| SDK | compileSdk 34 / minSdk 26 / targetSdk 34 |

> 版本组合为已知兼容集；不引第三方图表库，折线图用 Compose `Canvas` 手绘。

---

## 3. 项目结构

仓库根即安卓工程（单模块）。现有 `prototype/` 与 `docs/` 一并入库做参考。

```
ningwen/                              # 仓库根，remote = JsunDmer/ningwen
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jsundmer/ningwen/
│       │   ├── MainActivity.kt        # 单 Activity + Compose，页面状态切换
│       │   ├── NingwenApp.kt          # Application：初始化通知渠道 / 调度提醒
│       │   ├── data/
│       │   │   ├── Record.kt          # @Entity
│       │   │   ├── RecordDao.kt       # 增/删/查（按时间倒序）
│       │   │   ├── NingwenDatabase.kt # RoomDatabase
│       │   │   ├── SettingsStore.kt   # DataStore：targetMin/Max, interval, reminderEnabled
│       │   │   └── Repository.kt      # 统一数据入口（Flow）
│       │   ├── domain/
│       │   │   └── InrRules.kt        # judge()/status、nextTestDate()、stats —— 纯函数，可单测
│       │   ├── ui/
│       │   │   ├── App.kt             # Scaffold + 底部 Tab + FAB + 弹层
│       │   │   ├── home/HomeScreen.kt
│       │   │   ├── record/RecordSheet.kt
│       │   │   ├── trend/TrendScreen.kt   # Canvas 折线 + 目标带 + 范围切换
│       │   │   ├── settings/SettingsScreen.kt
│       │   │   ├── export/ExportSheet.kt  # CSV + 系统分享
│       │   │   └── theme/             # Color/Type/Theme（沿用原型配色）
│       │   └── notify/
│       │       ├── Notifications.kt   # 渠道 + 发通知
│       │       └── ReminderWorker.kt  # 每日本地检查到期
│       └── res/                       # strings.xml(zh)、图标、themes、FileProvider paths
├── gradle/
│   ├── wrapper/gradle-wrapper.properties
│   └── libs.versions.toml             # 版本集中管理
├── .github/workflows/android-release.yml
├── .gitignore
├── settings.gradle.kts
├── build.gradle.kts
├── README.md                          # 构建/下载/安装说明
├── prototype/                         # 现有 HTML 原型（参考）
└── docs/superpowers/                  # 设计与计划
```

---

## 4. 数据模型

```kotlin
@Entity(tableName = "records")
data class Record(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val value: Double,          // INR
  val measuredAt: Long,       // epoch millis
  val doseTabs: Double?,      // 华法林用量（片），可选，步进 0.25
  val note: String?           // 备注，可选
)
```

设置（DataStore Preferences）
| 键 | 默认 | 说明 |
|---|---|---|
| targetMin | 2.0 | 目标下限 |
| targetMax | 3.0 | 目标上限 |
| testIntervalDays | 7 | 下次测量间隔（天，用户填写） |
| reminderEnabled | true | 提醒开关 |

沿用上游设计：达标判断 `<min 偏低 / 区间内 达标 / >max 偏高`；首次启动无数据时插入 1 条示例可选（默认**不插**，用空状态引导）。

---

## 5. 页面与交互

与原型 1:1，底部 3 Tab（首页 / 趋势 / 我的）+ 右下 FAB。

- **首页**：hero 状态卡（大号 INR、达标徽标、较上次变化、日期、`距下次测量还有 N 天`）+ 最近记录列表（点行删除需二次确认）。
- **记录弹层**：INR（大号数字，1 位小数，范围 0.5–5.0 校验）、时间（默认现在，可选）、用量(片，带出上次值，步进 0.25)、快捷标签 + 备注、保存/取消。
- **趋势**：Canvas 折线，目标带绿色区间，范围切换（90 天 / 全部），顶部统计（平均/最高/最低/达标率）。
- **设置**：目标范围、下次测量间隔（天）、测量提醒开关、导出/分享、隐私与免责说明。
- **导出**：生成 CSV（`日期,INR,用量(片),备注`），经 FileProvider 走系统分享；无权限弹窗问题。

空状态、加载态、输入错误提示齐全，配色与原型一致（蓝色主色 + 达标绿/偏高橙/偏低红）。

---

## 6. 提醒机制

- 到期日 = `最后一条记录.measuredAt + testIntervalDays 天`。
- `ReminderWorker`（WorkManager，每日一次，约 9:00）检查：
  - `daysUntilDue == 1` → 通知「明天该测 INR 了」；
  - `daysUntilDue == 0` → 通知「今天该测 INR 了」。
- 使用**通知渠道**（API 26+），`POST_NOTIFICATIONS` 运行时权限（API 33+）在首次进入时请求；用户拒绝则仅 App 内倒计时。
- `reminderEnabled=false` 时不发。
- 采用**非精确**调度（WorkManager 天然非精确），避免精确闹钟权限；对"隔天提醒"场景足够。
- App 启动 / 数据变更 / 设置变更后重排 worker，保证基于最新记录。

---

## 7. CI/CD（GitHub 部署）

**签名密钥**（一次性，本地 `keytool` 生成）：
```
keytool -genkeypair -v -keystore ningwen.jks -alias ningwen \
  -keyalg RSA -keysize 2048 -validity 10000
```
- `ningwen.jks` 转 base64 → 仓库 Secret `KEYSTORE_BASE64`
- 另设 Secrets：`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`
- 本地保留一份 `ningwen.jks` 备份（丢了就无法覆盖升级）。

**Workflow `.github/workflows/android-release.yml`**
- 触发：push tag `v*`（正式发版）+ 手动 `workflow_dispatch`；PR/push 到 main 跑 `assembleDebug` 做冒烟。
- 步骤：checkout → setup JDK 17 → setup Gradle → 解码 keystore → `./gradlew assembleRelease` → 用 `softprops/action-gh-release`（或 `gh release`）创建 Release 并上传 APK。
- 产物命名：`ningwen-<tag>.apk`。

**用户侧**：打开 `https://github.com/JsunDmer/ningwen/releases` → 点最新 APK 下载 → 手机允许"未知来源"安装。升级直接覆盖安装。

---

## 8. 验证策略（本机无 Android SDK，全靠 CI）

- **能在本机做的**：Kotlin 语法/逻辑自审、`InrRules` 纯函数可本地用 `kotlinc`/单测思路验证（无 SDK 时至少人工推演 + 后续 CI 单测）、资源与 Gradle 文件静态检查。
- **必须在 CI 做的**：编译、单测、`assembleRelease`、生成 APK。
- **验收标准**：CI 绿 + 产出可安装 APK + 手机上能跑通"记录→首页倒计时→趋势→导出"。
- **预期**：首个能出包的版本可能要 1~2 轮 CI 迭代修编译问题，属正常。

---

## 9. 已确认决策

1. 路线：**原生安卓（Kotlin + Compose）+ GitHub Actions 出 APK**。
2. 仓库：**public**，`JsunDmer/ningwen`；包名 `com.jsundmer.ningwen`，App 名 凝稳。
3. minSdk **26**（Android 8+）。
4. 数据存储：**Room**（记录）+ DataStore（设置）。
5. 提醒：**到期前 1 天 + 当天，各提醒一次**。
6. 分发：**Release 签名 APK，自动发 GitHub Release**。
7. 原型与文档随仓库一起入库做参考。

---

## 10. 待办确认（评审后关闭）

- 首次启动是否插入示例数据？（默认：不插，走空状态引导）
- 记录是否允许"编辑"？（默认：MVP 只支持新增 + 删除，编辑列为后续）
