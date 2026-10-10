# 方案 A：App 登录保持 PHP 签发 + SaaS 令牌镜像验票（互不影响）实现方案

> 状态：App 侧已落地（`saasRequest` 透传 Bearer；公开路径跳过）  
> 日期：2026-08-06  
> 适用范围：`gjjd_appnext` + `gjjd_phalcon` + `qianlvguoyu_saas/platform-api`  
> 关联：AI 同步学鉴权 D1；`PlatformSyncService`；`AppTokenAuthFilter`  
> App 改动：`utils/saasRequest.js`、`api/research-collect.js`、`api/sign-up-collect.js`（ocr）

---

## 1. 目标与非目标

### 1.1 目标

| # | 目标 |
|---|------|
| 1 | App **登录入口不变**：仍调 PHP `/appauth/login` |
| 2 | 提升 / 考试 / 商城等 **PHP 业务零改造**，继续认 `istate=itoken` |
| 3 | SaaS 新能力（同步学、跟班作业、采集等）用同一 `itoken`，经镜像表验票 |
| 4 | 两端互不影响：不改登录签发权、不引入第二套主会话 |

### 1.2 非目标

- 不把主登录改成 SaaS `/appauth/login`
- 不做登录双写（那是方案 B）
- 不改 PHP promote / appsmart 主路径语义

---

## 2. 总体架构

```text
┌──────────── gjjd_appnext ────────────┐
│ 登录页 → request → PHP /appauth/login │
│ 成功 → storage.itoken                 │
│                                       │
│ 旧业务 → request(+istate=itoken) → PHP│
│ 新业务 → saasRequest                  │
│   Authorization: Bearer <itoken>      │
│   X-Tenant-Id: <tenant>               │
└───────────────┬───────────────────────┘
                │
     ┌──────────┴──────────┐
     ▼                     ▼
 PHP 库 gjjddb          SaaS 库 gjjddb2026
 platform_user*         镜像 platform_user*
 platform_user_token ◄──同步── platform_user_token
        ▲                     ▲
        │ 签发/踢下线          │ AppTokenAuthFilter 验票
        │                     │
   AppauthController    /appreviewsync、/training/follow…
```

**一句话**：票只在 PHP 签发；SaaS 每 3s（可配）把 token 镜像过来；App 调 SaaS 时带同一张票。

---

## 3. 已有能力（无需重写）

| 组件 | 路径 | 作用 |
|------|------|------|
| PHP 登录 | `gjjd_phalcon` `AppauthController::loginAction` | 写 PHP `platform_user_token`，返回 `token` |
| 三表同步 | `PlatformSyncService` | PHP→SaaS：`platform_user` / `detail` / `token` |
| 同步配置 | `platform.sync.*` | `enabled`、`intervalMs=3000`、每日对账 cron |
| 验票 | `AppTokenAuthFilter` | 读 SaaS 库 token，按 id 升序取首行（对齐 PHP 重复 token） |
| App 登录页 | `pages/auth/login.vue` | `url:"/appauth/login"` + `skipAuth` |
| 竞赛隔离 | `competition_player_token` | 与主 itoken 分键，不在本方案范围 |

同步语义要点（实现时勿破坏）：

1. **token 表每周期全量对账**（含物理删除 → 踢人 ≤1 周期在 SaaS 生效）  
2. SaaS 本地行 `id ≥ 10亿` 不同步任务删除  
3. token 列无 UNIQUE（与 PHP 一致），避免镜像 thrashing  

---

## 4. 实现步骤（按落地顺序）

### 阶段 0：环境与权限（运维）

1. SaaS 配置第二数据源可读 PHP 库（现网 `phpJdbcTemplate`）。  
2. 同步账号至少具备：

```sql
GRANT SELECT ON gjjddb.platform_user TO 'saas_sync'@'<SaaS-IP>';
GRANT SELECT ON gjjddb.platform_user_detail TO 'saas_sync'@'<SaaS-IP>';
GRANT SELECT ON gjjddb.platform_user_token TO 'saas_sync'@'<SaaS-IP>';
```

3. 生产打开同步（示例）：

```yaml
platform:
  sync:
    enabled: true          # 禁止长期 false
    interval-ms: 3000
    dry-run: false         # 生产必须 false
    fail-alert-threshold: 20
    daily-reconcile-cron: "0 30 3 * * *"
```

4. 确认调度已加载（启动日志 / 同步状态接口 `SyncStatusController`）。

**验收**：PHP 库新插一条测试 token → ≤3s SaaS 库可见同 token。

---

### 阶段 1：App 侧约定（开发，小改）

> 现状：`saasRequest.js` **默认不带** `Authorization`；公开采集可不带票。  
> 凡需登录态的 SaaS 接口必须显式带票。

**规范**：

```js
// 推荐：在 saasRequest 内统一注入（需登录的接口）
header: {
  'Content-Type': 'application/json',
  'X-Tenant-Id': tenantId,
  ...(itoken ? { Authorization: 'Bearer ' + itoken } : {}),
}
```

规则：

| 接口类型 | 是否带 Bearer |
|----------|----------------|
| `/training/public/**` 公开采集 | 否 |
| `/appreviewsync/**`、跟班作业、需登录 SaaS API | 是 |
| PHP `request` | 仍只走 `istate`，**不要改成只打 SaaS** |

**租户**：`X-Tenant-Id` 与现网约定一致（同步学文档曾用 `1`；调研采集有 `saas_tenant_id` / 默认 `10159`）。按业务域统一，禁止混用导致数据串租户。

**登录后进 SaaS 页**：若偶发 401，可提示「请稍候重试」或客户端最多退避重试 2～3 次（间隔 1s），覆盖同步窗口；**不要**为此改登录接口。

**验收**：PHP 登录成功 → 等 ≤3s → 带 Bearer 调任一需登录 SaaS 接口 → 200。

---

### 阶段 2：SaaS 接口接入纪律（开发）

1. 需登录的 App 接口放在 `AppTokenAuthFilter` 可覆盖路径，从 `SecurityContext` 取 `platformUserId`。  
2. **禁止**信任前端传的 `userId`。  
3. 异步线程禁止读 `SecurityContext`（沿用 AGENTS.md）。  
4. 不要在 SaaS `/appauth/login` 作为 App 主登录入口对外宣传/切换。

**验收**：无票 / 假票 → 401；真票（已同步）→ 业务成功。

---

### 阶段 3：踢下线与登出一致性（验证即可）

| 动作 | PHP | SaaS 镜像 |
|------|-----|-----------|
| 新设备登录踢旧票 | 删 PHP token + Gateway 通知 | ≤1 同步周期删除镜像行 |
| App 调 PHP logout | 删 PHP token | 同上 |
| 仅调 SaaS logout | 只删 SaaS 镜像（若已实现） | **不够**：PHP 票仍在，旧业务仍可用 |

**约定（方案 A）**：主登出仍走 **PHP** `/appauth/logout`；SaaS logout 仅作辅助清理镜像。

**验收**：设备 A 登录 → 设备 B 登录 → A 的 itoken 调 PHP 失败；≤3s 调 SaaS 亦失败。

---

## 5. 时序（登录 → 调 SaaS）

```text
App                PHP登录           PHP库            同步任务           SaaS API
 |--login---------->|                 |                 |                 |
 |                  |--insert token-->|                 |                 |
 |<--token----------|                 |                 |                 |
 | 存 itoken        |                 |                 |                 |
 |                  |                 |<--SELECT 3s-----|                 |
 |                  |                 |                 |--upsert token-->|
 |--saasRequest Bearer itoken------------------------------------------->|
 |                  |                 |                 |  Filter 查镜像  |
 |<-----------------------------------------------200--------------------|
```

最坏延迟：约一个 `intervalMs`（默认 3s）+ 网络。

---

## 6. 监控与告警

| 项 | 建议 |
|----|------|
| 同步连续失败 | `fail-alert-threshold`（默认 20 周期 ≈1 分钟）告警 |
| SaaS 401 尖刺 | 登录后短时 401 占比；若持续升高查同步延迟/开关 |
| token 行数漂移 | 每日对账；镜像 UNIQUE 禁止私自添加 |
| dry-run | 生产严禁 `platform.sync.dry-run=true` |

---

## 7. 测试清单

### 7.1 回归（证明互不影响）

- [ ] 密码登录 / 短信登录（PHP）成功  
- [ ] 设备绑定拦截、解绑申请仍走 PHP  
- [ ] 提升 Tab、考试、错题本等 PHP 接口正常  
- [ ] 退出登录后 PHP 业务不可用  

### 7.2 方案 A 正向

- [ ] 新登录用户 3s 内 SaaS 库出现同 token  
- [ ] Bearer + X-Tenant-Id 调同步学 overview（或其它需登录接口）成功  
- [ ] 踢下线后 SaaS 在 1 周期内 401  
- [ ] 公开采集接口不带票仍可用  

### 7.3 负面

- [ ] `platform.sync.enabled=false` 时新登录调 SaaS → 持续 401（预期；用于理解依赖）  
- [ ] 错误 Tenant-Id → 业务拒绝或空数据（按接口设计）  

---

## 8. 风险与应对

| 风险 | 应对 |
|------|------|
| 登录后立即调 SaaS 偶发 401 | 客户端短退避重试；产品文案「请稍候」；长期仍频繁则评估方案 B 验票回源 |
| 同步任务停摆 | 告警 + 禁止关 enabled；值班手册含手动 reconcile |
| 双库账号不一致 | user/detail 同步保证密码校验只在 PHP；SaaS 不靠本地密码登录 |
| 误切 App 登录到 SaaS | Code Review 红线：login.vue 保持 `/appauth/login` → PHP domain |

---

## 9. 交付物与责任

| 交付 | 责任方 |
|------|--------|
| 同步开关/库授权/告警 | 运维 + 后端 |
| `saasRequest` 统一带 Bearer（需登录接口） | App |
| SaaS 新接口鉴权与租户头 | 后端 |
| 本方案验收签字 | 测试 |

**工作量估计**（在同步已上线前提下）：

- 运维核对 + 回归：0.5～1 人日  
- App `saasRequest` 补 Authorization：0.5 人日  
- 联调同步学/跟班等：1 人日  

若同步未开：先完成阶段 0（1～2 人日含授权与对账）。

---

## 10. 结论

方案 A **不是从零开发登录**，而是：

1. **钉死** PHP 签发；  
2. **保障** 三表同步常开；  
3. **规范** App 调 SaaS 时透传同一 `itoken`；  
4. **用监控** 兜住延迟与停摆。  

做到以上四点，即可在不影响 App 现有 PHP 功能的前提下，持续接入 SaaS 能力。若日后登录后即时 SaaS 成为硬需求，再单开「方案 B：验票 miss 回源 PHP」补丁，无需推翻 A。
