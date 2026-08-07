# Android WebView Shell Demo

最小 Android 原生 WebView 壳，用于加载指定 H5 地址，并开启 WebView 调试。

## 环境

- Android Studio: `C:\Program Files\Android\Android Studio\bin\studio64.exe`
- Android SDK: `%LOCALAPPDATA%\Android\Sdk`
- JDK: Java 21
- Gradle: 使用项目内 Gradle Wrapper `gradlew.bat`

## 默认 H5 地址（信息采集平板端）

WebView **必须**加载信息采集平板端工作台，路径格式：

```
/practical-collection/workbench/{taskId}
```

| 路径 | 实际页面 | 是否适合 APK |
|------|----------|--------------|
| `/practical-collection/workbench/{taskId}` | 信息采集平板端工作台（扫码/指纹/签名） | ✅ 正确 |
| `/practical-collection/` | 平板端任务选择页 | ❌ 需再选任务 |
| `/` 或 `/dashboard` | PC 管理端 | ❌ 错误入口 |
| `/assessor-scoring` | 平板考评端 | ❌ 另一套平板应用 |

默认地址在 `app/src/main/res/values/strings.xml`（prod）和 `app/src/localTest/res/values/strings.xml`（localTest）：

```xml
<string name="default_web_url">http://192.168.1.98:5173/practical-collection/workbench/2069972938644135937</string>
```

`{taskId}` 换成你本机要测试的采集任务 ID。登录页会显示「平板信息采集端登录」，需账号具备 `certification.practical.collection.operate` 权限。

Android 模拟器访问宿主机服务时使用 `10.0.2.2`。真机访问局域网服务时改成电脑局域网 IP，例如 `http://192.168.1.10:5173/practical-collection/workbench/{taskId}`。

## 构建变体

| 变体 | 用途 | 包名 | 默认 H5（信息采集平板端） |
|---|---|---|---|
| `prodDebug` | 正式环境 | `com.example.webviewshelldemo` | `https://java.app.gjjd168.com/practical-collection` |
| `localTestDebug` | 本机联调 | `com.example.webviewshelldemo.localtest` | `192.168.1.98:5173/practical-collection/workbench/{taskId}` |

两个变体可同时安装，互不影响。

## 构建

```powershell
cd apps/android-webview-demo

# 远程服务器版（与线上一致）
.\gradlew.bat :app:assembleProdDebug

# 本机测试版（localTest）
.\gradlew.bat :app:assembleLocalTestDebug
```

## 本机测试（localTest）

前提：本机已启动 `admin-web`（5173）和 `platform-api`（8080），且平板与电脑在同一局域网。

平板通过局域网 IP 访问本机（默认 `192.168.1.98`），无需 `adb reverse`：

```powershell
adb install app/build/outputs/apk/localTest/debug/app-localTest-debug.apk
```

也可直接运行脚本（自动 reverse、构建、安装、启动）：

```powershell
.\wired-debug.ps1 -LocalTest
```

Android 模拟器未走 `adb reverse` 时，可把 `app/src/localTest/res/values/strings.xml` 里的地址改成 `http://10.0.2.2:5173/...`。

## 安装和指定 H5 地址（prod 远程版）

```powershell
adb install -r app/build/outputs/apk/prod/debug/app-prod-debug.apk

adb shell am start `
  -n com.example.webviewshelldemo/.MainActivity `
  --es web_url "https://java.app.gjjd168.com/practical-collection"
```

临时覆盖 H5 地址时，可用：

```powershell
adb shell am start `
  -n com.example.webviewshelldemo/.MainActivity `
  --es web_url "https://java.app.gjjd168.com/practical-collection/workbench/{taskId}"
```

也支持通过 `VIEW` data URL 启动：

```powershell
adb shell am start `
  -n com.example.webviewshelldemo/.MainActivity `
  -a android.intent.action.VIEW `
  -d "https://example.com"
```

## WebView 调试

代码中已调用：

```java
WebView.setWebContentsDebuggingEnabled(true);
```

连接设备后，在 Chrome 打开 `chrome://inspect/#devices` 即可调试 WebView。

## 原生扫码能力

H5 可通过 `AndroidBridge.scanQRCode(requestId, payloadJson)` 调起原生二维码扫描。

- 依赖：`com.journeyapps:zxing-android-embedded`
- 权限：首次扫码会申请 `CAMERA`
- 回调：沿用 `window.__onNativeCapabilitySuccess` / `window.__onNativeCapabilityError`，`type=scan`
- 成功字段：`success`、`code`、`rawText`、`parsed`、`message`

平板端实操采集页会自动检测 `AndroidBridge.scanQRCode`，无需额外配置 `VITE_SCAN_MODE`。
