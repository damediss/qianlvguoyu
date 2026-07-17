# 三年规划 · rencaishibie 直写 Java 数据库方案与开发文档

> 文档版本：v1.1  
> 编写日期：2026-07-09  
> **实施状态：共库方案 B 已落地（2026-07-09）**  
> 关联文档：[培训管理三年规划与rencaishibie集成设计开发方案](./培训管理三年规划与rencaishibie集成设计开发方案.md)  
> 适用范围：`rencaishibie`（Python FastAPI）与 `qianlvguoyu_saas/platform-api`（Java）

---

## 实施清单（共库 B）

| 项 | 路径 |
|----|------|
| Flyway 建引擎表 | `platform-api/.../V20260709_001__create_rencai_analysis_tables.sql` |
| Python 同库同步 | `rencaishibie/backend/app/services/platform_sync_service.py` |
| 分析完成触发 | `rencaishibie/backend/app/routers/analysis.py`（`X-Tenant-Id` / `X-Plan-Id`） |
| 连接配置 | `rencaishibie/backend/.env` → `DATABASE_URL=.../gjjddb2026` |
| Java 补偿轮询 | `TrainThreeYearPlanApplicationService.applyRencaiStatusToEntity` |

---

## 一、结论摘要

| 问题 | 结论 |
|------|------|
| **技术上能否让 Python 直接写 Java 库？** | **可以**。Python 使用独立数据源连接 `gjjddb2026`，按约定写入 `train_three_year_plan*` 表。 |
| **是否推荐作为默认架构？** | **不推荐裸写库作为唯一通道**。Schema 由 Java Flyway 管理，审批/租户/权限在 Java，Python 直写会引入跨服务耦合与一致性风险。 |
| **推荐演进方向** | **方案 C（Push 回调 Java 内部 API，由 Java 落库）** 为主；若必须降延迟，可采用 **方案 B（Python 直写 + 白名单表 + 幂等）** 作为补充。 |

**一句话**：能直写，但应 **「Python 算结果 → Java 管单据与落库」**；直写仅作为性能优化选项，且必须限制写入范围与幂等规则。

---

## 二、现状与痛点

### 2.1 当前数据流（已实现）

```
admin-web
    │ POST /three-year-plans（上传 Excel）
    ▼
platform-api（Java）
    │ ① INSERT train_three_year_plan（status=1）
    │ ② HTTP → rencaishibie upload + run
    │ ③ 轮询 GET /api/analysis/{taskId}/status
    │ ④ 完成后 HTTP → GET /api/review/{taskId}/employees（分页拉取）
    │ ⑤ INSERT train_three_year_plan_item / matrix / distribution_snapshot
    │ ⑥ UPDATE status=2
    ▼
admin-web 详情页读本地库
```

### 2.2 已暴露的问题

| 现象 | 根因 |
|------|------|
| 分析完成但详情无数据 | rencaishibie 先标 `COMPLETED` 再入库 `analysis_result_item`，Java 同步时读到空集 |
| 进度 100% 但状态仍「分析中」 | Java 等本地落库完成才改 status=2，与 rencaishibie 进度语义不一致 |
| 详情首屏慢（10～30s） | 首次打开触发全量 HTTP 拉取 + 本地重建矩阵 |
| 双库维护 | `qualification_mvp`（引擎）+ `gjjddb2026`（业务）各存一份明细 |

### 2.3 目标

- 分析完成后 **详情秒开**，无需二次全量 HTTP 同步  
- 消除 **COMPLETED ↔ 明细入库** 竞态  
- 保持 Java 对 **租户、审批流、审计** 的单一管控  
- rencaishibie 仍可 **独立运行**（iframe 审核工作台）

---

## 三、方案对比

### 3.1 四种集成模式

| 方案 | 描述 | 详情打开速度 | 耦合度 | 推荐度 |
|------|------|-------------|--------|--------|
| **A. HTTP 拉取（现状）** | Java 轮询状态，完成后分页调 review API 写本地库 | 慢（首屏） | 低 | 已实现，适合 MVP |
| **B. Python 直写 Java 库** | 分析结束 Python 连 `gjjddb2026`，写 item/matrix/plan 状态 | 快 | **高** | 可选优化 |
| **C. Python Push → Java 内部 API** | 分析结束 Python 调 Java `/internal/.../sync-from-rencai`，Java 事务落库 | 快 | 中 | **首选** |
| **D. 共享 Outbox 表** | 两服务共写 `train_rencai_sync_outbox`，Java 消费 | 快 | 中偏高 | 大规模异步场景 |

### 3.2 架构示意

**方案 A（现状）**

```mermaid
sequenceDiagram
    participant Web as admin-web
    participant Java as platform-api
    participant Py as rencaishibie
    participant DbP as qualification_mvp
    participant DbJ as gjjddb2026

    Web->>Java: 创建三年规划
    Java->>DbJ: INSERT plan status=1
    Java->>Py: upload + run
    Py->>DbP: 分析 + 存结果
    loop 每 3s 轮询
        Java->>Py: GET status
        Java->>Py: GET employees（同步时）
        Java->>DbJ: INSERT items/matrix
    end
    Web->>Java: GET detail-bundle
    Java->>DbJ: 读本地库
```

**方案 C（推荐）**

```mermaid
sequenceDiagram
    participant Web as admin-web
    participant Java as platform-api
    participant Py as rencaishibie
    participant DbP as qualification_mvp
    participant DbJ as gjjddb2026

    Web->>Java: 创建三年规划
    Java->>DbJ: INSERT plan status=1
    Java->>Py: upload + run（带 X-Plan-Id / X-Tenant-Id）
    Py->>DbP: 分析 + 存 analysis_result_item
    Py->>Java: POST /internal/three-year-plans/{id}/ingest（taskId）
    Java->>DbP: 可选校验 task 状态
    Java->>DbJ: 事务写入 item/matrix/snapshot
    Java->>DbJ: UPDATE plan status=2
    Web->>Java: GET detail-bundle（秒开）
```

**方案 B（Python 直写 Java 库）**

```mermaid
sequenceDiagram
    participant Java as platform-api
    participant Py as rencaishibie
    participant DbP as qualification_mvp
    participant DbJ as gjjddb2026

    Java->>Py: upload + run
    Py->>DbP: 分析完成
    Py->>DbJ: 直写 item/matrix/plan.status
    Note over Py,DbJ: 绕过 Java 业务层，需严格白名单与幂等
    Java->>DbJ: 轮询仅读 plan 状态（或不再轮询）
```

---

## 四、方案 B 详解：Python 直写 Java 数据库

### 4.1 可行性

- Python 侧已有 `aiomysql` / SQLAlchemy 异步栈，新增第二个 `database_url_platform` 即可连接 `gjjddb2026`  
- Java 表结构已稳定：`train_three_year_plan`、`train_three_year_plan_item`、`train_three_year_plan_matrix`（及 `distribution_snapshot` 字段）  
- 创建任务时 Java 已通过 HTTP Header 传递 `X-Tenant-Id`、`X-Plan-Id`（见 `RencaishibieClient.applyHeaders`），Python 端可持久化到 `analysis_task` 扩展字段或内存上下文

### 4.2 允许写入表白名单（强制）

| 表 | Python 权限 | 说明 |
|----|------------|------|
| `train_three_year_plan` | **UPDATE 限定列** | 仅 `analysis_status`、`status`、`distribution_snapshot`、`update_time` |
| `train_three_year_plan_item` | INSERT / 物理删除后重建 | 按 `plan_id + tenant_id` 幂等重建 |
| `train_three_year_plan_matrix` | INSERT / 物理删除后重建 | 注意 `uk_typ_matrix_cell` 与软删冲突，须物理删 |
| `train_three_year_plan` 其他表 | **禁止** | 不碰 `sys_*`、审批、文件表 |

**禁止**：Python 创建规划单、改 `target_org_id`、改审批状态（3/4/5）、写 `reviewed_plan_file_id`。

### 4.3 写入时机（消除竞态）

在 `rencaishibie/backend/app/routers/analysis.py` 的 `_run()` 中，**必须**遵循：

```
1. engine 跑完 → read_analysis_results
2. INSERT analysis_result_item（qualification_mvp）并 commit
3. 调用 platform_sync_service.sync_plan_results(plan_id, tenant_id, task_id)
4. UPDATE analysis_task.status = COMPLETED 并 commit
5. SSE 推送 done
```

与当前已修复的顺序一致；第 3 步改为写 Java 库或调 Java API。

### 4.4 字段映射

| rencaishibie `analysis_result_item` | Java `train_three_year_plan_item` |
|---------------------------------------|-----------------------------------|
| `person_name` | `person_name` |
| `company` | `company_name` |
| `determined_job`（非空才同步） | `job_name` |
| `determined_level` | `level_name` |
| `eligible_date` | `eligible_date` + 解析 `plan_year` |
| `row_index` | `row_index` |
| — | `source` = `'engine'` |
| — | `tenant_id`、`plan_id` 来自 Header |

矩阵 `train_three_year_plan_matrix`：按 `(job_name, plan_year, train_mode, level_name)` 聚合 `headcount`（与 Java `syncMatrixFromItems` 一致）。

认定分布：序列化写入 `train_three_year_plan.distribution_snapshot`（JSON，与 Java `TrainThreeYearPlanDistributionVO` 结构一致）。

### 4.5 状态机（Python 写库时）

| 阶段 | `train_three_year_plan.status` | `analysis_status` | 写入方 |
|------|-------------------------------|-------------------|--------|
| Java 创建 | 1 | `running` | Java |
| 引擎运行中 | 1 | `running` | — |
| 明细写入 Java 中 | 1 | `syncing` | Python |
| 写入成功 | 2 | `done` | Python |
| 写入失败 | 1 | `failed` | Python 记日志，Java 可轮询补偿 |

### 4.6 Python 模块设计

```
rencaishibie/backend/app/
├── config.py                          # + platform_database_url
├── services/
│   ├── platform_sync_service.py       # 新增：直写 gjjddb2026
│   └── platform_sync_repository.py    # 原生 SQL / 第二 SQLAlchemy engine
└── routers/
    └── analysis.py                    # _run() 末尾调用 sync
```

**配置示例（`.env`）**

```env
# 现有引擎库
DATABASE_URL=mysql+aiomysql://user:pass@host:3308/qualification_mvp

# Java 业务库（仅 sync 模块使用）
PLATFORM_DATABASE_URL=mysql+aiomysql://sync_user:pass@host:3308/gjjddb2026
PLATFORM_SYNC_ENABLED=true
PLATFORM_SYNC_MODE=direct_db   # direct_db | callback_java
```

**数据库账号建议**

```sql
CREATE USER 'rencai_sync'@'%' IDENTIFIED BY '***';
GRANT SELECT, INSERT, UPDATE, DELETE ON gjjddb2026.train_three_year_plan_item TO 'rencai_sync'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON gjjddb2026.train_three_year_plan_matrix TO 'rencai_sync'@'%';
GRANT SELECT, UPDATE ON gjjddb2026.train_three_year_plan TO 'rencai_sync'@'%';
-- 不授予其他表
```

### 4.7 幂等与事务

```python
async def sync_plan_results(plan_id: int, tenant_id: int, task_id: int) -> None:
    async with platform_engine.begin() as conn:
        # 1. 校验 plan 存在且 status=1
        plan = await fetch_plan(conn, plan_id, tenant_id)
        if plan.status != 1:
            return  # 已同步或已进入审批，跳过

        # 2. 物理清理旧明细（软删会导致 matrix 唯一键冲突）
        await conn.execute(delete_items_sql, {"plan_id": plan_id, "tenant_id": tenant_id})
        await conn.execute(physical_delete_matrix_sql, {"plan_id": plan_id})

        # 3. 批量 INSERT items（从 qualification_mvp 读）
        items = await load_result_items(task_id)
        await bulk_insert_items(conn, plan_id, tenant_id, items)

        # 4. 重建 matrix + distribution_snapshot
        await rebuild_matrix(conn, plan_id, tenant_id)
        await update_distribution_snapshot(conn, plan_id, tenant_id, task_id)

        # 5. 更新主表（同一事务）
        await conn.execute(update_plan_sql, {
            "id": plan_id,
            "status": 2,
            "analysis_status": "done",
        })
```

- **幂等键**：`(plan_id, rencai_task_id)`；若 `plan.rencai_task_id != task_id` 则拒绝  
- **失败重试**：Java 保留 `POST /sync-from-rencai` 作补偿  
- **机构审核后变更**：用户在 rencaishibie 工作台调整后，仍通过 **finalize 或手动「同步数据」** 触发二次 sync

### 4.8 风险与规避

| 风险 | 规避 |
|------|------|
| Flyway 改表导致 Python SQL 失效 | 共享 `docs/schema/train_three_year_plan_sync.md` 契约；CI 契约测试 |
| Python 绕过租户隔离 | 必须带 `tenant_id`；SQL 全程 `AND tenant_id=?` |
| 双写不一致 | 以 Java 库为详情页唯一读源；Python 库仅引擎/审核用 |
| 矩阵唯一键与软删 | 重建矩阵时 **物理 DELETE**（Java 已采用 `physicalDeleteByPlan`） |
| 连接池占满 | 独立小连接池（max 5），仅 sync 时使用 |

---

## 五、方案 C 详解：Push Java 内部 API（推荐）

### 5.1 思路

Python **不持有** Java 库账号；分析完成后：

```http
POST /api/internal/training/three-year-plans/{planId}/ingest-rencai
Authorization: Bearer {INTERNAL_TOKEN}
X-Tenant-Id: 1
Content-Type: application/json

{
  "rencaiTaskId": 47,
  "event": "ANALYSIS_COMPLETED"
}
```

Java 端：

1. 校验 `INTERNAL_TOKEN` + `plan.rencai_task_id`  
2. 复用现有 `syncRencaiDataToLocal(entity)`（已有逻辑）  
3. 事务内 `status → 2`  
4. 返回 `{ itemCount, matrixCount }`

### 5.2 优点

- **Schema 变更只改 Java**，Python 无 SQL 耦合  
- **权限/审计/事务** 仍在 Java 一处  
- 实现量小于方案 B（Python 仅多一个 HTTP POST）  
- 与现有 `RencaishibieClient` 方向相反但对称  

### 5.3 Java 新增接口（建议）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/internal/training/three-year-plans/{id}/ingest-rencai` | 内网 token，触发落库 |
| GET | `/api/internal/training/three-year-plans/{id}/sync-status` | 可选，查 itemCount |

配置：

```yaml
platform:
  rencaishibie:
    internal-token: ${RENCAI_INTERNAL_TOKEN:}   # Python 回调 Java 时使用
  internal-api:
    enabled: true
    token: ${PLATFORM_INTERNAL_TOKEN:}          # 与上可共用
```

---

## 六、推荐路线与分期

### 6.1 分期计划

| 阶段 | 内容 | 工期（估） |
|------|------|-----------|
| **P0（已完成）** | HTTP 拉取 + 竞态修复 + 详情 bundle + 同步中状态 | — |
| **P1（推荐下一步）** | 方案 C：Python 完成后 POST Java ingest；Java 去掉列表轮询内重 sync | 2～3 人日 |
| **P2（可选）** | 方案 B：高并发场景 Python 直写 + Java 只读；保留 ingest 作补偿 | 3～5 人日 |
| **P3** | finalize 后再 sync；与培训方案 `train_solution` 联动 | 按原集成文档 |

### 6.2 决策建议

```
若优先「稳」与「少维护」 → 方案 C
若优先「极致性能、可接受双库账号」 → 方案 B + C 补偿
若短期不改造 → 维持方案 A + 已做竞态修复
```

---

## 七、开发任务清单（方案 C，推荐实施）

### 7.1 Java（platform-api）

- [ ] 新增 `InternalThreeYearPlanController`（`/api/internal/...`，`@PreAuthorize` 或 Filter 校验 token）  
- [ ] `ingestRencaiResult(planId, IngestReq)`：调用 `syncRencaiDataToLocal` + `status=2`  
- [ ] 幂等：已 `status>=2` 且 `itemCount>0` 时直接返回成功  
- [ ] 日志：`AuditLog` 记录 `rencai-ingest`  
- [ ] 配置项：`platform.internal-api.token`  
- [ ] 单元测试：ingest 成功 / 重复调用 / taskId 不匹配  

### 7.2 Python（rencaishibie）

- [ ] `analysis.py` `_run()`：在 `analysis_result_item` commit 之后调用 `notify_platform_ingest(plan_id, tenant_id, task_id)`  
- [ ] 新建 `platform_callback_client.py`：HTTP POST Java ingest，失败写日志 + 可选 Celery 重试  
- [ ] 持久化 `tenant_id` / `plan_id`：upload/run 时从 Header 写入 `analysis_task` 扩展列（若无则新增 migration）  
- [ ] 配置：`PLATFORM_API_BASE_URL`、`PLATFORM_INTERNAL_TOKEN`  

### 7.3 前端（admin-web）

- [ ] 列表轮询：存在 `analysis_status=syncing` 时继续 3s 刷新（已部分支持）  
- [ ] 详情页：ingest 完成后 bundle 直读本地，可去掉自动 10 次重试（P1 完成后）  

### 7.4 运维

- [ ] 内网 token 配置进 K8s Secret / `.env`  
- [ ] 监控：ingest 失败率、sync 耗时、itemCount=0 且 recommended>0 告警  

---

## 八、开发任务清单（方案 B，直写 Java 库）

若团队选择直写，在方案 C 之外 **或代替 C** 增加：

### 8.1 Python

- [ ] `config.py` 增加 `platform_database_url`、`platform_sync_enabled`  
- [ ] `platform_sync_service.py`：§4.7 事务流程  
- [ ] `platform_sync_repository.py`：白名单 SQL  
- [ ] 矩阵物理删除 SQL 与 Java `TrainThreeYearPlanMatrixMapper.physicalDeleteByPlan` 行为一致  
- [ ] `distribution_snapshot` JSON 结构与 Java `TrainThreeYearPlanDistributionVO` 对齐  
- [ ] 集成测试：跑完分析后查 `gjjddb2026` item 行数 = rencai recommended  

### 8.2 Java

- [ ] 列表/详情 **不再** 在 `page()` 里调用 `syncRencaiDataToLocal`（改由 Python 写库）  
- [ ] 保留 `POST /sync-from-rencai` 人工补偿  
- [ ] Flyway 文档化：sync 相关列变更需通知 Python  

### 8.3 DBA

- [ ] 创建 `rencai_sync` 受限账号（§4.6）  
- [ ] 审计：可选 trigger 记录 `train_three_year_plan` 的 status 变更  

---

## 九、测试用例

| 编号 | 场景 | 预期 |
|------|------|------|
| T1 | 上传 Excel → 分析完成 → 不打开详情 | 列表 status=2，item 已有数据 |
| T2 | 分析完成瞬间点详情 | 明细非空，无「同步中」超过 30s |
| T3 | Python ingest/直写失败 | status 保持 1，`analysis_status=failed` 或继续 syncing；手动「同步数据」可恢复 |
| T4 | 重复 ingest / 重复直写 | 幂等，item 不翻倍 |
| T5 | 机构在 rencaishibie 改明细后 finalize | Java 二次 sync 后本地与审核结果一致 |
| T6 | 多租户 | plan_id=1 tenant=1 不能写入 tenant=2 |

---

## 十、与现有代码对照

| 能力 | 现有实现 | 方案 C 变更 | 方案 B 变更 |
|------|----------|-------------|-------------|
| 创建规划 | `TrainThreeYearPlanApplicationService.createWithFile` | 不变 | 不变 |
| 状态轮询 | `syncAnalysisStatusFromRencai` in `page()` | 仅处理失败/超时 | 可弱化 |
| 落库 | `syncRencaiDataToLocal` | 由 ingest 触发 | 迁至 Python |
| 详情 | `getDetailBundle` + 本地读 | 不变 | 不变 |
| Header | `X-Tenant-Id` / `X-Plan-Id` | Python 回调带回 | Python 写库使用 |

---

## 十一、FAQ

**Q1：能否让 Python 完全接管 `train_three_year_plan` 全流程？**  
不建议。创建、审批、企业确认、文件归档应留在 Java，Python 只负责 **分析结果 materialize**。

**Q2：两个 MySQL 库能否合并成一个？**  
长期可以，但 rencaishibie 历史表与 Java Flyway 生命周期不同，合并前需统一 migration 策略；短期保持双库 + sync 成本更低。

**Q3：直写后还要保留 HTTP review API 吗？**  
要。iframe 审核工作台仍读 `qualification_mvp`；finalize 后需再 sync 一次到 Java。

**Q4：生产环境网络？**  
Python → Java ingest 走内网；若直写库，MySQL 需对 rencaishibie 节点开放 3306/3308，建议只读从库 **不可** 用于写入。

---

## 十二、修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-07-09 | 初稿：直写可行性、方案 B/C 对比、开发清单 |
