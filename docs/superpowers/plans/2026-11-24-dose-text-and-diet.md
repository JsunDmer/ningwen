# 用量文本与饮食参考 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让华法林用量可以保存 `1.25/1.5` 这样的原文，并增加一栏查看可能降低或升高 INR 的饮食类型。

**Architecture:** 纯规则放在 `domain`，用 JVM 单测锁住。`doseTabs` 列名不变，类型改为 `String?`，Room 版本 2 用建新表的方式迁移。饮食是静态目录，由新的底部 Tab 展示。INR 数值、趋势和达标判断不改。

**Tech Stack:** Kotlin、Jetpack Compose、Room 2.6.1、JUnit 4。minSdk 26。

**Spec:** `docs/superpowers/specs/2026-11-24-dose-text-and-diet-design.md`

## Global Constraints

- INR 检测值保持 `Double`，合法范围 0.5–5.0，错误文案为「请输入 0.5–5.0 之间的数值」。
- 用量列名保持 `doseTabs`，类型改为 `String?`。空白存 `null`。
- 数据库 `version = 2`。迁移保留每一行的 id、INR、日期、备注。不要 `fallbackToDestructiveMigration`。
- 旧用量用最短普通小数：`1.0` → `1`，`1.25` → `1.25`。
- 有用量时界面显示「原文 + 空格 + 片」。
- CSV 表头保持 `日期,INR,用量(片),备注`；用量和备注中的 `,` 换成 `，`。
- 饮食声明使用 spec 中的原文。两组标题为「可能降低 INR」「可能升高 INR」。
- 不新增依赖，不改原型，不创建 git commit（除非用户明确要求）。

## Review Focus

- 已保存的 `1.0` 升级后应显示 `1 片`，不能变成 `1.0` 或科学计数法。Task 1 的 `formatLegacy` 覆盖。
- 用量 `1.25/1.5` 必须原样保存；按数字解析会变成空。Task 1 的 `normalize` 覆盖。
- 只输入空格的用量不能在列表里变成「片」。Task 1 的 `normalize` 覆盖。
- 用量里的英文逗号不能把 CSV 撑成多余的列。Task 1 的 `RecordCsv` 覆盖。
- 把 `1.25/1.5` 填进 INR 栏必须拒绝，趋势仍只接收单个数字。Task 1 的 `InrInput` 覆盖。

---

### Task 1: 用量、INR 与 CSV 规则

**Files:**
- Create: `app/src/test/java/com/jsundmer/ningwen/domain/DoseTextTest.kt`
- Create: `app/src/test/java/com/jsundmer/ningwen/domain/InrInputTest.kt`
- Create: `app/src/test/java/com/jsundmer/ningwen/domain/RecordCsvTest.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/domain/DoseText.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/domain/InrInput.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/domain/RecordCsv.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `DoseText.normalize(raw: String?): String?`
  - `DoseText.formatLegacy(dose: Double): String`
  - `InrInput.parse(raw: String): Double?`
  - `InrInput.ERROR: String`
  - `RecordCsv.HEADER: String`
  - `RecordCsv.line(date: String, value: Double, dose: String?, note: String?): String`

- [ ] **Step 1: Write the failing tests**

`DoseTextTest` 断言：`normalize("  1.25/1.5  ") == "1.25/1.5"`，`normalize("1.5") == "1.5"`，`normalize("   ") == null`，`normalize("") == null`，`normalize(null) == null`，`formatLegacy(1.25) == "1.25"`，`formatLegacy(1.5) == "1.5"`，`formatLegacy(1.0) == "1"`，`formatLegacy(0.25) == "0.25"`，`formatLegacy(2.0) == "2"`。

`InrInputTest` 断言：`parse("2.4")` 为 2.4，`parse(" 2.5 ")` 为 2.5，`parse("0.5")` 为 0.5，`parse("5.0")` 为 5.0；`parse("1.25/1.5")`、`parse("0.4")`、`parse("5.1")`、`parse("abc")`、`parse("")` 都是 null。`ERROR` 等于「请输入 0.5–5.0 之间的数值」。

`RecordCsvTest` 断言：`HEADER` 为 `日期,INR,用量(片),备注`；`line("2026-11-24", 2.4, "1.25/1.5", "饮食变化")` 为 `2026-11-24,2.4,1.25/1.5,饮食变化`；`line("2026-11-24", 2.4, "1.25,1.5", "a,b")` 为 `2026-11-24,2.4,1.25，1.5,a，b`；`line("2026-11-24", 2.4, null, null)` 为 `2026-11-24,2.4,,`。

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests com.jsundmer.ningwen.domain.DoseTextTest --tests com.jsundmer.ningwen.domain.InrInputTest --tests com.jsundmer.ningwen.domain.RecordCsvTest`

Expected: 编译失败，找不到 `DoseText`、`InrInput` 或 `RecordCsv`。

- [ ] **Step 3: Implement the three domain objects**

`normalize`：`trim` 后空则 null。`formatLegacy`：`BigDecimal(dose.toString()).stripTrailingZeros().toPlainString()`。`parse`：`trim` 后 `toDoubleOrNull`，闭区间 [0.5, 5.0] 才返回。`line`：dose 与 note 的 null 当空串，并把 `,` 换成 `，`，按 `日期,INR,用量,备注` 拼接。

- [ ] **Step 4: Run tests to verify they pass**

同一条命令。Expected: BUILD SUCCESSFUL，上述测试全部通过。

### Task 2: 把用量存成文本并迁移旧数据

**Files:**
- Modify: `app/src/main/java/com/jsundmer/ningwen/data/Record.kt`
- Modify: `app/src/main/java/com/jsundmer/ningwen/data/NingwenDatabase.kt`
- Modify: `app/src/main/java/com/jsundmer/ningwen/ui/NingwenViewModel.kt`
- Modify: `app/src/main/java/com/jsundmer/ningwen/ui/App.kt`
- Modify: `app/src/main/java/com/jsundmer/ningwen/ui/record/RecordSheet.kt`
- Modify: `app/src/main/java/com/jsundmer/ningwen/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/jsundmer/ningwen/ui/export/ExportSheet.kt`

**Interfaces:**
- Consumes: Task 1 的 `DoseText`、`InrInput`、`RecordCsv`
- Produces: `Record.doseTabs: String?`；`NingwenViewModel.add(value: Double, measuredAt: Long, doseTabs: String?, note: String?)`；`RecordSheet` 的 `lastDose: String?` 与同签名的 `onSubmit`

- [ ] **Step 1: 确认没有新的纯逻辑需要另写测试**

本任务只接线。用量空格、旧数字格式、CSV 逗号、INR 斜杠都已由 Task 1 锁住。不要加会清空数据库的 fallback。

- [ ] **Step 2: 改存储与调用链**

`Record.doseTabs` 改为 `String?`。数据库版本改为 2。`MIGRATION_1_2`：创建列顺序为 id、value、measuredAt、doseTabs TEXT、note 的 `records_new`；逐行拷贝；非空旧用量经 `DoseText.formatLegacy`；然后 `DROP TABLE records`，`ALTER TABLE records_new RENAME TO records`；最后把 `sqlite_sequence` 里 `records` 的 seq 设为当前最大 id，避免下次自增主键撞车。

`add` 与 `RecordSheet.onSubmit` 的用量参数改为 `String?`。用量输入使用 `KeyboardType.Text`，辅助说明「可填 1.5，或隔日交替 1.25/1.5」。保存时 INR 用 `InrInput.parse`，失败显示 `InrInput.ERROR`；用量用 `DoseText.normalize`。上次用量按字符串预填。

首页有用量时显示 `"$dose 片"`。导出用 `RecordCsv.HEADER` 和 `RecordCsv.line`。

- [ ] **Step 3: 编译**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL。

### Task 3: 饮食目录

**Files:**
- Create: `app/src/test/java/com/jsundmer/ningwen/domain/DietCatalogTest.kt`
- Create: `app/src/main/java/com/jsundmer/ningwen/domain/DietCatalog.kt`

**Interfaces:**
- Consumes: 无
- Produces:
  - `enum class InrDietEffect { LOWER, RAISE }`
  - `data class DietItem(val name: String, val detail: String)`
  - `data class DietGroup(val effect: InrDietEffect, val title: String, val summary: String, val items: List<DietItem>)`
  - `DietCatalog.DISCLAIMER: String`
  - `DietCatalog.groups: List<DietGroup>`

- [ ] **Step 1: Write the failing test**

断言两组顺序为 LOWER 然后 RAISE；标题为「可能降低 INR」「可能升高 INR」；摘要分别为「维生素 K 较多，可能减弱华法林作用」和「可能增强华法林作用，出血风险增加」。降低组名字依次为绿叶菜、十字花科、纳豆、动物肝脏、大量绿茶。升高组名字依次为西柚、蔓越莓、芒果、大量饮酒、「大蒜、生姜、鱼油、银杏」。声明等于 spec 原文。每个 detail 非空，名字不重复。

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.jsundmer.ningwen.domain.DietCatalogTest`

Expected: 编译失败，找不到 `DietCatalog`。

- [ ] **Step 3: Implement `DietCatalog`**

按 spec 写入声明和两组。detail 用一句话说明该类食物，不写剂量建议。

- [ ] **Step 4: Run test to verify it passes**

同一条命令。Expected: BUILD SUCCESSFUL。

### Task 4: 饮食页

**Files:**
- Create: `app/src/main/java/com/jsundmer/ningwen/ui/diet/DietScreen.kt`
- Modify: `app/src/main/java/com/jsundmer/ningwen/ui/App.kt`

**Interfaces:**
- Consumes: `DietCatalog.DISCLAIMER`、`DietCatalog.groups`、`InrDietEffect`
- Produces: `DietScreen()`；`Tab.DIET`，标签「饮食」

- [ ] **Step 1: 确认目录测试仍是行为来源**

页面只展示 `DietCatalog`，不复制第二份文案。没有记录时也能打开。

- [ ] **Step 2: 实现页面并接入底部栏**

`Tab` 顺序为首页、趋势、饮食、我的。饮食图标用 `Icons.Filled.Restaurant`。页面自上而下：声明卡片，然后每组一个标题、一行摘要、每条食物一张卡片（名字加粗，下面是 detail）。降低组用现有 `Bad` 色，升高组用现有 `Warn` 色。边距和圆角对齐首页。

- [ ] **Step 3: 编译并跑全部单元测试**

Run: `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL。
