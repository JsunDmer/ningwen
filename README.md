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
