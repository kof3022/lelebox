# 乐龄游戏盒 LeLeBox

[![Android CI](https://github.com/kof3022/lelebox/actions/workflows/android-build.yml/badge.svg?branch=main)](https://github.com/kof3022/lelebox/actions/workflows/android-build.yml)
[![Release](https://img.shields.io/github/v/release/kof3022/lelebox?include_prereleases&label=release&color=3D6B52)](https://github.com/kof3022/lelebox/releases)
[![License](https://img.shields.io/badge/license-GPL--3.0--or--later-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-7.0%2B%20%C2%B7%20API%2024-3D6B52.svg)](#技术栈)
[![Version](https://img.shields.io/badge/version-0.5.7--m7-orange.svg)](#版本与发布)
[![ads / IAP](https://img.shields.io/badge/ads%20%2F%20IAP-none-brightgreen.svg)](#产品铁律)

> 给爸妈、爷爷奶奶的一台「打开即玩」的游戏盒子：**零权限 · 无账号 · 无广告 · 无收费 · 大字大按钮 · 完全离线**。

## 这是什么

乐龄游戏盒（LeLeBox）是一个完全离线的 Android 单机游戏合集，专门为老年人重新设计：装好就能玩，不注册、不登录、不联网——Manifest 里连 `INTERNET` 权限都没有。

- **首页**：9 个游戏的大图标宫格，点进去直接开局；每个游戏右上角都有「玩法」大字说明。
- **看得清**：全局字号三档（小 / 标准 / 大），正文基准 20sp，触控目标 ≥ 64dp。
- **不添乱**：没有体力、倒计时、排行榜、内购、广告 SDK；进度只存本机，卸载即清空。
- **能用读屏**：原生界面全部支持 TalkBack，关键控件都带 `contentDescription`。

当前版本 **v0.5.7-m7**（versionCode 46），内置 **9 款**游戏：

| 游戏 | 一句话玩法 | 难度档位 |
| :-- | :-- | :-- |
| 🃏 斗地主 | 三人斗地主，先出完手牌就赢，双 AI 会配合 | 简单 / 普通 / 困难 |
| 🀄 麻将 | 国标四人麻将，凑齐四副 + 一对将即胡，结算番数 | 碰 / 杠 / 番数结算 |
| 🀫 牌九 | 打天九（安徽阜阳玩法），8 张骨牌，赢下最后一墩得牌 | 简单 / 中等 / 困难 |
| 🎴 记忆翻牌 | 翻开两张图案相同的牌配对 | 6 对 / 8 对 |
| 🔢 2048 | 滑动合并数字，合到 2048 | 大方向按钮 |
| 🍎 连连看 | 两张相同图案、最多拐两个弯连通即可消 | 全消过关 |
| ⚫ 五子棋 | 先连成五个就赢（玩家执黑先手） | 单一难度 |
| 🧩 数独 | 每行每列每宫 1–9 不重复，可「做记号」记候选数 | 简单 45 / 中等 36 / 困难 28 个提示 |
| 🔍 找不同 | 两幅图里找不同，点一下算找到 | 3 档 × 9 个场景（5–7 处不同） |

## 产品铁律

1. **零授权**：不声明任何 Android 危险权限，连 `INTERNET` 都没有（绝对离线）。
2. **无账号**：无注册 / 登录 / 收集，进度只存本机。
3. **无广告**：不引入任何广告 SDK。
4. **无收费**：无内购、无订阅。
5. **打开即玩**：无引导、无弹窗、无强制更新。
6. **老年友好**：正文 ≥ 20sp、触控目标 ≥ 64dp、单击优先、无时间压力、TalkBack 可读。

## 技术栈

- Kotlin **2.1.20** + Jetpack Compose（Material 3，Compose BOM 2025.05.00）
- AGP **8.7.3** / Gradle **8.13** / JDK 17
- `minSdk 24`（Android 7.0）/ `targetSdk 35` / `compileSdk 35`
- 单 `MainActivity` + Compose 状态切换（不引入导航库）；游戏由 `GameShellActivity` 承载，分 L1 原生、L2 离线 H5 两档
- 存档 `SharedPreferences`（`game_saves`）、设置（`lelebox_settings`）；音效 `SoundPool`
- 依赖只有 AndroidX + Compose + JUnit4，**无任何第三方 SDK**

## 构建

```bash
# 需要 JDK 17 + Android SDK（ANDROID_HOME，或 android/local.properties 里的 sdk.dir）
cd android
./gradlew assembleDebug        # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # R8 混淆 + 资源压缩；配置签名后产出 app-release.apk
./gradlew testDebugUnitTest    # 134 个单元测试（纯 JVM，不依赖 Android 框架）
```

签名凭据从 `android/local.properties` 读取（`lelebox.keystore.*`，已 gitignore）；未配置时 release 仍可构建，只是产出未签名包。

### 测试

`android/app/src/test/` 共 13 个测试类、**134** 个用例，覆盖 9 款游戏的核心规则：

> 牌九规则 30 · 牌九对局 22 · 斗地主 19 · 麻将 13 · 连连看 10 · 牌九 AI 10 · 2048 9 · 五子棋 9 · 找不同 6 · 数独 4 · 记忆翻牌 2

### 本机构建的几个坑（Windows）

1. 仓库目录含中文（`乐龄游戏盒`），构建已加 `android.overridePathCheck=true`。
2. **单元测试**的测试 JVM 在中文路径下会报 `ClassNotFoundException`（Gradle 工作进程 classpath 编码问题），用 ASCII 目录联接规避：

   ```powershell
   New-Item -ItemType Junction -Path C:\lelebox-build -Target <本仓库>\android
   cd C:\lelebox-build; .\gradlew.bat testDebugUnitTest
   ```

3. `lint` 当前不可用：AGP 8.7.x 内置 lint 与 Kotlin 2.1 UAST 存在已知崩溃（`NonNullableMutableLiveDataDetector`），本地与 CI 均已禁用，待 AGP 升级后恢复。
4. 中国大陆网络：直连 GitHub 可能失败，拉取 GitHub 资源可加镜像前缀 `https://gh-proxy.com/`，Gradle 依赖建议走阿里云镜像（见 `~/.gradle/init.gradle`），Gradle 发行版走 `services.gradle.org`（可达）。

## 目录结构

```text
乐龄游戏盒/
├── android/                             # Android 工程（Kotlin + Jetpack Compose）
│   └── app/src/
│       ├── main/kotlin/com/lelebox/app/
│       │   ├── MainActivity.kt            # 首页宫格 + 设置
│       │   ├── ui/                        # ElderTheme / ElderComponents / HomeScreen / SettingsScreen
│       │   ├── game/                      # 9 款游戏：逻辑（*Game.kt）+ 界面（*Screen.kt）+ 注册表 Games.kt
│       │   ├── game/GameShellActivity.kt  # 游戏壳（原生 / 离线 H5）
│       │   └── audio/Sfx.kt               # 音效引擎
│       └── test/kotlin/com/lelebox/app/   # 134 个单元测试
├── .github/                             # CI（android-build.yml）+ Issue / PR 模板
├── CONTRIBUTING.md                      # 贡献指南（适老硬指标、协议登记、提交规范）
├── SECURITY.md                          # 安全策略与漏洞报告
├── THIRD_PARTY_NOTICES.md               # 第三方开源代码与素材署名
└── LICENSE                              # GPL-3.0 全文
```

## 里程碑

| 阶段 | 内容 | 状态 |
| :-- | :-- | :-- |
| M0 立项 | 规划定稿、协议审计、仓库与 CI 骨架、Compose 外壳 | ✅ 完成 |
| M1 MVP | 外壳 + 2048 / 数独 / 记忆翻牌（纸牌接龙已于 v0.2.2 移除）+ 统一存档 | ✅ 完成（模拟器 + 真机验证） |
| M2 扩容 | 斗地主 / 五子棋 / 连连看 / 找不同，追加麻将 / 牌九 | ✅ 9 款游戏全部上线，134 个单测通过 |
| M3 发布 | 签名发布、GitHub Release、各渠道上架（Google Play / F-Droid / 酷安） | 🚧 进行中：上架与收费方案已成文，签名与正式发布待办 |

## 版本与发布

- **最新发布：[`v0.5.7-m7`](https://github.com/kof3022/lelebox/releases/tag/v0.5.7-m7)**（内测版）· 下载 [`LeLeBox-v0.5.7-m7.apk`](https://github.com/kof3022/lelebox/releases/download/v0.5.7-m7/LeLeBox-v0.5.7-m7.apk)（16.67 MB，已签名）
  - 包名 `com.lelebox.app`，versionCode `46`，Android 7.0+（API 24），ABI：arm64-v8a / armeabi-v7a / x86 / x86_64
  - 与既往内测包同一签名证书（SHA-256 `6d0e3c5a…54cc`），可直接覆盖安装并保留存档
  - APK SHA-256：`2c3435a0901c921518834875cb05fa64c2c1db3c70b6bc86efb82cd60a463f5e`
- 当前 `versionName = 0.5.7-m7`、`versionCode = 46`（见 [`android/app/build.gradle.kts`](android/app/build.gradle.kts)）；设置页显示的版本号由 `PackageManager` 实时读取，不会与构建配置脱节。
- **仓库不包含构建产物**：APK 只挂 GitHub Releases，不进 git 历史（`.gitignore` 已挡掉 `内测包/` 与 `*.apk`）。
- 后续正式版（去内测标记、上架 F-Droid / 酷安 / Google Play）随 M3 推进。

## 零权限说明（透明声明）

- Manifest 中**无任何 `<uses-permission>`**，包括 `INTERNET`（完全离线）。
- APK 内仅有一个 AndroidX 自动生成的 **signature 级内部权限** `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（AndroidX Core 标准行为：仅用于保护库内部动态广播接收器，**不向用户请求、不出现在系统权限列表**，任何 AndroidX 应用都有，无法也不需移除）。
- 不申请：存储、定位、相机、麦克风、电话、通讯录等一切用户可见权限。
- 无崩溃上报、无统计埋点、无网络请求；`allowBackup=false`，卸载即清空。

## 已知限制

- `lint` 暂时禁用（AGP 8.7.x + Kotlin 2.1 UAST 上游崩溃），CI 目前只跑构建与单测。
- 仅中文界面；L2 离线 H5 接入层已预留，目前 9 款游戏均为 Compose 原生实现。
- 牌九采用安徽阜阳「天九」地方规则，其他地区玩法尚未覆盖。
- 麻将、牌九为四人对局，界面按横屏桌面布局优化。

## 参与与反馈

- **提 PR 前请读 [CONTRIBUTING.md](CONTRIBUTING.md)**：本项目不接受新增权限、联网、广告、统计或第三方 SDK；界面改动须满足字号 ≥ 20sp、触控目标 ≥ 64dp、对比度达标等硬指标；新增第三方素材须在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 登记来源与协议。
- **Bug 与建议**：走 [Issues](https://github.com/kof3022/lelebox/issues)（已配问题模板，请填机型与复现步骤）。
- **安全问题**：请勿开公开 Issue，按 [SECURITY.md](SECURITY.md) 走 GitHub 私有漏洞报告。
- 本项目按 GPL-3.0-or-later 分发，衍生作品需同样开源。

## 版权与许可

Copyright (C) 2025 LeLeBox contributors

本项目整体以 **GPL-3.0-or-later** 发布，完整许可文本见 [LICENSE](LICENSE)（GNU 官方原文全文）。选用 GPL 系许可是为了兼容接入的 GPL 系开源游戏资源；MIT / Apache-2.0 / BSD / CC0 的第三方代码与素材可并入，但须在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 中保留其原始版权与许可声明。SPDX 标识：`GPL-3.0-or-later`。
