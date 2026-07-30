## 背景与目标

在“认定筹备期”的考评员配置中，考评员（含组长/成员）需要与“信息管理模块-考评员信息”数据联动。筹备期配置时只能从信息管理中“审核状态为已入库”的考评员里进行配置，本次落地口径进一步收敛为“可用考评员”（已入库 + 空闲 + 未过期）。

目标：

- 筹备期的“添加成员/选择组长”候选列表 **复用信息管理的考评员信息**，并按“可用”条件筛选。
- 配置操作遵循：**添加成员（多选） → 创建考评组（从已选成员中选组长） → 保存成功**。
- 保存后前后端保证联动：成员占用后信息管理的工作状态更新为“认定筹备中/认定任务中”，移除后可恢复空闲（在满足释放条件时）。

非目标：

- 不引入新的权限体系，不调整信息管理既有的审核流（草稿/待审/已入库/驳回/停用）。
- 不新增接口（优先复用现有接口）；若后续需要后端事务化“一次提交”，再单独立项。

---

## 术语与枚举

### 信息管理-考评员信息（Certification Info）

- **审核状态** `auditStatus`：
  - `draft` 草稿
  - `pending` 待主管审核
  - `approved` 已入库
  - `rejected` 已驳回
  - `disabled` 已停用
- **工作状态** `workStatus`：
  - `idle` 空闲
  - `preparing` 认定筹备中
  - `assigned` 认定任务中
  - `disabled` 停用
  - `expired` 过期

### 筹备期-考评组/成员（Preparation）

- 考评组 `CertificationTaskAssessorGroup`：
  - `leaderAssessorId`：组长 ID（来自候选考评员）
  - `status`：`preparing` / `dispatched` / `completed`（现有）
- 成员 `CertificationTaskAssessor`：
  - `taskRole`：`leader` / `member`
  - `workStatus`：成员在任务侧展示状态（现有：`preparing` / `assigned`）

---

## 数据联动规则（核心）

### 1) 候选成员数据源

筹备期“添加成员”弹窗的候选列表来自信息管理“考评员信息”：

- 仅展示“可用考评员”：
  - `auditStatus = approved`（已入库）
  - `workStatus = idle`（空闲）
  - 证书未过期（`validUntil` 为空或 `validUntil >= today`）

后端现成接口支持：

- `GET /certification/info/assessors/available?keyword=...`

> 说明：该接口在后端已内置 `auditStatus=approved` + `workStatus=idle` + 未过期过滤。

### 2) 占用/释放联动（状态写回）

当信息管理考评员被用于筹备期配置时，需要写回信息管理工作状态：

- **加入任务考评组成员**：
  - 信息管理 `workStatus`：`idle` → `preparing`
- **发放任务（考评任务生成）**：
  - 信息管理 `workStatus`：`preparing` → `assigned`
- **移除成员**：
  - 若该考评员不再被任何任务占用（同一时刻没有其它任务成员记录/任务分配），信息管理 `workStatus` → `idle`

约束与说明：

- 若成员在保存前后已被其他操作占用（并发），后端应报错并提示刷新候选列表。
- 证书过期不得分配到任务（已有后端校验，保留）。

---

## 交互流程（按需求原话落地）

### 流程：添加成员 → 创建考评组 → 选组长 → 保存

#### Step 1：点击“添加成员”

- 弹窗展示候选成员表格（支持关键字搜索、分页/滚动）
- 支持多选（checkbox）
- 必选校验：至少选择 1 人

#### Step 2：点击“创建考评组”

- 在同一弹窗进入“创建考评组”区（或弹窗内第二步）
- 表单字段：
  - `groupName`（必填）
  - `workTypeName`（默认带出任务工种，可编辑）
  - `leaderAssessorId`（必填，**只能从 Step 1 已选成员中选择**）
  - `remark`（可选）
- 组长选择校验：
  - 若 `leaderAssessorId` 不在已选成员内：前端拦截，提示“组长必须从已选成员中选择”

#### Step 3：保存（配置成功）

保存成功后：

- 提示“配置成功”
- 刷新：考评组列表、组成员列表、以及候选列表（可用人员会减少）

---

## 接口编排（不新增 API，复用现有）

> 前端一次点击“保存”触发串行调用；如中途失败，前端给出明确错误提示并引导刷新重试。

### 1) 创建考评组

- `POST /certification/preparation/tasks/{taskId}/assessor-groups`
- payload（沿用现有 `PreparationAssessorGroupSaveReq`）：
  - `groupName`
  - `leaderAssessorId`
  - `workTypeName`
  - `status = 'preparing'`
  - `remark`

返回：`groupId`

### 2) 批量添加成员（前端循环调用）

- 对所选成员逐个调用：
  - `POST /certification/preparation/tasks/{taskId}/assessor-groups/{groupId}/members`
  - payload（沿用现有 `PreparationTaskAssessorSaveReq`）：
    - `assessorId`
    - `taskRole`：
      - 组长那个人：`leader`
      - 其他成员：`member`
    - `roleName`（默认“组长/成员”，可不传或传默认）
    - `remark`

> 注：目前后端已防重复“同一任务同一考评员不得重复分配”。需要补充校验：候选必须来自信息管理“已入库+空闲”。

---

## 校验与错误处理

### 前端校验

- 未选择任何成员：提示“请至少选择 1 名成员”
- 未选择组长：提示“请选择组长”
- 组长不在已选成员：提示“组长必须从已选成员中选择”

### 后端校验（需要/保留）

- 候选合法性：
  - 考评员必须 `auditStatus=approved` 且 `workStatus=idle`（否则报错：“考评员非空闲或未入库，不能分配”）
  - 证书过期不得分配（已有）
  - 工种范围必须覆盖当前任务工种（已有）
  - 同一任务同一考评员不得重复分配（已有）

---

## 变更点清单（实现范围提示）

- 前端：
  - `AssessorConfig.tsx`：将“添加成员”改造为“多选候选 → 创建考评组（从已选中选组长）→ 保存”
  - 候选取数改用 `certificationInfo` 的 `assessors/available`
- 后端：
  - `CertificationPreparationApplicationService.addGroupMember`：
    - 改为从信息管理考评员表取数（或新增一个读取方法）并增加 `auditStatus/workStatus` 校验
    - 写回信息管理考评员 `workStatus`（加入成员/发放/移除时）
  - `dispatchGroup` / `removeGroupMember`：补齐信息管理工作状态联动

