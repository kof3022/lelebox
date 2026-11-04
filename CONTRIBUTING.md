# 参与贡献

先谢谢你愿意花时间。这个项目的目标很窄：**让六七十岁的人第一次打开就能玩明白**。所有改动都请以这条为准绳。

## 不接受的方向

下面这些改动不会合并，也请不要为此提 PR（有想法请先开 Issue 说明场景）：

- 增加任何 `uses-permission`（尤其是 `INTERNET`）、后台服务、开机自启、悬浮窗
- 接入广告、内购、订阅、统计埋点、崩溃上报、账号/登录体系
- 强制联网、云端存档、强制更新弹窗、评分弹窗
- 引入体积大或权限重的第三方 SDK（当前依赖只有 AndroidX + Compose + JUnit4，请保持）

## 环境准备

- **JDK 17**、**Android SDK**（`compileSdk 35`、`build-tools 35`）
- `git clone` 后把 SDK 路径写进 `android/local.properties`：

  ```properties
  sdk.dir=C\:\\path\\to\\Android\\Sdk
  ```

  该文件已在 `.gitignore` 中，**不要提交**。
- 需要打签名包时，另外在同一个文件里配置 `lelebox.keystore.path / pass / key.alias / key.pass`（同样不要提交；没有这些配置时 `assembleRelease` 产出未签名包）。

## 构建与测试

```bash
cd android
./gradlew assembleDebug        # 调试包 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 134 个单元测试（改游戏规则时必须跑）
```

**Windows 中文路径的坑**：仓库目录名是中文（`乐龄游戏盒`），单元测试的测试 JVM 会抛 `ClassNotFoundException`（Gradle 工作进程的 classpath 编码问题）。用 ASCII 目录联接绕过：

```powershell
New-Item -ItemType Junction -Path C:\lelebox-build -Target <本仓库路径>\android
cd C:\lelebox-build; .\gradlew.bat testDebugUnitTest
```

`lint` 目前是禁用状态（AGP 8.7.x 内置 lint 与 Kotlin 2.1 UAST 存在上游崩溃），不用为此提 PR，等 AGP 升级后一起恢复。

## 老年友好是硬指标

改 UI 前请先读 [docs/03-老年友好设计规范.md](docs/03-老年友好设计规范.md)。最低要求：

| 项目 | 要求 |
| --- | --- |
| 正文字号 | ≥ 20sp，且必须随「设置 → 文字大小」三档（小/标准/大）一起缩放 |
| 触控目标 | ≥ 64dp，相邻元素间距 ≥ 8dp |
| 操作方式 | 单击优先；手势可以用，但**必须同时有按钮替代**（不要求滑动精度、不要求长按、不要求双击） |
| 时间压力 | 不设倒计时、不设步数惩罚、不设体力 |
| 读屏 | 可点击元素都要有可读文本或 `contentDescription`，TalkBack 能独立走完一局 |
| 文案 | 用老人听得懂的话：「点这张牌」而不是「点击此处以选择牌面」 |

## 素材与协议

1. 新增第三方代码或素材，**必须**在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 登记：来源链接、协议、用途、做了哪些改动。
2. 协议必须明确可查（MIT / Apache-2.0 / BSD / CC0 / CC BY / CC BY-SA / 与本项目 GPL-3.0 兼容）。
   **没有 LICENSE 文件的仓库一律视为「保留所有权利」，不接受直接接入**，除非作者书面授权，并把授权记录写进 notices。
3. 审计判定规则见 [docs/02-协议审计表.md](docs/02-协议审计表.md)。
4. 图片素材请压缩到实际显示尺寸后再提交（首页图标 160 / 216 px 足够，别提交 2048 px 原图，仓库已经 90 MB+）。

## 代码风格

- Kotlin + Compose，跟随现有文件写法；**不新增依赖**，不引入 DI 框架、导航库、图片库
- 游戏逻辑（`*Game.kt` / `*Rules.kt`）必须与界面（`*Screen.kt`）分离，逻辑要能在纯 JVM 下被单测覆盖；改规则的请同步补测试用例
- 界面文字直接写中文（项目暂不做 i18n）
- 注释解释「为什么」，不复述「做了什么」

## 提交与 PR

- 提交信息用中文，动宾结构、说清范围，例：`牌九UI：选中牌上浮+粗金框+▲提示`、`麻将修复：中发白显示成风的映射错误`
- 一个提交只做一件事，重构与功能改动分开
- PR 标题写明模块；描述请按模板填：改了什么、为什么、怎么验证（跑了哪些测试、哪台设备/模拟器、系统版本）
- 提 PR 前请确认 `./gradlew testDebugUnitTest` 全绿（本地或 CI 均可）

## 报告问题

- Bug 与功能建议走 [Issues](https://github.com/kof3022/lelebox/issues)（有模板，请按模板填机型和复现步骤）
- 涉及权限、数据外泄、供应链的问题**不要开公开 Issue**，见 [SECURITY.md](SECURITY.md)
