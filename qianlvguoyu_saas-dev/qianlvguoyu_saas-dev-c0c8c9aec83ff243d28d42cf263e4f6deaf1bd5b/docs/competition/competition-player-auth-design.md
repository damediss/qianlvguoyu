# 竞赛选手认证设计

## 身份模型

管理后台继续使用平台 `PlatformUserPrincipal`、角色和权限码（RBAC）。选手端使用独立 player subject：`playerId`、`playerName`、选手资料、当前赛事和当前赛段。选手不是后台角色，不能因登录而获得 `competition:event:*` 等管理权限。

## Token

选手 token 由未来的 `POST /api/competition/player-auth/login` 签发，客户端仅保存到 `competition_player_token`，绝不读取或写入培训端的 `itoken`、`userId`、`userInfo`。

请求仅携带 `Authorization: Bearer <competition_player_token>`。401 时清理选手 session 并跳转选手登录页；不跳转培训首页，也不尝试使用培训 token 刷新。

## 服务端复用与隔离

COMP-002 应复用 Sa-Token 的生命周期、Spring Security 链、`TenantContextHolder`、`AuditLog` 和文件服务；但应创建 player 专用 principal / token session 和 `/competition/player/**` 控制器边界。当前 `tik` token 不支持 token extra，主体和 tenant 应按现有桥接过滤器的模式从持久化身份解析。

## 授权流程

1. 验证选手 token 并构建 player principal。
2. 解析租户上下文，且只允许选手所属租户的数据。
3. 校验选手与目标赛事的报名、状态、赛段和本人数据范围。
4. 对报名、任务提交等写操作记录审计日志。

前端守卫只做体验层拦截；服务端是最终裁决点。
