# 竞赛模块：当前平台与后台接入规范

## 核查结论

当前核心服务是 `services/platform-api`，已具备系统用户、角色、菜单、租户、组织、文件、知识库、题库等底座；正式管理端是 `apps/admin-web`。竞赛应以 `competition` 领域包接入主服务，并以 `pages/competition/` 接入管理端，不能把 `prototypes/competition-full/admin.html` 当正式后台继续开发。

## 后端规范

- Controller 负责 REST 入参、`@PreAuthorize` 与统一返回；ApplicationService 承担事务与用例编排；Entity 不外露；Mapper 结果判空。
- 沿用 `ApiResponse<T>`、`PageQuery`、`PageResponse<T>`；当前分页响应字段为 `items,page,pageSize,total`，实施任务必须保持前后端一致。
- `BaseEntity` 已规范 `BIGINT id`、`tenant_id`、审计字段和 `deleted`；多租户由 `TenantContextFilter` 与 MyBatis-Plus 插件处理，业务 SQL 不手拼 tenant 条件。
- Flyway 只新增 `V{YYYYMMDD}_{序号}__描述.sql`，snake_case，种子数据幂等；禁止修改已应用迁移。
- 所有写操作审计；文件只关联 fileId；认证、授权和范围校验失败必须记录 warn/error。

## 管理端接入规范

- `AppShell` 用 TanStack Query 请求 `/auth/menus`，后端菜单 `code` 决定侧栏和 `main.tsx` 内 `pageMap` 的页面挂载。
- 后续竞争模块必须配套：菜单种子、角色-菜单/权限关系、页面 `pageMap`、`src/pages/competition/`、`src/api/competition.ts`、`src/types/competition.ts`。
- 列表用 ProTable，编辑用 ProForm，接口数据用 TanStack Query；用户、菜单和页面选择状态使用 Zustand。
- `apiClient` 自动注入 Bearer token、`X-Tenant-Id` 与 `X-Request-Id`；401 清登录态，403 只提示，500 区分代理与业务错误。
- 前端仅控制可见性；所有竞赛数据范围、按钮写权限和状态转换由 `platform-api` 作最终裁决。

完整架构、API、实体和权限规划见 [competition-architecture-analysis.md](competition-architecture-analysis.md)。
