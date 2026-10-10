# 培训管理「方案规划·三年规划」× rencaishibie 集成设计开发方案

> 文档版本：v1.0  
> 编写日期：2026-07-08  
> 适用范围：qianlvguoyu_saas（Java platform-api + admin-web）与 rencaishibie（Python FastAPI）  
> 延伸阅读：[三年规划 rencaishibie 直写 Java 库方案与开发文档](./三年规划rencaishibie直写Java库方案与开发文档.md)（分析结果落库优化）

---

## 一、可行性结论

**可以实现**，且与现有系统架构匹配度高，但需要做一层「业务编排 + 数据桥接」，而不是让 Java 与 Python 直接共享业务逻辑或强耦合数据库。

| 维度 | 现状 | 结论 |
|------|------|------|
| rencaishibie 能力 | 已具备：Excel 导入 → 人才识别 → 自动生成三年规划表 → 审核工作台 → finalize | **核心算法无需重写** |
| Java 培训模块 | 已有完整培训流水线、企业确认/方案审批模式 | **审批流可复用** |
| 三年规划前端 | `SolutionPlanningPage.tsx` 仅为占位页 | **需新建业务页** |
| 人才识别入口 | iframe 嵌入 `/rencai/`（端口 8001） | **已有 UI 集成，缺业务联动** |
| 数据库 | Java 用 `gjjddb2026`，rencaishibie **共库**（`V20260709_001` 建引擎表） | **已实施共库**；Python 分析完成直写 `train_three_year_plan*` |

---

## 二、现状架构梳理

### 2.1 系统拓扑

```
┌─────────────────────────────────────────────────────────────┐
│  qianlvguoyu_saas（Java 单体 platform-api）                  │
│  admin-web React  →  platform-api :8080 /api  →  gjjddb2026 │
└─────────────────────────────────────────────────────────────┘
                              │
                    iframe /rencai/（仅 UI 嵌入）
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  rencaishibie（Python FastAPI）                              │
│  Vue3 web  →  FastAPI :8001 /api  →  qualification_mvp     │
│              qualification_analyzer.py（核心引擎）            │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 关键缺口

1. **三年规划**在 Java 侧无实体、无 API（`SolutionPlanningPage` 仅占位）。
2. **rencaishibie** 与 Java **无 API 级联动**，各自独立库。
3. **企业审核**在培训模块已有成熟模式，可直接借鉴：
   - 机构内审：`train_solution` 的 `submit → approve/reject`（status 0→1→2）
   - 企业确认：`train_info_list` 的 `company-confirm / company-reject`

### 2.3 相关代码路径

| 模块 | 路径 |
|------|------|
| 三年规划占位页 | `apps/admin-web/src/pages/training/SolutionPlanningPage.tsx` |
| 人才识别嵌入页 | `apps/admin-web/src/pages/knowledge/RencaishibieEmbedPage.tsx` |
| 培训方案审批 | `trainingdepartment/service/TrainSolutionApplicationService.java` |
| 企业确认开班 | `trainingdepartment/service/TrainInfoListApplicationService.java` |
| 企业数据范围 | `trainingdepartment/service/TrainingOperatorScopeService.java` |
| rencaishibie API 入口 | `rencaishibie/backend/app/main.py` |
| 识别+规划引擎 | `rencaishibie/python/qualification_analyzer.py` |
| 外部服务调用参考 | `app/coach/client/WeKnoraClient.java` |

---

## 三、目标业务流程

```
培训机构操作员
  │
  ├─ 方案规划 → 新建三年规划（选企业、上传员工 Excel）
  │
  ├─ Java 创建规划单 → 调用 rencaishibie upload + run
  │
  ├─ Python 执行人才识别 + 自动生成三年规划表
  │
  ├─ 机构在 rencaishibie 审核工作台调整 / reanalyze / finalize
  │
  ├─ Java 同步结构化结果 → 提交企业审核
  │
  └─ 指定企业账号 confirm / reject
        │
        └─（可选）转为 train_solution → 现有培训方案流水线
```

---

## 四、Java ↔ Python 数据互通方案（推荐）

### 4.1 推荐模式：Java 编排 + HTTP 调用 Python 服务

与现有 `WeKnoraClient`、`BaiduOcrService` 一致，在 Java 侧新增 `RencaishibieClient`：

```
admin-web  →  platform-api（唯一鉴权入口）
                ↓ HttpClient
            rencaishibie /api（内网服务）
```

**优点：**

- 不破坏 rencaishibie 现有 FastAPI + 引擎直调架构
- Java 统一租户、权限、审计、企业数据范围
- Python 服务可独立升级、扩缩容
- 避免跨语言共享 MyBatis 实体

**不推荐：**

- Java 直接读 `qualification_mvp` 表（绕过 Python 业务层，版本耦合高）
- Java 用 `ProcessBuilder` 调 Python 脚本（难做异步任务、进度、审核状态）
- 两系统共一张业务表（租户、审批状态与 Python task 生命周期冲突）

### 4.2 数据分层策略

| 数据类型 | 存放位置 | 说明 |
|----------|----------|------|
| 任务执行态（上传文件、分析进度、审核调整） | rencaishibie `qualification_mvp` | 保持现有表结构 |
| 业务单据（规划编号、企业、审批状态、关联培训方案） | Java `gjjddb2026` 新表 | 培训域主数据 |
| 结构化规划结果（矩阵、明细行） | Java 新表（finalize 后同步） | 供企业端列表/审批 UI |
| Excel 原件 | 文件服务 / COS | Java 记录 file_id，Python 存 runtime 副本 |

### 4.3 同步时机

1. **创建规划单**：Java 写 `train_three_year_plan`，调用 Python `upload + run`，记录 `rencai_task_id`。
2. **分析完成**：Java 轮询或 webhook 更新 `analysis_status=done`。
3. **机构审核完成**：Python `finalize` 后，Java 调用 Python 下载接口，解析 Excel 写入 `train_three_year_plan_item`。
4. **提交企业审核**：仅更新 Java 状态，不再调 Python。
5. **企业确认后**：可选生成 `train_solution` 草稿，进入现有培训方案流水线。

---

## 五、数据库设计（Java 侧新增）

### 5.1 三年规划主表

```sql
CREATE TABLE train_three_year_plan (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  plan_no         VARCHAR(32) NOT NULL COMMENT '规划编号',
  title           VARCHAR(200) NOT NULL,
  target_org_id   BIGINT NOT NULL COMMENT '目标企业 sys_org.id',
  research_org_id BIGINT NULL COMMENT '关联企业调研 train_research_org.id',
  rencai_task_id  BIGINT NULL COMMENT 'rencaishibie analysis_task.id',
  input_file_id   BIGINT NULL COMMENT 'platform file 或 analysis_file 映射',
  plan_file_id    BIGINT NULL COMMENT '三年规划 Excel',
  result_file_id  BIGINT NULL COMMENT '认定结果 Excel',
  plan_years      VARCHAR(32) DEFAULT '2026,2027,2028',
  status          TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1分析中 2待机构审核 3待企业确认 4企业已确认 5已驳回 6已作废',
  analysis_status VARCHAR(32) NULL COMMENT 'pending/running/done/failed',
  reject_reason   VARCHAR(500),
  company_confirm_time DATETIME,
  company_confirm_user_id BIGINT,
  created_by      BIGINT,
  create_time     DATETIME,
  update_time     DATETIME,
  deleted         TINYINT DEFAULT 0,
  INDEX idx_tenant_org (tenant_id, target_org_id),
  INDEX idx_rencai_task (rencai_task_id)
);
```

### 5.2 规划明细表

```sql
CREATE TABLE train_three_year_plan_item (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id      BIGINT NOT NULL,
  tenant_id    BIGINT NOT NULL,
  person_name  VARCHAR(64),
  id_card      VARCHAR(32),
  company_name VARCHAR(128),
  job_name     VARCHAR(128),
  level_name   VARCHAR(32),
  train_mode   VARCHAR(32) COMMENT '自费认定/新型学徒制/岗位技能提升',
  plan_year    INT,
  train_year   INT,
  cert_year    INT,
  row_index    INT,
  source       VARCHAR(16) DEFAULT 'engine' COMMENT 'engine/manual',
  deleted      TINYINT DEFAULT 0,
  INDEX idx_plan_year (plan_id, plan_year)
);
```

### 5.3 规划矩阵汇总表

```sql
CREATE TABLE train_three_year_plan_matrix (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id    BIGINT NOT NULL,
  tenant_id  BIGINT NOT NULL,
  job_name   VARCHAR(128),
  plan_year  INT,
  train_mode VARCHAR(32),
  level_name VARCHAR(32),
  headcount  INT DEFAULT 0,
  deleted    TINYINT DEFAULT 0,
  UNIQUE KEY uk_plan_cell (plan_id, job_name, plan_year, train_mode, level_name)
);
```

### 5.4 与现有表关联

- `target_org_id` → `sys_org`（企业账号范围由 `TrainingOperatorScopeService` 过滤）
- `research_org_id` → `train_research_org`（企业调研 → 方案规划链路）
- 确认后可写 `train_solution.developmentPlan` 或新建 `train_solution` 关联 `three_year_plan_id`

---

## 六、API 设计

### 6.1 Java 新增 API（`/api/training/three-year-plans`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/three-year-plans` | 分页列表（机构看全部，企业只看 `target_org_id=本企业`） |
| POST | `/three-year-plans` | 创建规划单（选企业、上传 Excel） |
| GET | `/three-year-plans/{id}` | 详情 |
| POST | `/three-year-plans/{id}/start-analysis` | 触发 Python 分析 |
| GET | `/three-year-plans/{id}/analysis-status` | 代理 Python 任务状态 |
| GET | `/three-year-plans/{id}/matrix` | 矩阵汇总 |
| GET | `/three-year-plans/{id}/items` | 明细分页 |
| PUT | `/three-year-plans/{id}/items/{itemId}` | 机构侧手工改明细（可选） |
| POST | `/three-year-plans/{id}/sync-from-rencai` | 从 Python finalize 结果同步 |
| POST | `/three-year-plans/{id}/submit` | 提交企业审核（status→3） |
| POST | `/three-year-plans/{id}/company-confirm` | 企业确认 |
| POST | `/three-year-plans/{id}/company-reject` | 企业驳回 |
| GET | `/three-year-plans/{id}/download-plan` | 下载三年规划 Excel |
| POST | `/three-year-plans/{id}/convert-to-solution` | 转培训方案草稿 |

**权限设计（对齐现有培训模块）：**

- 机构操作：`training.three_year_plan:manage`
- 企业查看/确认：`training.three_year_plan:enterprise_confirm`（复用 `INFO_LIST_ENTERPRISE_CONFIRM` 模式）

### 6.2 Java 调用 Python 的 Client 映射

```java
// 参考 WeKnoraClient 模式
public class RencaishibieClient {
  POST /api/analysis/upload          → uploadFile(MultipartFile)
  POST /api/analysis/run             → startAnalysis(RencaiRunRequest)
  GET  /api/analysis/{taskId}/status → getStatus(taskId)
  GET  /api/analysis/{taskId}/download-plan → downloadPlan(taskId)
  GET  /api/analysis/{taskId}/download      → downloadZip(taskId)
  POST /api/review/{taskId}/finalize → finalize(taskId)  // 可选由 Java 触发
  GET  /api/review/{taskId}/distribution → getDistribution(taskId)
}
```

### 6.3 配置项（application.yml）

```yaml
platform:
  rencaishibie:
    base-url: ${RENCAI_BASE_URL:http://localhost:8001}
    connect-timeout: 10s
    read-timeout: 300s
    internal-token: ${RENCAI_INTERNAL_TOKEN:}
```

### 6.4 Python 侧需小幅增强（建议）

| 增强点 | 目的 |
|--------|------|
| 请求头 `X-Tenant-Id` / `X-Plan-Id` / `X-Internal-Token` | 与 Java 鉴权对接 |
| `analysis_task` 增加 `external_plan_id`、`tenant_id` | 双向关联 |
| 可选 `POST /api/webhook/analysis-complete` | Java 注册回调，减少轮询 |
| finalize 后输出 JSON 摘要接口 | Java 免解析 Excel 即可落库 |

---

## 七、前端实现方案

### 7.1 方案规划页改造（`SolutionPlanningPage.tsx`）

**三年规划 Tab** 建议拆为：

1. **规划列表**：企业、状态、分析进度、操作（查看/审核/提交/下载）
2. **新建向导**：
   - 选企业（`sys_org`，`orgType=company`）
   - 上传员工 Excel（走 Java → 转调 Python）
   - 可选关联企业调研记录
3. **审核区**（二选一）：
   - **方案 A（快）**：iframe 嵌入 rencaishibie 审核页 `/tasks/{taskId}/review?embed=1`
   - **方案 B（统一体验）**：Java 页展示 matrix + 明细，复杂调整仍跳转 rencaishibie
4. **企业确认页**：只读矩阵 + 明细 + 确认/驳回（企业账号登录后可见）

### 7.2 iframe 与 API 协同

当前 `RencaishibieEmbedPage` 仅嵌入首页，可扩展：

```tsx
// 带 taskId 的深链
<iframe src={`/rencai/tasks/${taskId}/review?embed=1&planId=${planId}`} />
```

通过 `postMessage` 在 iframe 与父页面通信：

- 子页 `finalize` 完成 → 通知父页调用 `POST /sync-from-rencai`
- 父页传入 `tenantId`、`token`（若 Python 需独立鉴权）

### 7.3 企业账号审核

复用 `TrainingOperatorScopeService`：

- `loginRole=company` 时列表 API 自动加 `target_org_id = currentScope.orgId`
- 按钮仅展示 `status=3（待企业确认）` 的记录
- UI 参考 `TrainInfoList` 的企业确认交互

---

## 八、与现有培训流水线的衔接

企业确认三年规划后，建议提供「转为培训方案」：

```
train_three_year_plan（企业已确认）
  → train_solution 草稿
  → submit 机构审批
  → approve
  → train_info_list 开班
  → company-confirm 企业确认开班
```

这样三年规划不是孤立功能，而是培训管理上游环节，与智训平台原型（人才识别→初步方案→最终方案）一致。

---

## 九、分阶段开发计划

### 阶段 1：打通链路（2–3 周）

- [ ] Flyway 新建 `train_three_year_plan` 三表
- [ ] `RencaishibieClient` + `TrainThreeYearPlanApplicationService`
- [ ] CRUD + `start-analysis` + `analysis-status` + 文件下载代理
- [ ] `SolutionPlanningPage` 列表 + 新建 + 状态展示
- [ ] Python 增加 `external_plan_id` 字段与 internal token 校验

**验收：** 培训机构上传 Excel → 自动生成三年规划 → Java 可查看状态并下载文件。

### 阶段 2：审核与同步（2 周）

- [ ] finalize 后 `sync-from-rencai`（解析 Excel 或调 Python JSON 接口）
- [ ] 矩阵/明细展示页
- [ ] iframe 深链审核 + postMessage
- [ ] `submit` 提交企业审核

**验收：** 机构在 rencaishibie 审核后，Java 侧有结构化数据可编辑/预览。

### 阶段 3：企业审批（1–2 周）

- [ ] `company-confirm` / `company-reject`
- [ ] 企业端列表与详情页
- [ ] 权限、数据范围、审计日志

**验收：** 指定企业账号可审核指定规划单。

### 阶段 4：业务闭环（1–2 周）

- [ ] `convert-to-solution` 转 `train_solution`
- [ ] 与企业调研 `train_research_org` 关联
- [ ] 仪表盘里程碑统计（已确认三年规划数）

---

## 十、关键技术细节

### 10.1 鉴权

| 场景 | 方案 |
|------|------|
| 用户浏览器 → Java | 现有 Sa-Token + `X-Tenant-Id` |
| Java → Python | 内网 `X-Internal-Token` + `X-Tenant-Id` + `X-User-Id` |
| 浏览器 → Python（iframe） | 同域反代 `/rencai-api`；或 Java 签发短期 JWT 给 iframe |

### 10.2 异步任务

rencaishibie 分析为 CPU 密集型长任务，Java 侧建议：

- 创建规划单后立即返回 `planId`
- 前端轮询 `/analysis-status` 或 SSE 代理
- `analysis_status=done` 后允许进入审核

### 10.3 文件处理

- 上传：admin-web → Java（multipart）→ Java 转存并转发 Python `upload`
- 下载：Java 代理 Python 下载流，写入平台 file 模块，统一 CDN 地址

### 10.4 年份窗口

rencaishibie 引擎写死 `PLAN_YEARS = [2026,2027,2028]`，Java 主表 `plan_years` 应配置化，每年初需同步更新 Python 模板与常量（运维事项）。

---

## 十一、风险与对策

| 风险 | 对策 |
|------|------|
| 双库数据不一致 | 以 Java `plan.status` 为审批真源，Python `task` 为计算真源，用 `rencai_task_id` 关联 |
| iframe 体验割裂 | 阶段 2 用深链 + postMessage；长期可做 Java 原生审核 UI |
| Python 服务不可用 | Java 侧熔断 + 明确 `analysis_status=failed` + 重试 |
| 企业数据隔离 | 所有 Java 查询强制 `tenant_id` + 企业 scope |
| Excel 列格式不一致 | 创建时做模板校验；复用 rencaishibie 现有 `FIELD_ALIASES` |

---

## 十二、总结建议

1. **可以实现**，rencaishibie 已具备「导入 → 识别 → 三年规划 → 审核」全链路，Java 培训模块已具备「企业确认/方案审批」模式。
2. **Java 与 Python 的正确互通方式**是：**Java 做业务编排与审批主数据，Python 做识别计算服务，通过 HTTP API 调用**，finalize 后将结构化结果同步到 Java 新表。
3. **不要**让 Java 直接执行 Python 脚本或共用业务库表；**可以**共用同一 MySQL 实例的不同 schema，但职责要分离。
4. **最快落地路径**：阶段 1 用「Java 列表 + iframe 审核 + 文件下载」即可上线试用；再逐步把矩阵/明细和企业审批收到 Java 统一体验。

---

## 附录 A：rencaishibie 现有 API 清单

| 分类 | 接口 |
|------|------|
| 健康检查 | `GET /api/health` |
| 任务 | `GET /api/tasks`, `GET /api/tasks/{id}` |
| 分析 | `POST /api/analysis/upload`, `POST /api/analysis/run`, `GET /api/analysis/{id}/status`, `GET /api/analysis/{id}/download`, `GET /api/analysis/{id}/download-plan` |
| 审核 | `GET /api/review/{id}/distribution`, `GET /api/review/{id}/employees`, `POST /api/review/{id}/adjustments`, `POST /api/review/{id}/reanalyze`, `POST /api/review/{id}/finalize` |

## 附录 B：状态机定义

### train_three_year_plan.status

| 值 | 含义 | 可操作角色 |
|----|------|------------|
| 0 | 草稿 | 机构 |
| 1 | 分析中 | 系统 |
| 2 | 待机构审核 | 机构 |
| 3 | 待企业确认 | 企业 |
| 4 | 企业已确认 | 机构（可转方案） |
| 5 | 已驳回 | 机构（可修改重提） |
| 6 | 已作废 | — |
