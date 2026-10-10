# 第一批 API 合同

版本：V0.1 草稿  
日期：2026-05-01

## 一、通用约定

基础路径：`/api`

响应结构：

```json
{
  "code": "0",
  "message": "OK",
  "data": {},
  "traceId": "req-xxx"
}
```

分页响应：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

通用规则：

1. 认证使用 `Authorization: Bearer <accessToken>`。
2. 所有租户内接口从 token 上下文解析 `tenant_id`，前端不传 `tenant_id` 作为裁决依据。
3. 后端所有写操作记录审计日志。
4. 删除接口默认逻辑删除。
5. 所有列表接口默认 `page=1&pageSize=20`。
6. 前端权限展示可以依赖菜单接口，但最终接口权限以后端校验为准。
7. 对外统一使用 `/api` 前缀，内部可由 `platform-api`、`file-service`、`workflow-service`、`ai-gateway` 分别实现或代理，但权限、租户和审计规则必须统一。
8. `ai-gateway` 和 `mcp-server` 不直连业务数据库，只能调用受控业务 API。

## 二、认证与当前用户

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 公开 | 账号密码登录 |
| POST | `/auth/refresh` | 公开 | 刷新 token |
| POST | `/auth/logout` | 登录 | 退出并撤销 refresh token |
| GET | `/auth/me` | 登录 | 当前用户、角色、组织、租户 |
| GET | `/auth/menus` | 登录 | 当前用户动态菜单 |
| GET | `/auth/home` | 登录 | 当前用户默认首页配置 |

登录请求：

```json
{
  "tenantCode": "demo",
  "username": "admin",
  "password": "******"
}
```

登录响应 `data`：

```json
{
  "accessToken": "jwt",
  "refreshToken": "opaque-token",
  "expiresIn": 7200,
  "user": {
    "id": 1,
    "displayName": "平台管理员",
    "userType": "admin"
  }
}
```

## 三、系统管理

### 1. 租户

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/tenants` | `tenant:view` | 租户分页 |
| POST | `/system/tenants` | `tenant:create` | 新增租户 |
| GET | `/system/tenants/{id}` | `tenant:view` | 租户详情 |
| PUT | `/system/tenants/{id}` | `tenant:update` | 更新租户 |
| DELETE | `/system/tenants/{id}` | `tenant:delete` | 删除租户 |

### 2. 组织

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/orgs/tree` | `org:view` | 组织树 |
| POST | `/system/orgs` | `org:create` | 新增组织/企业/部门 |
| PUT | `/system/orgs/{id}` | `org:update` | 更新组织 |
| DELETE | `/system/orgs/{id}` | `org:delete` | 删除组织 |

组织请求关键字段：

```json
{
  "parentId": 0,
  "orgType": "enterprise",
  "code": "gxqy",
  "name": "广西示例企业",
  "sortOrder": 1,
  "status": "enabled"
}
```

### 3. 用户、角色、菜单

| 模块 | 方法与路径 | 权限码 |
| --- | --- | --- |
| 用户分页 | GET `/system/users` | `user:view` |
| 新增用户 | POST `/system/users` | `user:create` |
| 更新用户 | PUT `/system/users/{id}` | `user:update` |
| 删除用户 | DELETE `/system/users/{id}` | `user:delete` |
| 重置密码 | POST `/system/users/{id}/reset-password` | `user:update` |
| 角色分页 | GET `/system/roles` | `role:view` |
| 新增角色 | POST `/system/roles` | `role:create` |
| 更新角色 | PUT `/system/roles/{id}` | `role:update` |
| 绑定权限 | PUT `/system/roles/{id}/permissions` | `role:update` |
| 菜单树 | GET `/system/menus/tree` | `menu:view` |
| 新增菜单 | POST `/system/menus` | `menu:create` |
| 更新菜单 | PUT `/system/menus/{id}` | `menu:update` |
| 删除菜单 | DELETE `/system/menus/{id}` | `menu:delete` |

角色绑定权限请求：

```json
{
  "menuIds": [1001, 1002],
  "permissionIds": [2001, 2002]
}
```

### 4. 字典

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/system/dicts` | `dict:view` | 字典类型分页 |
| POST | `/system/dicts` | `dict:create` | 新增字典类型 |
| PUT | `/system/dicts/{id}` | `dict:update` | 更新字典类型 |
| GET | `/system/dicts/{code}/items` | 登录 | 按编码读取字典项 |
| POST | `/system/dicts/{id}/items` | `dict:update` | 新增字典项 |
| PUT | `/system/dict-items/{itemId}` | `dict:update` | 更新字典项 |

## 四、文件服务

一期接口对外仍使用 `/api/files/**`，内部可由 `file-service` 独立实现，也可先由 `platform-api` 的文件模块承载。

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/files` | 登录 | 上传文件，`multipart/form-data` |
| GET | `/files/{id}` | 登录 | 文件元数据 |
| GET | `/files/{id}/download-url` | 登录 | 下载地址或预签名 URL |
| DELETE | `/files/{id}` | 登录 | 删除文件 |

上传响应：

```json
{
  "id": 9001,
  "originalName": "课程资料.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "accessPolicy": "private"
}
```

## 五、课程、题库、考试

### 1. 课程

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/courses` | `course:view` | 课程分页 |
| POST | `/courses` | `course:create` | 新增课程 |
| GET | `/courses/{id}` | `course:view` | 课程详情 |
| PUT | `/courses/{id}` | `course:update` | 更新课程 |
| DELETE | `/courses/{id}` | `course:delete` | 删除课程 |
| GET | `/courses/{id}/chapters` | `course:view` | 章节树 |
| POST | `/courses/{id}/chapters` | `course:update` | 新增章节 |
| PUT | `/course-chapters/{chapterId}` | `course:update` | 更新章节 |

### 2. 题库

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/questions` | `question:view` | 题目分页 |
| POST | `/questions` | `question:create` | 新增题目 |
| GET | `/questions/{id}` | `question:view` | 题目详情 |
| PUT | `/questions/{id}` | `question:update` | 更新题目 |
| DELETE | `/questions/{id}` | `question:delete` | 删除题目 |

题目请求：

```json
{
  "questionType": "single",
  "stem": "以下哪项是正确做法？",
  "difficulty": "easy",
  "workTypeCode": "electrician",
  "levelCode": "level3",
  "knowledgePointCode": "safe_operation",
  "options": [
    {"optionLabel": "A", "content": "选项A", "isCorrect": 1},
    {"optionLabel": "B", "content": "选项B", "isCorrect": 0}
  ],
  "analysis": "解析内容"
}
```

### 3. 试卷与考试安排

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/exam-papers` | `exam:view` | 试卷分页 |
| POST | `/exam-papers` | `exam:create` | 新增试卷 |
| GET | `/exam-papers/{id}` | `exam:view` | 试卷详情 |
| PUT | `/exam-papers/{id}` | `exam:update` | 更新试卷 |
| PUT | `/exam-papers/{id}/questions` | `exam:update` | 设置试题和分值 |
| GET | `/exam-sessions` | `exam:view` | 考试安排分页 |
| POST | `/exam-sessions` | `exam:create` | 新增考试安排 |
| PUT | `/exam-sessions/{id}` | `exam:update` | 更新考试安排 |
| POST | `/exam-sessions/{id}/publish` | `exam:update` | 发布考试 |

## 六、学员端 API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/student/courses` | 学员 | 已发布课程列表 |
| GET | `/student/courses/{id}` | 学员 | 课程详情和章节 |
| POST | `/student/learning-records` | 学员 | 保存学习进度 |
| GET | `/student/exam-sessions` | 学员 | 可参加考试 |
| POST | `/student/exam-attempts` | 学员 | 开始考试 |
| GET | `/student/exam-attempts/{id}` | 学员 | 考试题目 |
| POST | `/student/exam-attempts/{id}/answers` | 学员 | 保存答案 |
| POST | `/student/exam-attempts/{id}/submit` | 学员 | 提交试卷并出分 |
| GET | `/student/exam-attempts/{id}/result` | 学员 | 查看成绩 |
| GET | `/student/wrong-questions` | 学员 | 错题列表 |
| POST | `/student/wrong-questions/{id}/mastered` | 学员 | 标记已掌握 |

## 七、流程样板 API

课程发布审批。

一期接口对外仍使用 `/api/workflow/**`，内部可由 `workflow-service` 独立实现，也可先由 `platform-api` 集成 Flowable 承载。

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/workflow/course-publish/{courseId}/submit` | `course:update` | 提交课程发布审批 |
| GET | `/workflow/tasks` | 登录 | 我的待办 |
| POST | `/workflow/tasks/{taskId}/approve` | `workflow:approve` | 审批通过 |
| POST | `/workflow/tasks/{taskId}/reject` | `workflow:approve` | 驳回 |
| GET | `/workflow/instances/{instanceId}/records` | 登录 | 流程记录 |

## 八、AI 样板 API

一期接口对外仍使用 `/api/ai/**`。AI 生成由 `ai-gateway` 承载，草稿保存、权限校验和业务落库必须通过 `platform-api` 的受控接口完成。

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/ai/course-outline-drafts` | `ai:course-outline` | 生成课程大纲草稿 |
| GET | `/ai/drafts/{id}` | `ai:course-outline` | 查看 AI 草稿 |
| POST | `/ai/drafts/{id}/accept` | `ai:course-outline` | 标记采纳 |
| POST | `/ai/drafts/{id}/discard` | `ai:course-outline` | 标记废弃 |

请求：

```json
{
  "courseTitle": "电工安全操作基础",
  "workTypeCode": "electrician",
  "levelCode": "level3",
  "sourceFileIds": [9001],
  "prompt": "请生成适合三级电工的课程大纲"
}
```

规则：

1. AI 结果只保存到 `ai_generation_draft`。
2. AI 结果不自动写入 `course.outline`。
3. 用户采纳后，由前端复制或后端显式应用到课程，必须记录操作日志。

## 九、MCP 样板工具

MCP Server 一期不直接对前端开放 REST 页面，先提供给 AI Gateway 或受控 Agent 调用。

| 工具名 | 权限码 | 说明 |
| --- | --- | --- |
| `course.search` | `course:view` | 按课程名称、工种、等级查询课程摘要 |
| `question.search` | `question:view` | 按题型、工种、等级、知识点查询题目摘要 |
| `file.metadata.get` | 登录 | 查询文件元数据，不返回越权文件内容 |
| `course_outline_draft.create` | `ai:course-outline` | 基于课程主题创建 AI 大纲草稿 |
| `material_check_report.create` | `ai:material-check` | 创建材料检查报告草稿 |
| `project_weekly_draft.create` | `ai:project-weekly` | 创建项目周报草稿 |
| `todo_draft.create` | 登录 | 创建待办草稿 |

规则：

1. MCP Tool 必须带用户身份、租户上下文和请求 ID。
2. MCP Tool 只调用 `platform-api` 或 `ai-gateway` 的受控接口。
3. 写操作默认只创建草稿，不直接发布、不直接审批通过、不直接删除业务数据。
4. 高风险动作必须先返回计划和风险提示，等待人工确认后才能执行。
5. Tool 调用结果必须记录工具名、参数摘要、触发人、租户、traceId 和执行结果。
