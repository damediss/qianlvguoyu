param(
    [string]$TaskId = "2069972938644135937",
    [string]$Device = "",
    [string]$LocalHost = "192.168.1.98",
    [int]$WebPort = 5173,
    [int]$ApiPort = 8080,
    [switch]$LocalTest,
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$LogcatOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Flavor = if ($LocalTest) { "localTest" } else { "prod" }
$ApkPath = Join-Path $Root "app\build\outputs\apk\$Flavor\debug\app-$Flavor-debug.apk"
$PackageName = if ($LocalTest) { "com.example.webviewshelldemo.localtest" } else { "com.example.webviewshelldemo" }
$WebUrl = if ($LocalTest) {
    "http://${LocalHost}:$WebPort/practical-collection/workbench/$TaskId"
} else {
    "http://43.138.163.7:$WebPort/practical-collection/workbench/$TaskId"
}

function Get-AdbTarget {
    param([string]$PreferredDevice)
    if ($PreferredDevice) {
        return $PreferredDevice
    }
    $lines = @(& adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" })
    if ($lines.Count -eq 0) {
        throw "No authorized adb device found."
    }
    if ($lines.Count -gt 1) {
        throw "Multiple devices found. Pass -Device with serial number."
    }
    return ($lines[0] -split "\t")[0]
}

function Invoke-Adb {
    param([string]$Target, [string[]]$CommandArgs)
    & adb -s $Target @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw ("adb failed: adb -s {0} {1}" -f $Target, ($CommandArgs -join ' '))
    }
}

$adbTarget = Get-AdbTarget -PreferredDevice $Device
Write-Host "Device: $adbTarget"

Write-Host "Setting adb reverse..."
Invoke-Adb $adbTarget @("reverse", "tcp:$WebPort", "tcp:$WebPort")
Invoke-Adb $adbTarget @("reverse", "tcp:$ApiPort", "tcp:$ApiPort")
& adb -s $adbTarget reverse --list

if ($LogcatOnly) {
    Write-Host "Starting scan logcat (Ctrl+C to stop)..."
    & adb -s $adbTarget logcat -c
    & adb -s $adbTarget logcat -s AndroidBridge:* QrCodeScanHelper:*
    exit 0
}

if (-not $SkipBuild) {
    Write-Host "Building $Flavor debug APK..."
    Push-Location $Root
    try {
        $gradleTask = if ($LocalTest) { ":app:assembleLocalTestDebug" } else { ":app:assembleProdDebug" }
        & .\gradlew.bat $gradleTask
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }
    } finally {
        Pop-Location
    }
}

if (-not $SkipInstall) {
    Write-Host "Installing APK..."
    Invoke-Adb $adbTarget @("install", "-r", $ApkPath)
}

Write-Host "Launching WebView shell ($PackageName)..."
Write-Host "URL: $WebUrl"
Invoke-Adb $adbTarget @("shell", "am", "force-stop", $PackageName)
Invoke-Adb $adbTarget @(
    "shell", "am", "start",
    "-n", "$PackageName/.MainActivity",
    "--es", "web_url", $WebUrl
)

Write-Host ""
Write-Host "Wired debug ready."
Write-Host "1. Keep admin-web dev server running on port $WebPort"
Write-Host "2. Keep platform-api running on port $ApiPort"
Write-Host "3. Open chrome://inspect/#devices and inspect WebView"
Write-Host "4. Console: typeof AndroidBridge?.scanQRCode"
Write-Host "5. Tap scan button on tablet"
Write-Host ""
Write-Host "Logcat: .\wired-debug.ps1 -LogcatOnly"
Write-Host "Relaunch: .\wired-debug.ps1 -SkipBuild -SkipInstall -TaskId YOUR_TASK_ID"
