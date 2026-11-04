# 安全策略

## 支持的版本

| 版本 | 状态 |
| --- | --- |
| 最新 Release（`v0.5.7-m7` 及以后） | 接受报告 |
| 更早的内测包、自行构建/改包的版本 | 不接受，请先升级到最新 Release |

## 如何报告漏洞

**请不要开公开 Issue。**

推荐使用 GitHub 的私有漏洞报告（Private Vulnerability Reporting）：

> 仓库页 **Security** 标签 → **Report a vulnerability**
> 直达：<https://github.com/kof3022/lelebox/security/advisories/new>

如果该入口不可用，请在 Issue 里只写「有安全问题，请提供联系渠道」，**不要贴细节**，维护者会私下联系你。

报告里请尽量包含：

- 受影响版本（应用内「设置」页底部可看到版本号）
- 复现步骤（越具体越好）
- 影响面：能读到什么、能改到什么、需要什么前提条件
- 设备与系统版本（例如 Redmi Note 12 / Android 13）
- 如有 PoC、日志、录屏，请一并附上

## 响应预期

这是个人业余维护的项目，尽力而为、不承诺 SLA：

- **3 天**内确认收到
- **30 天**内给出评估结论（修复 / 接受风险 / 判定不属于安全范围）
- 修复后会在 Release Notes 中致谢（除非你要求匿名）

## 本项目重点关注的问题类型

应用**完全离线、Manifest 零权限、无任何第三方 SDK**，因此攻击面与普通联网 App 差别很大。重点关注：

1. **权限回退**：代码/依赖引入了新的 `uses-permission`（尤其是 `INTERNET`、存储、位置），或让原本不可见的内部权限变成了用户可见权限
2. **数据外泄**：任何网络请求、埋点、SDK 回传，或把本机进度写到了可被其它应用读取的位置
3. **本地存档**：`SharedPreferences`（`game_saves` / `lelebox_settings`）被其它应用读取或篡改；`allowBackup=false` 的绕过路径
4. **WebView 桥接**：若恢复 L2 离线 H5 接入，`addJavascriptInterface` 暴露的方法面（当前 9 款游戏均为 Compose 原生实现，未启用）
5. **供应链与许可**：依赖或素材来源不明、协议冲突（参见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)）
6. **构建与发布**：CI 或发布流程中的密钥泄漏（签名文件、token）

## 不在范围内

- 已 root / 越狱设备上的本地数据修改
- 需要物理接触设备、或需要用户主动安装恶意软件的攻击场景
- 社会工程学、钓鱼邮件
- 单纯的功能缺陷、界面错位、文案问题（请走普通 [Issue](https://github.com/kof3022/lelebox/issues) 模板）
- 对「无障碍功能」被滥用的担忧（TalkBack 支持是产品目标，不是漏洞）

## 已知设计取舍（不算漏洞）

- APK 中存在一个 AndroidX 自动生成的 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（signature 级、系统权限列表不可见），任何 AndroidX 应用都有，无法也不需移除。
- 无加固、无签名校验、无防重新打包设计：本项目以 GPL-3.0-or-later 开源，**重新打包并分发是许可允许的行为**（需同样开源），不会被视为攻击。
- 无后端、无账号、无遥测：若发现「有网络行为」，那一定是缺陷，请按上面的渠道报告。
