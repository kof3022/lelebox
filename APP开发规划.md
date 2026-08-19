# 乐龄游戏盒 · 老年游戏聚合 APP 开发规划（v0.1 草案）

> 一句话定位：**给爸妈/爷爷奶奶的一台"打开即玩"的游戏盒子——零授权、无账号、无广告、无收费、大字大按钮、永远不弹窗。**
>
> 项目代号：LeLeBox（乐龄游戏盒，备选名：银龄乐园 / 老友棋牌屋 / 闲时游戏盒，见文末待决策项）

---

## 1. 产品铁律（写进 README 的第一行，任何功能不得违反）

| # | 铁律 | 落地方式 |
|---|------|----------|
| 1 | 零授权 | Manifest 不声明任何危险权限；默认全离线（连 INTERNET 权限都不要） |
| 2 | 无账号体系 | 无注册/登录/手机号/头像/实名；进度只存本机 SharedPreferences |
| 3 | 无广告 | 不接任何广告 SDK（AdMob/穿山甲等一律不引入） |
| 4 | 无收费 | 无内购、无订阅、无"去广告"按钮（因为根本没有广告） |
| 5 | 打开即玩 | 首屏即游戏宫格，无引导页、无弹窗、无强制更新、无隐私政策确认框（因为不收集数据，连隐私政策都可省） |
| 6 | 操作简单 | 单击为主，无手势依赖，无长按菜单；每个游戏提供"按钮操作模式"兜底 |
| 7 | 文字放大 | 全局最小字号 20sp+，系统大字适配，应用内"超大字体"开关 |
| 8 | 无压力 | 无倒计时惩罚、无失败扣分、无体力/生命值、随时可退出且自动存档 |

---

## 2. 目标用户与场景

- **用户**：60–85 岁，智能手机使用水平偏低（会刷视频/微信，但怕点错、怕收费、怕广告）。
- **设备**：以子女淘汰的中低端 Android 手机为主（4–8 年前机型），屏幕 5–7 寸，部分开启系统大字体。
- **场景**：独居无聊时、饭后消遣、陪孙子玩、康复/认知锻炼。
- **痛点对照**：主流游戏要登录 → 我们零账号；广告多 → 我们零广告；付费 → 我们免费开源；操作复杂 → 我们"大字 + 单点 + 按钮模式"。

---

## 3. 技术架构选型

### 3.1 方案对比

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| A. Kotlin + Jetpack Compose 原生外壳 + 离线 WebView 内嵌开源 H5 游戏（**推荐**） | 开源游戏资源 90% 是 H5/JS，复用成本最低；零权限容易实现；Compose 对大字/无障碍支持好；包体小（<15MB） | H5 游戏需做字体/CSS 适配；老设备 WebView 版本参差 | **首选**：外壳原生保体验，游戏分层接入 |
| B. 纯原生逐游戏实现 | 质量、无障碍、性能最佳 | 开发慢，10 个游戏要写 10 遍 | 只用于"旗舰游戏"（2048/记忆翻牌/数独） |
| C. Flutter 外壳 | 跨平台、UI 一致 | 包体大、WebView 插件维护成本、社区 H5 复用同 A | 不采用（团队无既有 Flutter 资产） |
| D. Godot 引擎单项目多场景 | 自研休闲游戏（钓鱼/打地鼠）质量高 | 与"聚合开源 H5"路线冲突，学习成本 | **第二阶段**自研 2–3 个游戏时引入 |
| E. 纯 H5 App（WebView 套壳） | 最快 | 首屏体验、系统集成、无障碍都打折 | 不采用（用户是老年人，外壳体验必须原生） |

### 3.2 推荐架构（分层）

```
┌─────────────────────────────────────────────┐
│ 外壳层（Kotlin + Jetpack Compose）           │
│  · 首页游戏宫格（9 宫格大卡片，可翻页）       │
│  · 设置：超大字体 / 高对比度 / 音量 / 清进度  │
│  · 全局："返回上级"大按钮、退出二次确认       │
├─────────────────────────────────────────────┤
│ 游戏层（三档接入策略）                       │
│  L1 原生旗舰（Compose 实现）：2048、记忆翻牌、│
│     数独 —— 体验/无障碍最佳，做"招牌"        │
│  L2 离线 H5（WebView 加载 assets/）：纸牌、  │
│     五子棋、象棋、连连看、麻将接龙、扫雷等   │
│     · 加载前注入统一 CSS（全局放大 1.6×）    │
│     · JS 桥：字号/难度/音量/返回/存档        │
│  L3 长尾自研（第二阶段，Godot 导出）：钓鱼、 │
│     打地鼠类动作小游戏                       │
├─────────────────────────────────────────────┤
│ 内核层：零权限清单 + WebView 加固 + 存档      │
└─────────────────────────────────────────────┘
```

### 3.3 零权限与 WebView 安全基线

- `AndroidManifest.xml` **不声明任何权限**；`minSdk 24`（Android 7.0，覆盖存量机型），`targetSdk` 最新。
- WebView 只加载 `file:///android_asset/` 本地资源：`setJavaScriptEnabled(true)`（必须）、`setAllowFileAccess(false)`、`setAllowContentAccess(false)`、`setDomStorageEnabled(true)`、`blockNetworkLoads(true)`（彻底断网，双保险）。
- 不申请 `READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`：存档全走 SharedPreferences/DataStore，H5 存档经 JS 桥回写。
- 每个 H5 游戏入口包一层"游戏页壳"（Compose）：顶部大字标题 + 返回 + 字体缩放，避免 H5 内自己实现导航。

### 3.4 打包与分发

- 输出 AAB + 可直接安装的 APK（国内渠道需要 APK）。
- 完全离线，安装后**永不需要联网**，因此没有隐私政策弹窗的合规负担。

---

## 4. 老年友好设计规范（UI/UX 基线，所有游戏统一执行）

参考适老化设计 9 原则（[uisdc 适老化设计原则](https://www.uisdc.com/group/612163.html)、[适老化设计要点（中老服协）](https://www.siaa.org.cn/news_content?id=750)）：

1. **字号**：正文 ≥ 20sp；按钮文字 ≥ 22sp；全局提供"标准 / 大 / 超大"三档（同时跟随系统字体缩放）。
2. **触控目标**：≥ 48dp（Google 底线），本应用强制 **≥ 64dp**，卡片式按钮带明显按压反馈。
3. **对比度**：前景/背景对比度 ≥ 7:1；提供"高对比度模式"（黑底白字/白底黑字+粗描边）。
4. **单击优先**：不做双击、长按、滑动切换；滑动类游戏（如 2048）提供四方向按钮替代。
5. **无时间压力**：所有游戏无倒计时惩罚；计时只作"成绩展示"不扣分。
6. **防误触**：返回/退出一律二次确认；误点不产生破坏性操作（先存档再执行）。
7. **一致性**：所有游戏共用同一套顶栏（返回 + 标题 + 字体/音量）、同一套配色、同一套按钮。
8. **零学习成本**：游戏说明用"一屏大字图文"（首次进入自动显示，可常驻在顶栏"帮助"按钮）。

---

## 5. 无障碍（Accessibility）

- 原生游戏：全部控件 `contentDescription`，支持 TalkBack 完整朗读；操作链路单手指可完成。
- H5 游戏：注入 `role`/`aria-label` 标注，按钮用真实 `<button>`；提供"按钮操作模式"（所有交互都有等价的屏幕按钮）。
- 系统级：跟随系统字体缩放与"移除动画"设置；不锁定屏幕方向（老年人可能锁定竖屏，游戏需横竖屏都可用）。
- 参考实现：**antimine（开源扫雷）明确支持 TalkBack/开关控制，是本项目无障碍的样板**（见开源资源 §7）。

---

## 6. 游戏清单与选型（重点讨论：先做哪几个）

### 6.1 选游戏的标准（打分制）

| 维度 | 权重 | 说明 |
|------|------|------|
| 规则简单 | 30% | 一句话能讲清，无需教程 |
| 无时间压力 | 25% | 不考验反应速度 |
| 认知价值 | 20% | 益智/锻炼记忆，防认知衰退 |
| 开源可得性 | 15% | 有高质量 MIT/Apache/GPL 开源实现 |
| 国人熟悉度 | 10% | 纸牌、麻将、象棋、连连看等本土化强 |

### 6.2 第一批（MVP，强烈建议这 4 个，覆盖"牌、数、忆、弈"四种心智模式）

| 游戏 | 一句话规则 | 为什么进第一批 | 载体 | 开源资源 | 预估工时 |
|------|-----------|----------------|------|----------|----------|
| **2048** | 滑动合并相同数字 | 规则 10 秒学会；原生实现极简；滑块+按钮双操作 | **L1 原生** | [gabrielecirulli/2048（MIT，13k star）](https://github.com/gabrielecirulli/2048) 可作算法参考 | 3–4 天 |
| **纸牌接龙（Klondike）** | 按顺序把牌收进四个花色的家 | 全球老人最熟的单机游戏；可加"自动提示" | **L2 H5**（长线自研） | [TobiasBielefeld/Simple-Solitaire（Android 纸牌合集，GPL-3.0，无广告）](https://github.com/tobiasBielefeld/Simple-Solitaire) | 4–5 天（含适配） |
| **数独** | 每行每列每宫 1–9 不重复 | 最经典益智；可生成题目+难度；防认知衰退 | **L1 原生** | [chrisboyle/sgtpuzzles（Simon Tatham 谜题合集，含数独，MIT 风格许可）](https://github.com/chrisboyle/sgtpuzzles) 算法参考 | 4–5 天 |
| **记忆翻牌** | 翻开两张相同的牌 | 规则最简单；锻炼记忆力；素材自绘即可 | **L1 原生** | [bojidar-bg/simple-memory-android（安卓记忆翻牌）](https://github.com/bojidar-bg/simple-memory-android) 参考 | 2–3 天 |

> 第一批决策理由：**先做 1 个"手熟国民级"（纸牌）、1 个"零门槛数字"（2048）、1 个"益智王牌"（数独）、1 个"认知锻炼"（记忆翻牌）**，四者玩法差异大、覆盖不同老人偏好，且两个原生两个 H5，正好把两套接入管线（L1/L2）都打通，为第二批铺路。

### 6.3 第二批（+6，MVP 验证后 4–6 周内补齐）

| 游戏 | 载体 | 开源资源 | 备注 |
|------|------|----------|------|
| 五子棋（人机） | L2 H5 | [yyjhao/HTML5-Gomoku](https://github.com/yyjhao/HTML5-Gomoku)、[yangboz/HTML5-Gomoku](https://github.com/yangboz/HTML5-Gomoku) | 人机难度分三档 |
| 中国象棋（人机） | L2 H5 | [zhoudaqing/Chess（HTML5 中国象棋）](https://github.com/zhoudaqing/Chess) | 本土国民棋类 |
| 连连看 | L2 H5 | [WangShunYang/link-game（H5 连连看）](https://github.com/WangShunYang/link-game) | 规则极简 |
| 扫雷 | L2 H5 / L1 移植 | [lucasnlm/antimine-android（Apache-2.0，无广告，支持 TalkBack）](https://github.com/lucasnlm/antimine-android) | **无障碍样板项目** |
| 麻将接龙（Mahjong Solitaire） | L2 H5 | [ffalt/mah（HTML5 麻将接龙）](https://github.com/ffalt/mah) | 华人情感连接强 |
| 华容道/滑块拼图 | L2 H5 | sgtpuzzles 内含 sliding puzzle；自研亦可 | 经典益智 |

### 6.4 长尾候选（滚动加入，按用户反馈排期）

俄罗斯方块、斗地主（单机人机）、找不同、24 点口算、看图识字、养生知识问答、钓鱼/打地鼠（Godot 自研）、听声音辨方向（锻炼听觉）。

---

## 7. 开源资源盘点（优先开源复用，附协议）

### 7.1 核心资源（上面已列，汇总）

| 项目 | 内容 | 协议 | 用途 |
|------|------|------|------|
| [gabrielecirulli/2048](https://github.com/gabrielecirulli/2048) | 2048 原版 H5 | MIT | L1 算法参考 |
| [TobiasBielefeld/Simple-Solitaire](https://github.com/tobiasBielefeld/Simple-Solitaire) | 安卓纸牌合集 | GPL-3.0 | 纸牌规则/素材参考 |
| [chrisboyle/sgtpuzzles](https://github.com/chrisboyle/sgtpuzzles) | Simon Tatham 谜题合集安卓移植（数独/华容道/数回/点灯等 40+） | MIT 风格 | 数独算法 + 长线益智池 |
| [lucasnlm/antimine-android](https://github.com/lucasnlm/antimine-android) | 开源扫雷，无广告，无障碍优秀 | Apache-2.0 | 扫雷 + 无障碍样板 |
| [yyjhao/HTML5-Gomoku](https://github.com/yyjhao/HTML5-Gomoku) | H5 五子棋 | 开源 | L2 五子棋 |
| [zhoudaqing/Chess](https://github.com/zhoudaqing/Chess) | H5 中国象棋 | 开源 | L2 象棋 |
| [WangShunYang/link-game](https://github.com/WangShunYang/link-game) | H5 连连看/消消乐 | 开源 | L2 连连看 |
| [ffalt/mah](https://github.com/ffalt/mah) | H5 麻将接龙 | 开源 | L2 麻将 |
| [bojidar-bg/simple-memory-android](https://github.com/bojidar-bg/simple-memory-android) | 安卓记忆翻牌 | 开源 | L1 参考 |
| [GodLeaveMe/Pixel-Memories](https://github.com/GodLeaveMe/Pixel-Memories) | 纯前端经典游戏合集（即开即玩） | 开源 | 长尾游戏来源池 |
| [xosg/WebGames](https://github.com/xosg/WebGames) | 轻量网页游戏合集 | 开源 | 长尾游戏来源池 |
| [Cateners/gardendless](https://github.com/Cateners/gardendless) | 把 Web 游戏打包进安卓 APK 的完整示例 | 开源 | **L2 接入管线架构参考** |
| [j8267643/awesome-open-source-games](https://github.com/j8267643/awesome-open-source-games)、[michelpereira/awesome-open-source-games](https://github.com/michelpereira/awesome-open-source-games) | 开源游戏大清单 | — | 选游戏目录 |
| [F-Droid 游戏分类](https://f-droid.org/zh_Hans/categories/games/) | 无广告开源安卓游戏目录 | — | 候选池 + 上架渠道 |

### 7.2 协议审计规则（接入任何开源游戏前必做）

1. 只接受 **MIT / Apache-2.0 / GPL-3.0 / BSD / CC0**；**拒绝** CC-BY-NC 类非商用协议、无 LICENSE 的仓库、以及"开源但内置广告 SDK"的项目（打开源码先 grep 广告 SDK 关键字）。
2. GPL-3.0 允许使用，但**本项目必须整体开源**——本项目的定位本来就是完全开源免费，无冲突。
3. 每个游戏建 `THIRD_PARTY_NOTICES.md`，记录：来源仓库、协议全文链接、改动清单（我们对字体/按钮的适配改动）。
4. 素材（图标/音效）：优先用 CC0 素材库（如 Kenney、OpenGameArt CC0 区），自绘矢量图标为主。

---

## 8. 需要的插件与 Skills

### 8.1 本机 DSH 插件（开发协作直接可用）

| 插件 | 用途 |
|------|------|
| **dsh-task-board** | 把 M0–M3 里程碑拆成看板任务，可钉住工作区与执行模式、定时跑构建/审计 |
| **dsh-aionui-panel** | 右侧预览 Markdown 规划/代码/diff、SCM 变更管理（git stage/discard） |
| **cocoloop** | 安装/更新/安全审计下面的 skills |
| **dsh-ssh** | （可选）远程真机/构建机部署、发布服务器运维 |

### 8.2 推荐安装的 Skills（`npx skills add <owner/repo@skill> -g -y`，用 `cmd /c npx ...` 绕过本机执行策略）

**必装（核心开发）——安装状态（2025-06 已执行）：**

| Skill | 用途 | 状态 |
|-------|------|------|
| `wshobson/agents@mobile-android-design`（20.8k 安装）→ https://skills.sh/wshobson/agents/mobile-android-design | Material3 + Jetpack Compose 设计规范 | ✅ 已装 |
| `new-silvermoon/awesome-android-agent-skills@android-accessibility` → https://skills.sh/new-silvermoon/awesome-android-agent-skills/android-accessibility | 安卓无障碍审计清单（Compose） | ✅ 已装 |
| `drjacky/claude-android-ninja`（310 安装）→ https://skills.sh/drjacky/claude-android-ninja/claude-android-ninja | Kotlin/Compose/MVVM/Hilt/Room/多模块 Gradle 全栈安卓开发（**替代**已失效的 `partme-ai/full-stack-skills@android-kotlin`——原仓库已重构、无 SKILL.md） | ✅ 已装 |
| ~~`alphaonedev/openclaw-graph@android-jetpack`~~ | 原仓库已删除（404） | ❌ 失效，职责已由 mobile-android-design 覆盖，无需替代 |

> 安装技巧：本机直连 GitHub 不通（连接被重置），使用镜像环境变量方案绕过：`$env:GIT_CONFIG_COUNT=1; $env:GIT_CONFIG_KEY_0="url.https://gh-proxy.com/https://github.com/.insteadOf"; $env:GIT_CONFIG_VALUE_0="https://github.com/"`，再执行 npx 安装。

**建议装（H5 游戏接入与质量）：**
5. `yakoub-ai/phaser4-gamedev@phaser-mobile` + `@phaser-build`（若自研/改造 H5 游戏用 Phaser）→ https://skills.sh/yakoub-ai/phaser4-gamedev/phaser-mobile
6. `omer-metin/skills-for-antigravity@mobile-game-dev`（移动游戏开发方法论）→ https://skills.sh/omer-metin/skills-for-antigravity/mobile-game-dev
7. `amit-nayar/android-adb-skill@android-device`（ADB 真机自动化测试）→ https://skills.sh/amit-nayar/android-adb-skill/android-device
8. `openai/plugins@android-performance`（性能优化）→ https://skills.sh/openai/plugins/android-performance

**第二阶段（Godot 自研游戏时）：**
9. `wshobson/agents@godot-gdscript-patterns`（13.9k 安装）→ https://skills.sh/wshobson/agents/godot-gdscript-patterns
10. `zate/cc-godot@godot-ui`（Godot UI，做大按钮大字天然契合）→ https://skills.sh/zate/cc-godot/godot-ui

> 说明：以上 skills 安装后即成为本项目开发时可直接调用的"任务说明书"；安装用 `cmd /c "npx skills add <owner/repo@skill> -g -y"`（本机 PowerShell 执行策略拦截 npx.ps1，走 cmd 即可）。

### 8.3 外部工具链

Android Studio（Kotlin 2.x + Compose）、Gradle、JDK 17+、真机测试矩阵（中低端机 ×3、开启系统大字体、开启 TalkBack）、GitHub Actions（构建 AAB/APK 产物）、F-Droid 上架流程。

---

## 9. 里程碑与工时估算（单人或小团队 13 周）

| 阶段 | 周期 | 交付物 | 验收标准 |
|------|------|--------|----------|
| **M0 立项** | 第 1 周 | 本规划定稿、游戏清单拍板、协议审计表、GitHub 仓库、CI 骨架 | 首批游戏开源资源全部验证可运行 |
| **M1 MVP** | 第 2–6 周 | 外壳（宫格首页+设置+游戏壳）+ **4 个第一批游戏** + 零权限打包 | 安装即玩；断网可玩；TalkBack 可读首页与 2048 |
| **M2 扩容** | 第 7–10 周 | +6 个第二批游戏、超大字体/高对比度模式、无障碍走查、真机矩阵测试 | 10 款游戏全部通过"老年试用问卷"（家人内测） |
| **M3 发布** | 第 11–13 周 | 图标/启动图、打磨动画、协议文档、开源发布（GitHub）+ F-Droid/酷安/官网 APK | 无障碍审计报告 + 真机报告归档 |

---

## 10. 风险与对策

| 风险 | 对策 |
|------|------|
| 老设备 WebView 版本旧，部分 H5 游戏白屏 | 关键游戏 L1 原生兜底；H5 兼容性测试矩阵锁定 WebView 57+；必要时内置 `Android System WebView` 提示 |
| GPL-3.0 传染 | 项目整体开源（本来就是定位）；被引用的 GPL 项目源码保持可追溯 |
| H5 游戏默认字号太小、按钮难点 | 统一注入"老年样式 CSS"（全局放大 1.6×、按钮最小 64dp 等效）；只选可定制 UI 的开源项目；不可定制的游戏弃用 |
| 无障碍与游戏操作冲突 | 每个游戏提供按钮操作模式；TalkBack 走查用例固定每周跑一轮 |
| 误触退出/误操作 | 二次确认 + 自动存档 + 游戏内"撤销"按钮 |
| "开源"鱼目混珠带广告 | 协议审计 + 源码 grep 广告 SDK（adMob/穿山甲/unityads）+ 仅信任高 star 活跃仓库 |
| 老年人不会用"返回手势" | 全 App 顶部常驻大"← 返回"按钮，禁用系统手势依赖 |
| 包体膨胀 | 每个 H5 游戏瘦身（去未用资源）；目标 APK < 20MB |

---

## 11. 待你拍板的决策点

1. **项目名**：默认「乐龄游戏盒 / LeLeBox」，备选「银龄乐园」「老友棋牌屋」「闲时游戏盒」——你定。
2. **首发市场**：纯国内（酷安/官网侧载/F-Droid 国内镜像）还是同时上 Google Play？
3. **是否绝对离线**：我强烈建议零 INTERNET 权限（彻底断网、免合规负担），代价是游戏更新只能靠装新版 APK——接受吗？
4. **第一批 4 款**（纸牌、2048、数独、记忆翻牌）是否按此锁定？
5. **开源策略**：项目仓库从第一天就公开开源（我建议这样做，也符合 GPL 合规），还是先私有、发布时再开源？
6. 要不要我**现在就动手**：创建仓库骨架 + 搭建 M0 目录（协议审计表、THIRD_PARTY_NOTICES 模板、Compose 外壳项目脚手架）？

---

## 12. 已拍板决策（2025-06 确认）

| 决策点 | 结论 |
|--------|------|
| 项目名 | **乐龄游戏盒 / LeLeBox** |
| 第一批游戏 | **2025-08 修订：纸牌接龙已移除**（自研 H5 效果差、多数老人不会玩），现为 **2048、数独、记忆翻牌** 三款原生 |
| 网络策略 | **绝对离线**：零 INTERNET 权限，更新靠安装新版 APK |
| 当前动作 | 必装 Skills 已装 3 枚（mobile-android-design、android-accessibility、claude-android-ninja；原 android-kotlin/android-jetpack 条目已失效并处理），脚手架待后续 |

---

*规划版本：v0.2 · 已记录首轮决策 · 所有第三方资源链接均已在 §7 列出*
