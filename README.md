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
./gradlew assembleRelease      # 发布版（含混淆瘦身，配置签名后产出 app-release.apk）
./gradlew testDebugUnitTest    # 游戏逻辑单元测试（2048/数独/记忆翻牌，15 个用例）
```

> **Windows 中文路径提示**：
> 1. 仓库目录含中文（乐龄游戏盒），构建已加 `android.overridePathCheck=true`。
> 2. **单元测试**的测试 JVM 在中文路径下会报 `ClassNotFoundException`（Gradle 工作进程 classpath 编码问题），
>    用 ASCII 目录联接规避：
>    ```powershell
>    New-Item -ItemType Junction -Path C:\lelebox-android -Target <本仓库>\android
>    cd C:\lelebox-android; .\gradlew.bat testDebugUnitTest
>    ```
> 3. `lint` 当前不可用：AGP 8.7.x 内置 lint 与 Kotlin 2.1 UAST 存在已知崩溃（NonNullableMutableLiveDataDetector），
>    本地与 CI 均已禁用（见 CI 工作流注释）；待 AGP 升级后恢复。
>
> 中国大陆网络提示：直连 GitHub 可能失败。下载 Gradle 发行版用 `services.gradle.org`（可达）；拉取 GitHub 资源走镜像前缀 `https://gh-proxy.com/https://github.com/...`；Gradle 依赖走 `google()`（dl.google.com）与 `mavenCentral()`（均可达）。

## 里程碑

| 阶段 | 内容 | 状态 |
|------|------|------|
| M0 | 立项：规划定稿、协议审计、仓库/CI 骨架、外壳脚手架（本地编译通过） | ✅ 完成 |
| M1 | MVP：4 款第一批游戏（2048/数独/记忆翻牌原生 + 纸牌接龙自研 H5）+ 游戏壳帮助/设置联动 + 统一存档 | ✅ 功能完成（模拟器冒烟通过；真机/试玩见 docs/01、docs/06 §3） |
| M2 | 扩容至 10 款 + 超大字体/高对比度 + 无障碍实机走查 | ⏳ |
| M3 | 打磨与发布（开源 + F-Droid/酷安/官网 APK） | ⏳ |

## 版本发布

- M1 版本：`v0.2.0-m1`（versionCode 2），GitHub Releases 草案（含 unsigned APK 附件）：https://github.com/kof3022/lelebox/releases
- 正式签名与各渠道上架在 M3 完成（见 [docs/04-代码仓库与发布指引.md](docs/04-代码仓库与发布指引.md)）。

## 零权限说明（透明声明）

- Manifest 中**无任何 `<uses-permission>`**，包括 INTERNET（完全离线）。
- APK 内仅有一个 AndroidX 自动生成的 **signature 级内部权限** `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（AndroidX Core 标准行为：仅用于保护库内部动态广播接收器，**不向用户请求、不出现在系统权限列表**，任何 AndroidX 应用都有，无法也不需移除）。
- 不申请：存储、定位、相机、麦克风、电话、通讯录等一切用户可见权限。

## 开源承诺

本项目完全开源、永久免费。第三方代码与素材按协议在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 逐一登记，审计规则见 [docs/02-协议审计表.md](docs/02-协议审计表.md)。
