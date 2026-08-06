# App 同步学 / 班级刷题 / 课堂作业 — 实现方案

> 文档类型：**可执行实现方案**（任务拆解 · 文件清单 · 接口 · 验收）  
> 产品设计见：[App端同步学与刷题作业设计实现方案.md](./App端同步学与刷题作业设计实现方案.md)  
> 日期：2026-08-06  
> 工程：`gjjd_appnext`（主）+ `qianlvguoyu_saas/platform-api`（作业后端）+ `admin-web`（作业 Tab）

---

## 0. 范围与红线

### 做

| 通道 | 名称 | 本期交付 |
|------|------|----------|
| C | 同步练习 | App 页接通已有 `/appreviewsync` |
| A | 班级刷题 | 「我的班级」入口 + 刷题带 `classId`（兼容不传） |
| B | 课堂作业 | 后端四表+API → App 布置/答题 → 管理端作业 Tab |

### 不做 / 红线

- 不改 `pages/promote/*` 主语义，不把作业接到 `/apppromote`
- 不改 PHP 登录主路径（方案 A）
- 不同通道答卷不混表、指标不混算
- SaaS 单开高位 `platform_user` 未回写 PHP 前，不作为「可登录学员」验收账号

---

## 1. 总体架构

```text
登录：PHP /appauth/login → itoken
        │
        ├─ request(+istate) ──► PHP：promote / appsmart（通道 A 抽题交卷）
        │
        └─ saasRequest(Bearer) ► SaaS：
              /appreviewsync/**     通道 C
              /training/follow/**   通道 B（待建）
              practice-scope 查询   通道 A 辅助
```

已具备：`saasRequest.js` Bearer + 401 短重试；`PlatformSyncService`；`AppReviewSyncController`。

---

## 2. 分期与排期建议

| 阶段 | 名称 | 工期（人日，估） | 依赖 |
|------|------|------------------|------|
| **P0** | 同步学 App | 3～5 | sync 开、账号 PHP 可登 |
| **P1** | 我的班级 + 刷题带班 | 3～4 | practice-scope；appsmart 可选参 |
| **P2** | 作业后端 M3-a + 管理端 Tab | 5～8 | Flyway、跟班权限 |
| **P3** | 作业 App 闭环 | 5～7 | P2 |
| **P4** | 可选增强 | 2～3 | 题库选题、错题注入同步学 |

建议发版：`P0` 可单独上；`P1` 可跟；`P3` 必须等 `P2`。

---

## 3. P0 — 同步学（通道 C）

### 3.1 目标

学员在提升区进入「同步练习」，完成：概览 → 今日题 → 作答 → 模考 → 学情。

### 3.2 后端（已有，只验收）

| 方法 | 路径 |
|------|------|
| POST | `/appreviewsync/overview` |
| POST | `/appreviewsync/today` |
| POST | `/appreviewsync/question` |
| POST | `/appreviewsync/answer` |
| POST | `/appreviewsync/mock/start` |
| POST | `/appreviewsync/mock/submit` |
| POST | `/appreviewsync/finish` |
| POST | `/appreviewsync/progress` |
| POST | `/appreviewsync/explain`（可选） |

请求头：`Authorization: Bearer <itoken>`、`X-Tenant-Id`。

### 3.3 App 文件清单（新建）

| 文件 | 职责 |
|------|------|
| `api/sync-practice.js` | 封装上表 API，一律 `saasRequest` |
| `pages/sync-practice/home.vue` | 计划/阶段概览 |
| `pages/sync-practice/today.vue` | 今日清单 |
| `pages/sync-practice/answer.vue` | 单题作答 |
| `pages/sync-practice/mock.vue` | 模考 |
| `pages/sync-practice/progress.vue` | 学情 |
| `pages.json` | 注册页面；提升首页加入口 |

### 3.4 入口改造（最小）

| 文件 | 改动 |
|------|------|
| `pages/promote/home.vue` 或 `taskList` / 复习相关页 | **仅增加**「同步练习」入口按钮/Tab，跳转新页；不改原列表接口 |

### 3.5 验收

- [ ] PHP 登录后进同步练习，overview 非 401  
- [ ] 今日作答、finish、progress 可走通  
- [ ] 关闭网络/无票有明确提示  
- [ ] promote 原「课后作业/考试」路径回归通过  

---

## 4. P1 — 我的班级 + 班级刷题（通道 A）

### 4.1 目标

学员看到在籍班级；从班级进入刷题且带 `classId`；无班上下文的旧刷题入口行为不变。

### 4.2 后端

| 能力 | 状态 | 说明 |
|------|------|------|
| practice-scope CRUD | 已有 | `/training/info-classes/{id}/practice-scope` |
| App「我的班级」列表 | **待补** | 建议 `GET /training/follow/my-classes`（按 platform_user 查花名册） |
| appsmart 接收 classId | **待补/确认** | 落 `exam_* .class_id`；不传则旧逻辑 |

若 P1 赶工：班级列表可暂用已有培训人员/班级只读接口拼装，但需产品确认口径。

### 4.3 App 文件清单

| 文件 | 职责 |
|------|------|
| `api/follow-class.js` | 我的班级、scope、课次只读 |
| `pages/follow/class-list.vue` | 班级列表 |
| `pages/follow/class-home.vue` | 班级首页（刷题入口、作业入口占位） |
| `pages/follow/class-practice.vue` | 读 scope → 跳转/内嵌现有刷题并传入 `classId` |
| 现有 appsmart 调用处 | **可选参数** `classId`，默认 undefined |

### 4.4 验收

- [ ] 在籍学员能看到班级  
- [ ] 从班级刷题的请求带 classId  
- [ ] 提升里原刷题入口不带 classId，行为与现网一致  
- [ ] 未配 scope 有提示或约定回退  

---

## 5. P2 — 课堂作业后端 + 管理端（通道 B 服务端）

### 5.1 目标

可造数验收：发布任务 → 学员提交判分 → 跟班 Tab 看见完成率；插班补发。

### 5.2 数据库（Flyway 新建）

| 表 | 用途 |
|----|------|
| `train_follow_task` | 任务主表（homework/practice，schedule_id / biz_date） |
| `train_follow_task_question` | 题目快照 |
| `train_follow_task_item` | 学员任务态 |
| `train_follow_task_answer` | 作答明细 |

字段细则：[跟班作业与随堂练实现方案.md](./跟班作业与随堂练实现方案.md) §3。

### 5.3 API（建议前缀 `/training/follow`）

**老师 / 管理**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/my-teaching-classes` | 可布置班级 |
| GET | `/classes/{classId}/schedules` | 课次（可复用已有 schedule API） |
| GET | `/classes/{classId}/tasks` | 任务列表 |
| POST | `/tasks` | 创建草稿 |
| PUT | `/tasks/{id}` | 改草稿 |
| POST | `/tasks/{id}/publish` | 发布+名单快照 |
| POST | `/tasks/{id}/close` | 关闭 |
| GET | `/tasks/{id}/items` | 进度 |

**学员**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/student/tasks` | 待办/已做 |
| GET | `/tasks/{id}/paper` | 拉卷（无答案） |
| POST | `/tasks/{id}/submit` | 交卷判分 |
| GET | `/tasks/{id}/result` | 结果 |

权限：`train:follow:homework:*` 或复用 `train:follow:view` + 角色校验。  
插班：花名册新增 student → 补 `task_item`。

### 5.4 后端文件（新建/改）

| 区域 | 内容 |
|------|------|
| `db/migration/V*_create_train_follow_task*.sql` | 四表 |
| `entity/mapper` ×4 | |
| `TrainFollowTaskApplicationService` | 发布/判分/补发 |
| `controller` App+Admin 或统一 `/training/follow` | |
| `SecurityConfig` | App 路径允许 Bearer（与现 App 接口一致，非 Sa-Token 管理端） |

### 5.5 管理端

| 文件 | 改动 |
|------|------|
| `FollowClassWorkbench.tsx` | 「作业」Tab 替换 comingSoon：列表+明细+完成率 |
| `api/training.ts` | 作业相关方法 |

### 5.6 验收

- [ ] 后台造数：发布 → 提交 → graded  
- [ ] 插班生自动 pending  
- [ ] 跟班作业 Tab 完成率正确  
- [ ] 客观题自动判分；主观题本期不做  

---

## 6. P3 — 课堂作业 App（通道 B 客户端）

### 6.1 目标

老师布置（挂课次/按日，首版自拟客观题）；学员待办答题看结果。

### 6.2 App 文件清单

| 文件 | 职责 |
|------|------|
| `api/follow-homework.js` | 布置/发布/待办/拉卷/交卷 |
| `pages/follow/homework-todo.vue` | 学员待办 |
| `pages/follow/homework-answer.vue` | 答题 |
| `pages/follow/homework-result.vue` | 结果 |
| `pages/follow/teacher-assign.vue` | 布置向导 |
| `pages/follow/teacher-progress.vue` | 进度 |
| `pages/follow/class-home.vue` | 挂上作业入口 |

### 6.3 交互要点

1. 布置：选课次 → 录题（单选/多选/判断）→ 预览 → 发布。  
2. 学员：待办 → paper → submit → result。  
3. 文案固定为「课堂作业/随堂练」，与「同步练习」「考前练习」区分。

### 6.4 验收

- [ ] 老师发布后学员 3s 内（sync）可见待办  
- [ ] 交卷得分与管理端一致  
- [ ] promote 回归通过  
- [ ] 无授课身份看不到布置入口  

---

## 7. P4 — 可选增强（排期外可砍）

| 项 | 说明 |
|----|------|
| 题库选题拷贝快照 | 作业方案档 B |
| 作业错题注入 `ai_review_wrong_item` | 开关默认关 |
| 培训详情「今日作业 N」角标 | 只读计数 |
| 开户双写 PHP | 解决 SaaS 单开账号无法登录（独立强依赖） |

---

## 8. 跨切面清单

### 8.1 鉴权

| 项 | 要求 |
|----|------|
| 登录 | 仍 PHP；测试账号必须在 PHP `platform_user` |
| SaaS 调用 | `saasRequest`（已带 Bearer） |
| 租户头 | `X-Tenant-Id` 与同步学/采集约定统一，禁止硬编码混乱 |
| 公开采集 | 继续 `publicRequest`，勿误带强制登录 |

### 8.2 配置 / 运维

| 项 | 要求 |
|----|------|
| `platform.sync.enabled` | 生产 true |
| App `util.saas` | pro/dev 指向正确 platform-api |
| 401 | 已有短重试；产品可提示「请稍候再进同步练习」 |

### 8.3 禁止改动的文件（除非入口级一行跳转）

- `utils/index.js` PHP request 主逻辑  
- `pages/promote/exam.vue`、`task.vue` 等核心考前页业务  
- PHP `AppauthController` 登录签发（方案 A）

---

## 9. 测试计划（摘要）

| 用例 | 期望 |
|------|------|
| 仅用 PHP 老功能 | 与改前一致 |
| 同步学全流程 | 见 §3.5 |
| 班级刷题有/无 classId | 见 §4.4 |
| 作业老师+学员+插班 | 见 §5.6 / §6.4 |
| SaaS 高位 id 账号 | 预期登不上；文档注明，不作为 P0 阻塞除非做双写 |

---

## 10. 人员与协作

| 角色 | 职责 |
|------|------|
| App | P0/P1/P3 页面与 api |
| 后端 | P1 我的班级/classId；P2 作业全域 |
| 管理端 | P2 作业 Tab |
| 测试 | 三通道隔离回归 + 登录账号基线 |
| 运维 | sync 监控、租户与 SaaS 域名 |

---

## 11. 开工检查单（Kickoff）

- [ ] 确认测试学员在 **PHP** 可密码登录（默认 `123123`）  
- [ ] 确认 `platform.sync` 运行正常（新 token ≤3s 进 SaaS）  
- [ ] 确认 App `saas` base URL 可访问 `/appreviewsync/overview`  
- [ ] 产品确认三通道文案：同步练习 / 班级刷题 / 课堂作业  
- [ ] 排期确认：先 P0 还是等开户双写  

---

## 12. 一句话交付顺序

**P0 同步学 App → P1 我的班级+刷题带班 → P2 作业后端与跟班 Tab → P3 作业 App**；全程 PHP 登录 + `saasRequest`，promote 零破坏。

关联设计文档保持产品口径；本文作为研发排期与验收依据。
