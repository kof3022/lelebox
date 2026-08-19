# 乐龄游戏盒 LeLeBox

> 给爸妈/爷爷奶奶的一台「打开即玩」的游戏盒子：**零授权 · 无账号 · 无广告 · 无收费 · 大字大按钮 · 完全离线**。

## 产品铁律

1. 零授权：不声明任何 Android 危险权限，连 `INTERNET` 都没有（绝对离线）。
2. 无账号：无注册/登录/收集，进度只存本机。
3. 无广告：不引入任何广告 SDK。
4. 无收费：无内购、无订阅。
5. 打开即玩：无引导、无弹窗、无强制更新。
6. 老年友好：正文 ≥ 20sp、触控目标 ≥ 64dp、单击优先、无时间压力、TalkBack 可读。

完整规划见 [APP开发规划.md](APP开发规划.md)。

## 目录结构

```
乐龄游戏盒/
├── APP开发规划.md            # 总体规划（产品/架构/里程碑/风险）
├── docs/
│   ├── 01-游戏清单与验收标准.md
│   ├── 02-协议审计表.md
│   └── 03-老年友好设计规范.md
├── THIRD_PARTY_NOTICES.md   # 第三方开源资源清单（协议合规）
├── android/                 # Android 工程（Kotlin + Jetpack Compose）
│   └── app/src/main/assets/games/   # 离线 H5 游戏（L2 层）
└── .github/workflows/       # CI 骨架（Android 构建）
```

## 技术栈

- Kotlin 2.1 + Jetpack Compose（Material 3）+ AGP 8.7 / Gradle 8.13
- `minSdk 24` / `targetSdk 35`，**Manifest 零权限**
- 游戏三档接入：L1 原生（Compose）· L2 离线 H5（WebView 内嵌 assets，断网加固）· L3 自研（Godot，第二阶段）

## 构建

```bash
# 需要 JDK 17 + Android SDK（ANDROID_HOME）
cd android
./gradlew assembleDebug        # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # 发布版（含混淆瘦身）
```

> 中国大陆网络提示：直连 GitHub 可能失败。下载 Gradle 发行版用 `services.gradle.org`（可达）；拉取 GitHub 资源走镜像前缀 `https://gh-proxy.com/https://github.com/...`；Gradle 依赖走 `google()`（dl.google.com）与 `mavenCentral()`（均可达）。

## 里程碑

| 阶段 | 内容 | 状态 |
|------|------|------|
| M0 | 立项：规划定稿、协议审计、仓库/CI 骨架、外壳脚手架 | ✅ 进行中 |
| M1 | MVP：外壳 + 纸牌接龙(H5) + 2048/数独/记忆翻牌(原生) | ⏳ 待开始 |
| M2 | 扩容至 10 款 + 超大字体/高对比度 + 无障碍走查 | ⏳ |
| M3 | 打磨与发布（开源 + F-Droid/酷安/官网 APK） | ⏳ |

## 开源承诺

本项目完全开源、永久免费。第三方代码与素材按协议在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 逐一登记，审计规则见 [docs/02-协议审计表.md](docs/02-协议审计表.md)。
