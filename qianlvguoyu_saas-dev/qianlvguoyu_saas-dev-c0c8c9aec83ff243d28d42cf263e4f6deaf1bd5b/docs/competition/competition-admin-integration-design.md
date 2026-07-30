# 竞赛后台接入设计

管理后台位于 `apps/admin-web`。它以 `AuthGate`、Zustand `authStore`、`apiClient` 的 Bearer token / `X-Tenant-Id` 拦截器、后端菜单树和 `PermissionGate` 作为接入基础。

后续在 `src/pages/competition/` 建立：赛事管理、赛段管理、报名审核、任务管理和通知管理；相应 API 放在 `src/api/competition.ts`，类型放在 `src/types/competition.ts`。在 `src/main.tsx` 的 `pageMap` 增加页面映射。菜单最终由 `/auth/menus` 动态加载，不能只依赖 `fallbackMenuItems`。

建议权限码：

- `competition:event:view` / `competition:event:create` / `competition:event:update`
- `competition:stage:view` / `competition:stage:manage`
- `competition:registration:view` / `competition:registration:audit`
- `competition:task:view` / `competition:task:manage`
- `competition:notice:view` / `competition:notice:manage`

这些权限仅赋予后台管理角色。选手端从不读取后台菜单，也不消费这些权限码。
