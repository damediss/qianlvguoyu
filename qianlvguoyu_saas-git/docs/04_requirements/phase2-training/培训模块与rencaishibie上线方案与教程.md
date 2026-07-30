# 培训模块 × rencaishibie 上线方案与操作教程

> 文档版本：v1.0  
> 编写日期：2026-07-14  
> 适用范围：将近期开发的培训能力（企业调研/人员采集查证/方案规划/人员认定等）与 rencaishibie 分析引擎一并上线  
> 关联仓库：`qianlvguoyu_saas`（Java platform-api + admin-web）、`rencaishibie`（Python FastAPI + Vue）  
> 关联部署文档：`qianlvguoyu_saas/deploy/README.md`、`rencaishibie/局域网部署说明.md`

---

## 一、上线目标与范围

### 1.1 目标

保证生产（或公网测试）环境中：

1. **培训管理后台**可登录使用：企业调研、人员采集与查证、方案规划（三年规划）、人员认定 Excel 查证回填等  
2. **rencaishibie** 可被 Java 正确调用：上传名单 → 分析 → 共库写入三年规划表  
3. 独立打开 rencaishibie Web / CLI 脚本时，分析与下载功能可用  

### 1.2 本期重点功能清单

| 模块 | 入口 / 接口 | 上线依赖 |
|------|-------------|----------|
| 企业调研 | 菜单 `pxyan_jiu` | Flyway + 权限 + `sys_org` |
| 人员采集 / 查证 | 调研详情抽屉；`POST .../collection/people/verify-cert` | `sys_dict` 查证配置；`cert_verified` 迁移 |
| 方案规划 · 三年规划 | 菜单 `pxan_fang_gui_hua` | `RENCAI_BASE_URL`；共库引擎表；Python `PLATFORM_SYNC_ENABLED` |
| 方案规划 · 人员认定 | Tab「人员认定」；`POST .../personnel-cert/verify-lookup` | 同上查证配置；前端已构建发布 |
| rencaishibie 引擎 | `:8001` 或经 nginx `/rencai-api` | 共库 `gjjddb2026`；运行目录与资源文件 |

### 1.3 架构关系（必须遵守）

```
浏览器
  ├─ admin-web (nginx 静态) ──/api──► platform-api :8080
  │                                      │
  │                                      ├─ MySQL gjjddb2026 (Flyway 管 DDL)
  │                                      └─ HTTP ──► rencaishibie :8001
  │                                                      │
  └─ (可选) /rencai/ 审核台 ─────────────────────────────┘
                                                     同库 DML
                                                     train_three_year_plan*
                                                     analysis_*
```

**硬性约定：**

- Java 与 Python **共库** `gjjddb2026`  
- **仅 Java Flyway** 建/改表；**禁止**对生产库执行 rencaishibie Alembic  
- 分析完成写三年规划表：Python `platform_sync_service`（需请求带 `X-Tenant-Id` + `X-Plan-Id`）  
- 查证（采集抽屉 + 人员认定）：读租户 `sys_dict` 的 `chazheng_url` / `chazheng_datas`

---

## 二、上线前检查清单（不做完勿发版）

### 2.1 代码与分支

- [ ] `qianlvguoyu_saas` 目标分支（如 `dev`/`release`）已包含：查证接口、`personnel-cert/verify-lookup`、企业调研/三年规划相关后端  
- [ ] 前端含：`EnterpriseResearchPage`、`ResearchCollectionDrawer`、`SolutionPlanningPage`、`PersonnelCertImportPanel` 等已对齐 API  
- [ ] `rencaishibie` 含 `platform_sync_service`、`.env` 中 `PLATFORM_SYNC_ENABLED=true`  
- [ ] JDK **21** 构建 platform-api；Node 用于 admin-web / rencaishibie web build  

### 2.2 基础设施

- [ ] MySQL 可达（示例：`43.138.163.7:3308` / 库 `gjjddb2026`），账号具备 DML + Flyway 所需权限  
- [ ] Redis 可达（platform-api 依赖）  
- [ ] 文件存储：COS 或本地 `CDN_PATH` / `FILE_LOCAL_ROOT` 已配  
- [ ] 服务器可互通：`platform-api → rencaishibie:8001`（内网优先）  

### 2.3 数据库迁移（Java 启动即跑）

确认 Flyway 历史中至少含（名称以仓库为准）：

| 版本/脚本关键词 | 作用 |
|-----------------|------|
| `V20260709_001` | 建 rencaishibie 引擎表 `analysis_*` 等 |
| `V20260708_*` / `V20260709_*` | 三年规划表、调研企业字段、菜单权限 |
| `V20260709_008` | `sys_dict.value` 列（查证模板） |
| `V20260714_001` | `train_sign_up_people_self.cert_verified` |

首次上线或新环境：拉齐代码后启动 platform-api，观察日志无 Flyway 失败。

### 2.4 业务配置（手工，迁移不会自动配齐）

| 配置 | 说明 |
|------|------|
| `sys_dict.code = chazheng_url` | 第三方查证 API 地址（租户维度） |
| `sys_dict.code = chazheng_datas` | 查证请求模板 JSON 数组 |
| 菜单角色授权 | `pxyanjiu:*`、`pxanfangguihua:*`、`train:research_org:*` 等已赋给运营/平台角色 |
| `sys_org` | 企业调研「关联系统企业」有可用组织数据 |
| 采集模板文件 | `高命中率认定采集模板精简版.xlsx` 位于 classpath 或 CDN 约定路径 |
| rencaishibie 资源 | 「职业大典」等引擎依赖文件在服务器可访问路径 |

### 2.5 环境变量（两端对齐）

**platform-api（示例 `/data/guoyu/.env`）新增/核对：**

```bash
# 必填：指向生产 rencaishibie
RENCAI_ENABLED=true
RENCAI_BASE_URL=http://127.0.0.1:8001
# 若启用内部鉴权则两端一致
# RENCAI_INTERNAL_TOKEN=your_shared_token

MYSQL_URL=jdbc:mysql://43.138.163.7:3308/gjjddb2026?...
MYSQL_USERNAME=gjjd
MYSQL_PASSWORD=********

# 采集 H5 / 文件（按实际环境）
H5_BASE_URL=https://your-domain/h5
CDN_PATH=/data/guoyu/storage/cdn
```

**rencaishibie `backend/.env`：**

```bash
DATABASE_URL=mysql+aiomysql://gjjd:********@43.138.163.7:3308/gjjddb2026
PLATFORM_SYNC_ENABLED=true
# 跨域前端若非 localhost，需扩展 config.cors_origins
```

**admin-web 构建期（可选）：**

```bash
VITE_API_PROXY_TARGET=   # 构建产物走 nginx /api，一般无需代理
VITE_H5_BASE_URL=https://your-domain/h5
```

**nginx（与现网一致时建议补齐）：**

| location | 上游 |
|----------|------|
| `/api/` | `127.0.0.1:8080` |
| `/rencai-api/` | `127.0.0.1:8001`（路径按现网约定改写） |
| `/rencai/` | rencaishibie 静态或反代 |
| `/` | admin-web `dist` |

> 现网若尚未配置 `/rencai*`，至少保证 **Java 能内网访问 8001**，方案规划自动分析即可；独立审核台 URL 再逐步开放。

---

## 三、上线方案（推荐顺序）

```
T0 冻结版本 → T1 备份库 → T2 先启/核 rencaishibie
 → T3 发 platform-api（Flyway）→ T4 发 admin-web
 → T5 配置字典与权限 → T6 冒烟 → T7 放开用户
```

| 阶段 | 动作 | 回滚策略 |
|------|------|----------|
| T0 | 打 tag / 记录 commit | 可切回上一 tag |
| T1 | 备份 `gjjddb2026` | 库还原 |
| T2 | 部署并健康检查 rencaishibie | 停 uvicorn 不影响已落库数据 |
| T3 | 部署 platform-api，确认 Flyway 成功 | 回退 jar + 谨慎回滚 migration |
| T4 | 部署 admin-web dist | 回退上一版静态资源 |
| T5 | 配 `chazheng_*`、核菜单角色 | 改 dict / 角色即可 |
| T6 | 按第六章冒烟 | 失败则阻断业务放量 |
| T7 | 通知用户 | — |

**原则：先保证 Python 可达且共库正确，再发 Java；避免 Java 已发版但 `RENCAI_BASE_URL` 仍指向本机无效地址。**

---

## 四、上线教程（操作步骤）

### 4.1 备份数据库

```bash
mysqldump -h 43.138.163.7 -P 3308 -u gjjd -p gjjddb2026 \
  --single-transaction --routines --triggers \
  > backup_gjjddb2026_$(date +%Y%m%d_%H%M).sql
```

### 4.2 部署 / 重启 rencaishibie

在分析服务器（可与 Java 同机或内网机）：

```powershell
cd D:\phpApp\rencaishibie\backend
# 确认 .env：DATABASE_URL + PLATFORM_SYNC_ENABLED=true
pip install -r requirements.txt

cd ..\web
npm install
npm run build

cd ..\backend
# 仅保留一个进程
uvicorn app.main:app --host 0.0.0.0 --port 8001 --timeout-keep-alive 30
```

**验收：**

```bash
curl -s http://127.0.0.1:8001/api/health
# 期望含 ok / healthy 类响应
```

防火墙放行 `8001`（若跨机）。  
运行目录留出磁盘：`backend/runtime/uploads|outputs|logs`。

**可选 systemd（Linux 示例）：**

```ini
[Unit]
Description=rencaishibie
After=network.target

[Service]
WorkingDirectory=/data/rencaishibie/backend
EnvironmentFile=/data/rencaishibie/backend/.env
ExecStart=/usr/bin/uvicorn app.main:app --host 0.0.0.0 --port 8001 --timeout-keep-alive 30
Restart=always
User=guoyu

[Install]
WantedBy=multi-user.target
```

### 4.3 部署 platform-api

参考 `deploy/README.md` / `deploy-public.sh`，或手工：

```bash
cd /path/to/qianlvguoyu_saas/services/platform-api
# JAVA_HOME = JDK 21
./mvnw -DskipTests package

# 更新 jar + .env（写入 RENCAI_BASE_URL）后
sudo systemctl restart platform-api
sudo journalctl -u platform-api -n 200 --no-pager
```

**务必在日志中确认：**

1. Flyway migrate 成功（含 `V20260709_001`、`V20260714_001` 等）  
2. 启动完成无 Fatal  
3. `curl http://127.0.0.1:8080/api/actuator/health` → `UP`

### 4.4 部署 admin-web

```bash
cd /path/to/qianlvguoyu_saas/apps/admin-web
npm ci   # 或 npm install
npm run build
# 将 dist/ 同步到 nginx 根目录，如 /data/guoyu/web/
sudo nginx -t && sudo systemctl reload nginx
```

### 4.5 配置查证字典（每个要用的租户执行一次）

在管理端「字典」或直接 SQL（示例，按实际表结构改）：

```sql
-- 查证 URL（示例）
INSERT INTO sys_dict (tenant_id, code, value, /* 其它必填列 */)
VALUES (1, 'chazheng_url', 'https://第三方查证地址', ...);

-- 查证模板（JSON 数组，字段与现网 chazheng 一致）
INSERT INTO sys_dict (tenant_id, code, value, ...)
VALUES (1, 'chazheng_datas', '[{...}]', ...);
```

保存后重启非必须（一般读库）；若有缓存再刷新。

### 4.6 配置 Nginx 反代（建议）

在现有 `deploy/nginx.conf` 基础上增加（路径按网关设计微调）：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_read_timeout 300s;
}

# 审核台 / 独立 rencaishibie（可选）
location /rencai-api/ {
    proxy_pass http://127.0.0.1:8001/api/;
    proxy_read_timeout 300s;
}
location /rencai/ {
    proxy_pass http://127.0.0.1:8001/rencai/;
}
```

将 `platform-api` 的 `RENCAI_BASE_URL` 设为 **Java 服务器能访问到的地址**（同机用 `http://127.0.0.1:8001` 最稳）。

---

## 五、rencaishibie「脚本」如何保证可用

| 用法 | 命令 / 方式 | 上线注意 |
|------|-------------|----------|
| 服务内引擎 | Java 调 `/api/analysis/run` → 进程内 import `qualification_analyzer` | 保持 `python/` 与 `backend` 同仓部署 |
| CLI | `cd rencaishibie/python && python qualification_analyzer.py -i in.xlsx -o out.xlsx ...` | 服务器装齐 openpyxl 等；配置职业大典路径 |
| GUI | `python qualification_analyzer_gui.py` | 仅有桌面环境的运维机可用 |
| Web 独立站 | 浏览器打开 `:8001`（build 后） | 无 `X-Plan-Id` 时**不会**写三年规划表（符合设计） |

**共库写三年规划仅当：** `PLATFORM_SYNC_ENABLED=true` **且** 请求带 `X-Tenant-Id` + `X-Plan-Id`（由 Java `RencaishibieClient` 在创建规划分析时自动带上）。

---

## 六、冒烟验收（上线必做）

### 6.1 基础

| # | 步骤 | 期望 |
|---|------|------|
| 1 | 打开后台登录 | 菜单含企业调研、方案规划 |
| 2 | `GET /api/actuator/health` | UP |
| 3 | `GET rencaishibie /api/health` | 正常 |

### 6.2 企业调研 → 采集 → 查证

| # | 步骤 | 期望 |
|---|------|------|
| 1 | 新增/编辑调研企业，关联系统企业 | 保存成功 |
| 2 | 人员采集 → 启采 → 录入/导入人员 | 列表有人 |
| 3 | 勾选人员点「查证」 | 回填证书或「无结果」；非「查证接口未配置」 |
| 4 | 导出精简版 Excel | 文件可下载 |

### 6.3 三年规划 × rencaishibie

| # | 步骤 | 期望 |
|---|------|------|
| 1 | 方案规划「新建三年规划」并上传名单 / 或从采集创建 | `status` 进入分析中 |
| 2 | 等待分析完成 | `status=2`（分析完成请审核） |
| 3 | 查库 `train_three_year_plan_item` | 有明细（Python 共库 sync） |
| 4 | 打开详情：认定分布 / 矩阵 / 人员明细 | 有数据 |
| 5 | 「审核工作台」可打开 | 能进 rencaishibie 审核页（若已配反代） |

### 6.4 人员认定 Tab

| # | 步骤 | 期望 |
|---|------|------|
| 1 | 上传采集模板 Excel | 解析出行数 |
| 2 | 开始查证回填 | 真实查证（非 Mock）；可下载回填文件 |
| 3 | 日期列 | 不为 Excel 序列号异常显示（若已合前端修复） |

### 6.5 负例（快速兜底）

| 场景 | 期望表现 |
|------|----------|
| 停掉 rencaishibie 再新建规划分析 | Java 报错明确，不拖垮整个后台 |
| 清空 `chazheng_url` 再查证 | 提示未配置，不出现 500 堆栈到前端 |
| 未带 PlanId 独立跑分析 | 只写 `analysis_*`，不覆盖他人三年规划 |

---

## 七、运维与常见问题

| 现象 | 排查 |
|------|------|
| 分析一直「分析中」 | `RENCAI_BASE_URL` 是否通；uvicorn 是否存活；任务是否缺 `X-Plan-Id`；看 Python 日志 `[WARN] platform sync failed` |
| 分析完成但详情无明细 | 共库账号是否同一库；`PLATFORM_SYNC_ENABLED`；Flyway 引擎表是否存在；Python sync 是否因 `status>=2` 跳过 |
| 查证提示未配置 | 租户下 `sys_dict` 是否有 `chazheng_url` / `chazheng_datas` |
| 前端菜单没有入口 | 角色是否授了 `pxyanjiu:view` / `pxanfangguihua:view` 等；清缓存重新登录 |
| Flyway 失败 | 看失败版本；勿手搓改表后校验不一致；可用既有 repair 流程（谨慎） |
| 多人假死 | rencaishibie **只起一个** uvicorn；加大 `proxy_read_timeout` |

**日志位置建议：**

- platform-api：`journalctl -u platform-api` / 应用日志目录  
- rencaishibie：`backend/runtime/logs` + 控制台  
- 查证：`chazheng.log-dir`（默认 `./logs/chazheng`）

---

## 八、回滚方案

1. **前端：** 还原上一版 `dist`，`nginx reload`  
2. **Java：** 还原上一 jar，`systemctl restart`（注意 Flyway **一般不自动降级**；破坏性迁移需提前评估）  
3. **rencaishibie：** 停服务或回退代码重启；库中 `analysis_*` / 已同步的 `train_three_year_plan*` 按备份决定是否还原  
4. **紧急隔离：** `RENCAI_ENABLED=false` 可暂时关闭三年规划外呼（其它培训功能仍可用，视代码开关行为）

---

## 九、上线日建议排期（半天模板）

| 时间 | 事项 |
|------|------|
| 09:00 | 冻结代码、备份库 |
| 09:30 | 发 rencaishibie + health |
| 10:00 | 发 platform-api，盯 Flyway |
| 10:30 | 发 admin-web |
| 10:45 | 配字典 / 抽查权限 |
| 11:00–12:00 | 第六章全量冒烟 |
| 12:00 | 签署上线结论 / 通知业务 |

---

## 十、交付物与责任边界

| 角色 | 责任 |
|------|------|
| 后端 | jar、Flyway、`.env`（含 `RENCAI_*`）、字典 SQL |
| 前端 | admin-web 构建产物与菜单联调 |
| 算法/识别 | rencaishibie 进程、共库 `.env`、引擎资源文件 |
| 运维 | 备份、nginx、防火墙、systemd、磁盘监控 |

---

## 十一、结论（怎么做才算「可以上线」）

1. **同一 MySQL 共库**就绪，Java Flyway 已建引擎表与培训表  
2. **rencaishibie 常驻 8001**，`PLATFORM_SYNC_ENABLED=true`  
3. **platform-api** `RENCAI_BASE_URL` 指向该服务，并用 JDK 21 发布  
4. **admin-web** 新前端已发布  
5. **每个租户**配好查证 `sys_dict`  
6. **冒烟**通过：采集查证、三年规划自动落库、人员认定回填  

满足以上六条，即可视为培训模块 + rencaishibie 具备上线条件。
