---
feature: polish-batch
status: delivered
updated: 2026-02-14
branch: fix/polish-batch
commits: 45a4280..e56e125
---

# Polish Batch

## Report

**What was built** — 修复只读分析中的非功能大改缺陷：单测对齐 `CapsuleConfig` 并恢复可编译；Remote 桥接补 Long 与断开 detach；Hook prefs 失败不再永久缓存 SystemUI 本地 SP，偏好监听只绑 remote；安全模式 draw 与 Provider 一致；详情页补导航栏 inset；横竖屏开关不再依赖沉浸式；偏移滑条对齐 ±200 钳制；重置/恢复改按钮语义并 Toast；深色状态卡与 API 动态值；关 blur 不订阅重力；CI 先跑单测；gitignore 白名单 `.github`；版本 1.0.3 / code 13。

**Verification** — `:app:compileDebugKotlin` / `compileDebugJavaWithJavac` / `testDebugUnitTest` / `assembleDebug` 均 PASS。独立审查 12 条验收：11 条通过，DetailPage inset 经二次修复后改为显式 `WindowInsets.navigationBars`。

**Journey log**
- miuix Scaffold 在 `contentWindowInsets` 仅 Horizontal 时 bottom 恒为 0，不能靠 `innerPadding` 补导航栏。
- Remote prefs 失败时不可把 SystemUI 本地 SP 写回 `remotePreferences`，否则配置永久失效。
- 偏好监听若绑在 fallback 本地 SP 上，后续 remote 变更不会刷新。
- 本环境 `git worktree add` 被沙箱拒绝，改在同仓 feature 分支实现。
- 版本自动升方案已按用户要求回退，发版仍为手动 tag 触发。

## [S1] Problem
只读分析发现一批非功能大改的缺陷：单测编译失败、远程配置桥接漏 Long、Hook prefs 回退永久缓存、详情页底部 inset、横竖屏开关与 Hook 语义不一致、安全模式双源、文案过时、重置无反馈、CI 不跑测试等。

## [S2] Design
- 配置契约继续以 `CapsuleConfig` 为唯一键源；测试改为断言 `CapsuleConfig` 键/默认值与 `AppPreferences` 读写一致。
- `RemotePreferencesBridge.attach` 同步全部类型含 Long；`put*` 服务未绑定时仍写本地，绑定后由 attach 全量覆盖 remote。
- Hook `preferences()`：remote 失败可重试；SystemUI 本地 SP 仅作临时 fallback，不永久缓存；`refreshAll` 判空；监听只绑 remote。
- `CapsuleDrawable.draw` 与 `isSafeMode` 一致，读 Provider。
- UI：DetailPage 显式使用导航栏 inset；横竖屏开关不再依赖沉浸式开启；偏移滑条范围对齐配置钳制；重置/恢复用按钮语义；补 toast。
- 工程：CI 跑单测；gitignore 白名单加 `.github`；`SafeModeProvider` 复用 `CapsuleConfig.PREFS`；版本 1.0.3/13。

## [S3] Out of Scope
- 不改 hook 算法/材质渲染质量
- 不做英文 i18n 全量
- 不启用 main push 自动升版本
- 不重写底栏液态玻璃

## Tasks
- [x] T1: 修复 AppPreferencesDefaultsTest 并对齐 CapsuleConfig — acceptance: `testDebugUnitTest` 通过 (covers: S2)
- [x] T2: Remote 桥接 Long + Hook prefs 重试/判空 — acceptance: 代码审查通过，无永久 SystemUI 缓存 (covers: S2)
- [x] T3: 安全模式 draw 路径与 Provider 一致 + PREFS 常量复用 — acceptance: 代码审查通过 (covers: S2)
- [x] T4: UI inset/开关/滑条/文案/重置反馈/按钮语义 — acceptance: 编译通过，关键交互代码正确 (covers: S2)
- [x] T5: 传感器与深色状态卡 + CI test + gitignore — acceptance: 配置正确 (covers: S2)
- [x] T6: assembleDebug + unit test 验证 — acceptance: 本地构建与测试通过 (covers: S2)
- [ ] T7: bump 1.0.3/13 并打 tag — acceptance: tag 可触发 CI (covers: S2)
