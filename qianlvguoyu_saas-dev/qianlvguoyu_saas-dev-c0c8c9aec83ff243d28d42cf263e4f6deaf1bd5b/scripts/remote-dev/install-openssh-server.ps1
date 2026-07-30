# 以管理员身份运行此脚本，安装并配置 OpenSSH Server（供家里 Cursor SSH 连接）
#Requires -RunAsAdministrator

$ErrorActionPreference = "Stop"

Write-Host "=== OpenSSH Server 安装脚本 ===" -ForegroundColor Cyan

$cap = Get-WindowsCapability -Online | Where-Object Name -like "OpenSSH.Server*"
if ($cap.State -ne "Installed") {
    Write-Host "正在安装 OpenSSH Server..."
    Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
} else {
    Write-Host "[OK] OpenSSH Server 已安装"
}

Start-Service sshd
Set-Service -Name sshd -StartupType Automatic

$sshDir = "$env:USERPROFILE\.ssh"
$authKeys = "$sshDir\authorized_keys"
New-Item -ItemType Directory -Force -Path $sshDir | Out-Null

if (-not (Test-Path $authKeys)) {
    New-Item -ItemType File -Path $authKeys -Force | Out-Null
}

Write-Host ""
Write-Host "请将家里电脑的公钥追加到: $authKeys" -ForegroundColor Yellow
Write-Host "家里电脑执行: Get-Content `$env:USERPROFILE\.ssh\id_ed25519.pub" -ForegroundColor Yellow
Write-Host ""

icacls $authKeys /inheritance:r /grant "Administrators:F" /grant "$($env:USERNAME):F" | Out-Null

Get-NetFirewallRule -Name *OpenSSH-Server* -ErrorAction SilentlyContinue |
    Select-Object Name, Enabled, Direction | Format-Table -AutoSize

Get-Service sshd | Format-Table Name, Status, StartType -AutoSize

Write-Host "[完成] 连 VPN 后从家里测试: ssh $env:USERNAME@<公司内网IP>" -ForegroundColor Green
