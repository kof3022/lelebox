# THIRD_PARTY_NOTICES — 第三方开源资源登记

> 规则：**任何**接入的第三方代码/素材/字体都必须在此登记：来源、协议、改动清单。
> 审计与决策流程见 [docs/02-协议审计表.md](docs/02-协议审计表.md)。
> 只接受 MIT / Apache-2.0 / GPL-3.0 / BSD / CC0；拒绝 CC-BY-NC 类非商用协议与无 LICENSE 仓库。

## 已接入（随 APK 分发）

| 资源 | 来源 | 协议 | 用途 | 改动清单 |
|------|------|------|------|----------|
| 2048（H5） | [gabrielecirulli/2048](https://github.com/gabrielecirulli/2048) | MIT | L2 管线验证游戏（M0 冒烟用；M1 将替换为原生版） | 未改动源码；WebView 注入老年样式 CSS（字号放大、隐藏分享按钮） |
| 应用图标（生成的 PNG） | 自绘 | — | 启动图标 | — |

## 已规划待接入（M1/M2）

| 资源 | 来源 | 协议 | 用途 | 备注 |
|------|------|------|------|------|
| Simple-Solitaire（纸牌合集） | [TobiasBielefeld/Simple-Solitaire](https://github.com/tobiasBielefeld/Simple-Solitaire) | GPL-3.0 | 纸牌规则/素材参考（或移植 Klondike） | 引用代码需整体 GPL（本项目即 GPL，无冲突） |
| sgtpuzzles（Simon Tatham 谜题合集） | [chrisboyle/sgtpuzzles](https://github.com/chrisboyle/sgtpuzzles) | MIT 风格 | 数独/华容道算法参考 | 仅参考算法，不复制 C 代码 |
| antimine（扫雷） | [lucasnlm/antimine-android](https://github.com/lucasnlm/antimine-android) | Apache-2.0 | 扫雷 + 无障碍实现样板 | 参考架构与无障碍做法 |
| HTML5-Gomoku（五子棋） | [yyjhao/HTML5-Gomoku](https://github.com/yyjhao/HTML5-Gomoku) | 开源 | L2 五子棋 | 接入前需确认 LICENSE 文件 |
| HTML5 中国象棋 | [zhoudaqing/Chess](https://github.com/zhoudaqing/Chess) | 开源 | L2 象棋 | 同上 |
| H5 连连看 | [WangShunYang/link-game](https://github.com/WangShunYang/link-game) | 开源 | L2 连连看 | 同上 |
| H5 麻将接龙 | [ffalt/mah](https://github.com/ffalt/mah) | 开源 | L2 麻将接龙 | 同上 |
| 记忆翻牌 | [bojidar-bg/simple-memory-android](https://github.com/bojidar-bg/simple-memory-android) | 开源 | L1 参考 | — |

> ⚠️ 表中「开源」待接入项，M1 正式接入前必须完成协议核实（见审计表 §2），核实后方可更新本表状态。

## 模板（新接入资源时复制此块追加）

```markdown
| <资源名> | <来源链接> | <协议> | <用途> | <改动清单> |
```
