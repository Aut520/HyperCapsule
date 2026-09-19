# 更新说明

本文件由 Release workflow 读取，对应 `v*` 版本小节会写入 GitHub Release 正文。

## v1.0.8

- **修复「关闭横屏灵动岛」**：对齐 HyperIsland / SystemUI 真实藏岛路径，不再依赖错误的 immersive 协议或状态栏 View 扫描。
- **收窄生效范围**：仅在开关打开且处于**横屏全屏**（immersive、状态栏半透明/隐藏、横屏下拉）时隐藏 HyperOS 灵动岛；竖屏、竖屏全屏、横屏非全屏保持系统默认。
- **Hook 插件 ClassLoader**：岛 UI 位于 `miui.systemui.plugin`，动态加载后仍可挂上 Hook。
- **降低管理端被无谓拉起**：配置本地缓存，SafeMode Provider 查询节流，绘制热路径不再每帧读远程配置。

## v1.0.7

- 尝试修复 OS3–OS4「关闭横屏灵动岛」（初版 immersive 方案，由 v1.0.8 修正）。

## v1.0.6

- 浅色主页状态卡跟随应用主题，不再错误依赖系统深浅色。
- 关于页去掉 logo 外层图标背景圆。

## v1.0.5

- 状态卡浅/深色色调修正，浅色主页使用白底表面色。

## v1.0.4

- 支持 HyperOS OS1–OS4 胶囊能力；超级岛隐藏仅 OS3+。
- 适配 `BarTransitions` 在 OS3/OS4 的包名迁移与岛相关方法签名差异。
