# platform-api

核心业务主服务，默认采用 Java Spring Boot。

## 职责

1. 统一账号、角色、菜单、权限、租户、组织和审计。
2. 承载课程、题库、考试、学习记录、成绩、错题等一期主流程。
3. 向后台端、学员端、AI Gateway、MCP Server 和工作流能力提供受控业务 API。

## 边界

1. 不直接承载复杂 Agent 编排。
2. 不把文件存储实现写死到具体云厂商 SDK。
3. 不让外部服务绕过权限直接访问业务数据库。

## 约定

1. 健康检查接口预留为 `/actuator/health`。
2. 配置通过环境变量和配置文件注入，不提交真实密钥。
3. 核心业务按 Controller、Application、Domain、Repository、Infrastructure 分层。

## 本地启动

先启动本地 MySQL 和 Redis：

```bash
docker compose -f ../../infra/docker/docker-compose.platform-api-dev.yml up -d
```

再启动服务：

```bash
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/actuator/health
```

本地演示账号由 Flyway 种子数据创建：

```text
tenantCode: demo
username: admin
password: Admin@123456
```

## 当前已落地

1. Spring Boot 3 / Java 21 / Maven 工程骨架。
2. 统一响应结构、分页响应结构、全局异常处理、`X-Request-Id` traceId。
3. Spring Security 无状态配置，放行健康检查、登录和刷新 token。
4. MyBatis-Plus 分页插件、逻辑删除和审计字段自动填充基础配置。
5. 一期首批 Flyway 表结构迁移，覆盖系统底座、课程题库考试、学员记录、流程记录和 AI 草稿。
6. 本地开发环境变量模板和 MySQL / Redis Docker Compose。
7. 认证接口第一版：登录、当前用户、动态菜单、默认首页；刷新 token 的 Redis 撤销存储后续补。
8. 租户管理第一版：分页、详情、新增、更新、逻辑删除，接口路径符合一期 API 合同。
9. 组织管理第一版：按当前登录用户 `tenantId` 查询组织树、新增、更新、逻辑删除。
10. 用户管理第一版：按当前登录用户 `tenantId` 分页、详情、新增、更新、重置密码、逻辑删除和角色绑定。
11. 角色权限管理第一版：角色分页、详情、新增、更新、删除、绑定菜单和权限。
12. 菜单管理第一版：按当前登录用户 `tenantId` 查询菜单树、新增、更新、逻辑删除。
13. 字典管理第一版：字典类型分页、新增、更新、按编码读取字典项、新增/更新字典项。
14. 文件管理第一版：本地上传、元数据、下载 URL、下载、逻辑删除。
15. 企业 RAG 知识库第一版：知识库空间、可见角色、文档级 ACL、资料入库、Tika 文档解析、文本切片、索引任务状态、权限过滤检索、知识库问答草稿。
14. 操作审计第一版：系统管理写操作通过 `@AuditLog` 记录到 `sys_audit_log`，并提供分页查询。
15. 文件服务本地适配第一版：上传文件到本地磁盘，数据库保存元数据，提供元数据、下载地址、内容下载和逻辑删除。

## 已实现接口

认证：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 账号密码登录 |
| POST | `/api/auth/refresh` | 占位，后续接 Redis refresh token 存储 |
| POST | `/api/auth/logout` | 退出 |
| GET | `/api/auth/me` | 当前用户 |
| GET | `/api/auth/menus` | 当前用户动态菜单 |
| GET | `/api/auth/home` | 当前用户默认首页 |

租户：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/tenants` | `tenant:view` | 租户分页 |
| POST | `/api/system/tenants` | `tenant:create` | 新增租户 |
| GET | `/api/system/tenants/{id}` | `tenant:view` | 租户详情 |
| PUT | `/api/system/tenants/{id}` | `tenant:update` | 更新租户 |
| DELETE | `/api/system/tenants/{id}` | `tenant:delete` | 逻辑删除租户 |

组织：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/orgs/tree` | `org:view` | 当前租户组织树 |
| POST | `/api/system/orgs` | `org:create` | 新增组织/企业/部门 |
| PUT | `/api/system/orgs/{id}` | `org:update` | 更新组织 |
| DELETE | `/api/system/orgs/{id}` | `org:delete` | 逻辑删除组织；存在子节点时拒绝 |

用户：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/users` | `user:view` | 用户分页 |
| POST | `/api/system/users` | `user:create` | 新增用户并绑定角色 |
| GET | `/api/system/users/{id}` | `user:view` | 用户详情 |
| PUT | `/api/system/users/{id}` | `user:update` | 更新用户和角色 |
| POST | `/api/system/users/{id}/reset-password` | `user:update` | 重置密码 |
| DELETE | `/api/system/users/{id}` | `user:delete` | 逻辑删除用户 |

角色：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/roles` | `role:view` | 角色分页 |
| POST | `/api/system/roles` | `role:create` | 新增角色 |
| GET | `/api/system/roles/{id}` | `role:view` | 角色详情 |
| PUT | `/api/system/roles/{id}` | `role:update` | 更新角色 |
| PUT | `/api/system/roles/{id}/permissions` | `role:update` | 绑定菜单和权限 |
| DELETE | `/api/system/roles/{id}` | `role:delete` | 逻辑删除角色；已绑定用户时拒绝 |

文件：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/files` | 登录 | 本地上传文件，`multipart/form-data`，字段名 `file` |
| GET | `/api/files/{id}` | 登录 | 文件元数据 |
| GET | `/api/files/{id}/download-url` | 登录 | 下载 URL |
| GET | `/api/files/{id}/download` | 登录 | 下载文件 |
| DELETE | `/api/files/{id}` | 登录 | 标记文件删除 |

知识库：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/knowledge/spaces` | `knowledge:view` | 知识库分页，按租户和可见角色过滤 |
| POST | `/api/knowledge/spaces` | `knowledge:create` | 创建企业/部门/课程知识库 |
| GET | `/api/knowledge/spaces/{id}` | `knowledge:view` | 知识库详情 |
| PUT | `/api/knowledge/spaces/{id}` | `knowledge:update` | 更新知识库和可见角色 |
| DELETE | `/api/knowledge/spaces/{id}` | `knowledge:delete` | 逻辑删除知识库 |
| GET | `/api/knowledge/spaces/{id}/documents` | `knowledge:view` | 知识库文档列表 |
| POST | `/api/knowledge/spaces/{id}/documents` | `knowledge:document` | 将已上传文件入库并切片索引 |
| GET | `/api/knowledge/index-tasks/{id}` | `knowledge:view` | 查看入库索引任务状态 |
| POST | `/api/knowledge/search` | `knowledge:search` | 权限过滤后的知识库检索 |
| POST | `/api/knowledge/answer-drafts` | `knowledge:answer` | 生成只基于检索片段的问答草稿 |
| GET | `/api/knowledge/chunks/{id}/citation` | `knowledge:view` | 查看引用片段和来源 |

知识库 MVP 说明：

1. 原文件仍由文件模块管理，本地开发存储在 `FILE_LOCAL_ROOT`，生产 OSS/COS 适配后替换存储实现。
2. 文档解析使用 Apache Tika，可处理常见 PDF、Word、Excel、PPT、Markdown 和文本文件；扫描件 OCR 后续接入。
3. 当前检索先落地 MySQL 元数据、权限过滤和关键词召回；`kb_chunk` 已预留 embedding、vectorId 和 vectorStatus，后续由 `ai-gateway` 接 embedding 模型和 Qdrant。
4. 问答草稿使用 `ai_generation_draft` 保存，回答必须带引用；没有可用资料时返回证据不足，不编造结论。
5. 知识库模块是系统内唯一 RAG 底座；企业、部门、课程只是同一套 RAG 下的知识空间，不应另起多套知识库实现。
6. 普通文档默认 `accessScope=inherit`，继承知识空间可见角色；合同金额、报价、薪酬、客户敏感资料等必须使用 `accessScope=restricted` 和 `sensitiveLevel=confidential`，并通过 `visibleUserIds` / `visibleRoleCodes` 授权给签署人、负责人或高权限角色。检索、问答草稿和引用查看都会再次校验文档级 ACL。

菜单：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/menus/tree` | `menu:view` | 当前租户菜单树 |
| POST | `/api/system/menus` | `menu:create` | 新增菜单 |
| PUT | `/api/system/menus/{id}` | `menu:update` | 更新菜单 |
| DELETE | `/api/system/menus/{id}` | `menu:delete` | 逻辑删除菜单；存在子节点或已绑定角色时拒绝 |

字典：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/dicts` | `dict:view` | 字典类型分页 |
| POST | `/api/system/dicts` | `dict:create` | 新增字典类型 |
| PUT | `/api/system/dicts/{id}` | `dict:update` | 更新字典类型 |
| GET | `/api/system/dicts/{code}/items` | 登录 | 按编码读取启用字典项 |
| POST | `/api/system/dicts/{id}/items` | `dict:update` | 新增字典项 |
| PUT | `/api/system/dict-items/{itemId}` | `dict:update` | 更新字典项 |

操作日志：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/audit-logs` | `audit-log:view` | 操作日志分页 |

文件：

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/files` | 登录 | 上传文件，`multipart/form-data`，字段名 `file` |
| GET | `/api/files/{id}` | 登录 | 文件元数据 |
| GET | `/api/files/{id}/download-url` | 登录 | 下载地址 |
| GET | `/api/files/{id}/content` | 登录 | 下载文件内容 |
| DELETE | `/api/files/{id}` | 登录 | 逻辑删除文件 |

## 下一步编码顺序

1. 实现租户、用户、角色、菜单实体和 Mapper。
2. 实现账号密码登录、JWT 生成和 `/auth/me`。
3. 接入动态菜单和权限码校验。
4. 开始系统管理 CRUD，并记录写操作审计日志。
