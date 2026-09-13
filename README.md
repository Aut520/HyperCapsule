# HyperCapsule

HyperOS 沉浸式状态栏双胶囊 LSPosed 模块。

全屏下拉临时状态栏时，为时间与系统图标绘制可配置的胶囊背景（半透明 / 高斯模糊 / 柔光 / 液态玻璃等）。

## 要求

- Android 15 / HyperOS 2，或 Android 17 / HyperOS 4
- LSPosed（libxposed API 102）
- 作用域：`com.android.systemui`
- 可选：Root（用于快速重启系统界面）

## 构建

```bash
# JDK 21+
./gradlew assembleDebug
# 或 Windows
gradlew.bat assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

Release 需在 `other/signing/keystore.properties` 配置签名后执行 `assembleRelease`。

## 使用

1. 安装 APK，在 LSPosed 中启用模块  
2. 勾选作用域 **系统界面** (`com.android.systemui`)  
3. 重启系统界面（应用内右上角重启按钮，或重启手机）  
4. 在「功能 → 系统界面」里调整胶囊材质、尺寸、左右布局等  

配置即时写入；部分 Hook 行为需重启 SystemUI 后完全生效。

## 许可证

GNU GPL v3.0，见 [LICENSE](LICENSE)。
