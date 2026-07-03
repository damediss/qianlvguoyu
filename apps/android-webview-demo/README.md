# Android WebView Shell Demo

最小 Android 原生 WebView 壳，用于加载指定 H5 地址，并开启 WebView 调试。

## 环境

- Android Studio: `C:\Program Files\Android\Android Studio\bin\studio64.exe`
- Android SDK: `%LOCALAPPDATA%\Android\Sdk`
- JDK: Java 21
- Gradle: 使用项目内 Gradle Wrapper `gradlew.bat`

## 默认 H5 地址

默认地址在 `app/src/main/res/values/strings.xml`:

```xml
<string name="default_web_url">https://java.app.gjjd168.com/</string>
```

Android 模拟器访问宿主机服务时使用 `10.0.2.2`。真机访问局域网服务时改成电脑局域网 IP，例如 `http://192.168.1.10:5173`。

## 构建

```powershell
cd apps/android-webview-demo
.\gradlew.bat :app:assembleDebug
```

## 安装和指定 H5 地址

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk

adb shell am start `
  -n com.example.webviewshelldemo/.MainActivity `
  --es web_url "https://java.app.gjjd168.com/"
```

临时测试服务器 IP 地址时，可用：

```powershell
adb shell am start `
  -n com.example.webviewshelldemo/.MainActivity `
  --es web_url "http://43.138.163.7:5173/"
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
