# 远程开发落地状态（本机实测）

> 评估日期：2026-06-08  
> 主机：Windows 11 · 用户 `pc` · 局域网 IP `192.168.1.98`

## 与方案文档的差异（已适配）

| 方案假设 | 本机实际 | 处理 |
|---------|---------|------|
| 项目根 `D:\qianlvguoyu` | 实际代码在 `...\qianlvguoyu_saas-dev\...\` 子目录 | 所有配置已改为真实路径 |
| Maven 多模块 `-pl order-service` | 仅 `services/platform-api` 独立 pom | Skill/规则已改为 `platform-api` |
| 配置目录 `%USERPROFILE%\.hermes\` | Windows 原生为 `%LOCALAPPDATA%\hermes\` | Skill/.env 已写入正确目录 |
| Git 仓库 + develop 分支 | 项目根无 `.git` | 需后续 `git init` 或 clone 正式仓库 |
| 内网 Nexus settings.xml | 不存在，Maven 走公网 | 公司环境需补 settings.xml |
| OpenSSH Server | 未安装，当前无管理员权限 | 已提供管理员安装脚本 |

## Phase 完成度

| Phase | 状态 | 说明 |
|-------|------|------|
| A Java/Maven 环境 | ✅ | JDK 21、Maven 3.9.11、Docker(MySQL/Redis) 正常；`mvn test` 通过 |
| B Hermes 安装 | ✅ | v0.16.0（PyPI），已加入 User PATH |
| C 飞书 Gateway | ⏳ 待你操作 | 需 `hermes setup` + `hermes gateway setup` 扫码 |
| D java-cloud-dev Skill | ✅ | `%LOCALAPPDATA%\hermes\skills\dev\java-cloud-dev\` |
| E OpenSSH Server | ⏳ 需管理员 | 运行 `scripts\remote-dev\install-openssh-server.ps1` |
| F Cursor 规则 | ✅ | `.cursor\rules\java-cloud.mdc` |
| G 家里 Cursor SSH | ⏳ 待 E 完成后 | 见 `ssh-config-home.example` |

## 你需要手动完成的 3 步

### 1. 配置 LLM + Hermes（约 5 分钟）

```powershell
# 新开 PowerShell（使 PATH 生效）
hermes setup
hermes doctor
```

### 2. 配置飞书 Bot（约 15 分钟）

```powershell
hermes gateway setup
# 扫码授权后，确认 %LOCALAPPDATA%\hermes\.env 中有 FEISHU_ALLOWED_USERS
hermes gateway start          # 前台试跑
hermes gateway install        # 登录自启
```

手机飞书测试：

```text
cd /d/qianlvguoyu/qianlvguoyu_saas-dev/qianlvguoyu_saas-dev-c0c8c9aec83ff243d28d42cf263e4f6deaf1bd5b && git status && mvn -v
```

### 3. OpenSSH（需管理员 PowerShell）

```powershell
cd D:\qianlvguoyu\qianlvguoyu_saas-dev\qianlvguoyu_saas-dev-c0c8c9aec83ff243d28d42cf263e4f6deaf1bd5b\scripts\remote-dev
.\install-openssh-server.ps1
```

将本机已有公钥写入 `authorized_keys`（家里 Cursor 连本机时用）：

```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIJrz3+UDJXlLLRfzVOW+Rh6dWMmkVLmlWOdJ/JSTBP7S 2505803782@qq.com
```

## 关键路径速查

```
项目根（Windows）:
D:\qianlvguoyu\qianlvguoyu_saas-dev\qianlvguoyu_saas-dev-c0c8c9aec83ff243d28d42cf263e4f6deaf1bd5b

项目根（Git Bash / 飞书 Hermes）:
/d/qianlvguoyu/qianlvguoyu_saas-dev/qianlvguoyu_saas-dev-c0c8c9aec83ff243d28d42cf263e4f6deaf1bd5b

Hermes 配置:
C:\Users\pc\AppData\Local\hermes\.env

Maven 测试:
cd services\platform-api && mvn clean test
```
