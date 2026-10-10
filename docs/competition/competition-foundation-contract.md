# 竞赛基础链路合同（FND-00）

本合同冻结竞赛基础链路第一阶段的领域边界、状态、权限和 API 草案。实现范围仅限 `services/platform-api` 内的 `competition` 领域；本阶段不创建 Controller、ApplicationService、前端页面或任何选手端能力。

## 1. 范围与排除项

本阶段只为后续链路准备以下基础对象：赛事、赛事参与企业、赛段、任务模板、名单导入批次、名单行、参赛资格、站内通知。

明确不创建：`competition_task`、任务提交、选手/选手账号、考试、题目、成绩、排名、专家、短信、身份、手机号、身份证、验证码、token、文件成果提交、AI/MCP 业务表或接口。

## 2. 状态合同

| 对象 | 状态 | 允许迁移 |
| --- | --- | --- |
| 赛事 | `draft` / `published` / `closed` / `archived` | `draft -> published -> closed -> archived` |
| 赛事参与企业 | `invited` / `active` / `suspended` / `closed` | `invited -> active`；`active <-> suspended`；`active -> closed` |
| 名单导入批次 | `validating` / `validation_failed` / `pending_confirm` / `confirmed` | 预检进入前两者或 `pending_confirm`；`pending_confirm -> confirmed`，已确认批次不可重复确认 |
| 名单行 | `valid` / `invalid` | 由预检结果确定；`invalid` 行不得生成资格 |
| 参赛资格 | `imported_pending_confirm` / `pending_audit` / `supplement_required` / `rejected` / `eligible_unactivated` / `withdrawn` / `expired` | 审核赛事：`imported_pending_confirm -> pending_audit -> eligible_unactivated/supplement_required/rejected`；非审核赛事：`imported_pending_confirm -> eligible_unactivated`；`supplement_required -> imported_pending_confirm` |
| 任务模板 | `notice_ack` / `check_in` / `file_submission` | 仅作为类型合同，`file_submission` 不代表本阶段实现文件上传 |
| 站内通知 | `draft` / `published` | `draft -> published` |

禁止状态回退、任意跳转以及提前出现 `activated`、`participating`、`completed` 等选手阶段状态。

## 3. 数据与权限合同

所有业务实体继承 `BaseEntity`，表中必须有 `tenant_id`、`deleted`、`created_by`、`updated_by`、`created_at`、`updated_at`。企业内部赛事必须有 `owner_org_id`；企业管理员提交的组织 ID 不能作为最终裁决依据，后续服务必须从登录上下文取得组织范围。所有 Mapper 仅提供 MyBatis-Plus 基础 CRUD，不提供跨租户或 `selectAll` 绕过数据范围的方法。

本阶段冻结的最小权限码为：

```text
competition:event:view
competition:event:create
competition:event:update
competition:event:publish
competition:enterprise-event:create
competition:event-org:manage
competition:roster:import
competition:roster:confirm
competition:roster:view
competition:registration:view
competition:registration:audit
competition:notice:publish
competition:notice:view
competition:task-template:manage
competition:task-template:view
```

本轮仅将权限资源种子授予现有平台管理员角色 `role_id=2000`；正式后台菜单父级和企业业务角色在 FND-05/FND-02 明确后再补，不创建不可访问的假页面。

## 4. 导入模板合同

本节属于 FND-00 的历史基础合同。当前选手名单导入已由
[`competition-player-registration-employee-no-and-time-conflict.md`](../04_requirements/competition-player-registration-employee-no-and-time-conflict.md)
取代：不再使用 `employee_no`，并按企业、选手身份、报名工种和报名等级逐行匹配。

FND-00 的基础链路仍只保存已有文件模块的 `source_file_id`，不在本阶段定义选手身份或激活能力。

## 5. API 草案（仅记录合同）

统一前缀 `/api/competition`，统一返回 `ApiResponse<T>`，列表使用 `PageQuery/PageResponse`。本阶段只记录路径和权限，不实现 Controller 或业务逻辑：

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `POST` | `/events` | `competition:event:create` 或 `competition:enterprise-event:create` |
| `GET` | `/events`、`/events/{eventId}` | `competition:event:view` |
| `PUT` | `/events/{eventId}` | `competition:event:update` |
| `POST` | `/events/{eventId}/publish` | `competition:event:publish` |
| `GET/PUT` | `/events/{eventId}/stages` | `competition:event:view` / `competition:event:update` |
| `GET/POST/PUT` | `/stages/{stageId}/task-templates[/{templateId}]` | `competition:task-template:view/manage` |
| `GET/POST` | `/events/{eventId}/organizations` | `competition:event-org:manage` |
| `POST/GET/POST` | `/events/{eventId}/roster-imports/precheck`、`/roster-imports/{batchId}`、`/roster-imports/{batchId}/confirm` | `competition:roster:import/view/confirm` |
| `GET/POST` | `/registrations`、`/registrations/{registrationId}/audit` | `competition:registration:view/audit` |
| `POST/GET` | `/events/{eventId}/notices` | `competition:notice:publish/view` |

业务状态迁移、文件解析、名单确认、资格生成、审核和通知发布均留到后续 FND-02～FND-04。

## 6. 统一错误码草案

`401` 未登录，`403` 无赛事/企业数据范围，`409` 状态竞争或重复确认，`422` 字段/规则校验失败，`500` 外部依赖或未分类服务错误。错误响应仍使用既有 `ApiResponse`，不得在日志或响应中输出身份证、手机号、验证码、token 或文件内容。
