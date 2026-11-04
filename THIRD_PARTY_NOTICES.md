# THIRD_PARTY_NOTICES — 第三方开源资源登记

> 规则：**任何**接入的第三方代码/素材/字体都必须在此登记：来源、协议、改动清单。
> 判定口径：来源可查、协议明确、逐项登记；只接受 MIT / Apache-2.0 / GPL-3.0 / BSD / CC0，拒绝 CC-BY-NC 类非商用协议；**无 LICENSE 文件的仓库一律视为「保留所有权利」，未经作者授权不得接入**。

## 已接入（随 APK 分发）

| 资源 | 来源 | 协议 | 用途 | 改动清单 |
|------|------|------|------|----------|
| 应用图标（生成的 PNG + 自适应矢量） | 自绘 | — | 启动图标 | — |
| 游戏图标（2048/数独/记忆翻牌/五子棋/连连看/找不同/斗地主）与记忆翻牌动物头像 | **自产**（即梦 AI 生成，2025-08） | — | 首页卡片图标 / 记忆翻牌卡面 | 已压缩为 xxhdpi/xxxhdpi PNG 接入 |
| 牌九图标 + 浅色牌桌背景 | **自产**（即梦 AI 生成，2025-08） | — | 首页牌九卡片图标 `ic_game_paijiu` / 牌九界面桌布 `pj_table` | 图标 2048² 压缩为 xxhdpi 160 / xxxhdpi 216 PNG；桌布 2560×1440 居中裁剪 2:1 存 drawable-xxhdpi/pj_table.jpg |
| 记忆翻牌 / 2048 / 数独（原生） | **本项目自研** | 本项目 GPL-3.0-or-later | L1 原生游戏 | 算法思想参考下述 MIT 项目，未复制其代码 |

> 变更记录：自研 H5「纸牌接龙」已于 **v0.2.2 移除**（页面效果差、多数老年人不会玩）。

## 算法参考（未随包分发，仅参考实现思路）

| 资源 | 来源 | 协议 | 参考内容 |
|------|------|------|----------|
| 2048（H5 原版） | [gabrielecirulli/2048](https://github.com/gabrielecirulli/2048) | MIT | 滑动合并算法思想 → 原生实现 `Game2048.kt` |
| Simon Tatham 谜题合集 | [chrisboyle/sgtpuzzles](https://github.com/chrisboyle/sgtpuzzles) | MIT 风格 | 数独生成/求解算法思想 → 原生实现 `SudokuGame.kt` |
| Simple-Solitaire | [TobiasBielefeld/Simple-Solitaire](https://github.com/tobiasBielefeld/Simple-Solitaire) | GPL-3.0 | Klondike 规则参考 → 自研 H5 实现 |
| antimine（扫雷） | [lucasnlm/antimine-android](https://github.com/lucasnlm/antimine-android) | Apache-2.0 | M2 扫雷 + 无障碍实现样板（未使用） |
| 记忆翻牌 | [bojidar-bg/simple-memory-android](https://github.com/bojidar-bg/simple-memory-android) | 开源 | 玩法参考 → 自研实现（未复制代码） |
| 麻将牌面素材 | [lietxia/mahjong_graphic](https://github.com/lietxia/mahjong_graphic) | M+ 字体授权（免费商用，可修改） | 34 面牌 + 背面 PNG 直接接入 `res/drawable/mj_*`；部分图形源自 [SyaoranHinata/I.Mahjong](https://github.com/SyaoranHinata/I.Mahjong)（随仓库授权） |
| 牌九牌面素材（黑底骨牌） | [ammagician/GuPai](https://github.com/ammagician/GuPai) `src/main/webapp/images/cards/WEN*.png` / `WU*.png` | ⚠️ **仓库无 LICENSE 文件**（见下方「牌九素材许可」注） | 牌九 21 面牌 + 背面 PNG 统一放大接入 `res/drawable-xxxhdpi/pj_*`（`PaiJiuTileFace.kt` 按点数映射） | 原图 50×100 / 41×100，经 LANCZOS 统一为 320×640（1:2.0）；牌背自绘（黑底灰描边「乐」字）；牌面点阵/配色未改动 |

> **牌九素材许可注**：`ammagician/GuPai` 为开源天九 web 游戏（Java + JS，作者 pym，2015），经 GitHub API 核实其仓库**未提供 LICENSE 文件**，因此无法确定明确的开源协议。用户明确指定该仓库作为牌九牌面来源，故按用户要求接入；若后续作者补充许可证，需按其条款重新评估。接入遵守项目审计规则的精神：牌面仅做尺寸统一缩放，未改动点阵与配色；牌背为自绘。

## 已评估弃用（审计未通过，记录在案）

| 资源 | 原因 |
|------|------|
| kriscarilloxyz/html5-solitaire-js（含原版 SolitaireJS 系） | 仓库**无 LICENSE 文件**（审计规则 §1.1），且为 PhoneGap 年代旧代码、含大量平台适配冗余 → 弃用，改为自研 |
| GodLeaveMe/Pixel-Memories、xosg/WebGames（长尾来源池） | LICENSE 待核实，核实前不接入 |

## 模板（新接入资源时复制此块追加）

```markdown
| <资源名> | <来源链接> | <协议> | <用途> | <改动清单> |
```
