# App 端：同步学 · 学员刷题 · 老师布置作业 — 设计实现方案

> 目标：在 `gjjd_appnext` 落地三类学习能力，且**不影响**现有 promote / PHP 主链路  
> 约束：先方案、后开发（本文不动代码）  
> 前置：方案 A（PHP 登录签发 + SaaS Bearer 透传，`saasRequest` 已落地）  
> 上游文档：  
> - [跟班作业与随堂练实现方案.md](./跟班作业与随堂练实现方案.md)  
> - [班级刷题学情方案.md](./班级刷题学情方案.md)  
> - [AI同步学（学员复习）实现方案.md](../AI同步学（学员复习）实现方案.md)  
> - [方案A_App登录PHP签发与SaaS令牌镜像验票实现方案.md](./方案A_App登录PHP签发与SaaS令牌镜像验票实现方案.md)  
> 编写日期：2026-08-06

---

## 1. 结论先看

在 App 上要同时支持「同步学 / 学员刷题 / 老师布置作业」，必须按**三条独立通道**设计入口与接口，禁止揉进 promote「课后作业」。

| 通道 | 用户感知名称（建议） | 谁发起 | App 入口 | 网络 | 后端 |
|------|----------------------|--------|----------|------|------|
| **C 同步学** | 同步练习 | 系统按计划 | 提升 → 复习任务 → **同步练习**（新增 Tab/页） | `saasRequest` → `/appreviewsync/**` | 已具备 |
| **A 班级刷题** | 班级刷题 / 练习 | 学员自助 | **我的班级** → 某班 → 刷题 | 现有 `/appsmart`（可加 `classId`）+ 可选拉 practice-scope | 范围已有；学情带班待补 |
| **B 跟班作业** | 课堂作业 / 随堂练 | 老师课后布置 | **我的班级** → 布置 / 待办 | `saasRequest` → `/training/follow/**` | 表与 API **未建**（M3） |

**登录**：继续 PHP 登录拿 `itoken`；调 SaaS 用 `Authorization: Bearer <itoken>`（已实现）。能登录的学员账号必须落在 **PHP `platform_user`**（SaaS 单开高位 id 账号 App 登不上，见分班开户双写专题）。

---

## 2. 信息架构（IA）

```text
Tab「提升」promote（现有，不改语义）
  ├─ 课后作业 / 复习任务 / 考试…     → 仍走 /apppromote（考前）
  └─ 【新增】同步练习                 → SaaS 同步学（通道 C）

【新增包】pages/follow/ 或 pages/my-class/
  ├─ 我的班级列表（学员+老师按角色）
  ├─ 班级首页
  │    ├─ 待办作业（B）
  │    ├─ 班级刷题（A）
  │    ├─ 课表摘要（只读，可选）
  │    └─ 老师：布置练习（B）
  ├─ 作业答题 / 结果（B）
  ├─ 布置向导（老师 B）
  └─ 任务进度（老师 B）

培训详情页（可选角标）
  └─ 「今日作业 N」→ 跳转跟班待办（不改原考试入口）
```

### 2.1 入口隔离原则

| 允许 | 禁止 |
|------|------|
| 新页面包 + `pages.json` 注册 | 改 `pages/promote/*` 把作业接到 `/apppromote` |
| 同步学挂在提升下**并列**新入口 | 用同步学接口交跟班作业 |
| 班级刷题继续 `/appsmart` | 刷题答卷写入 `train_follow_task_*` |
| 作业走 `saasRequest` | 作业走 PHP `request` 改旧接口 |

---

## 3. 三通道详细设计

### 3.1 通道 C — AI 同步学（学员备考）

**目标**：按三阶段规则每日排题、错题牌、模考；提升通过率。

| 项 | 设计 |
|----|------|
| 鉴权 | PHP 登录 → Bearer itoken；`X-Tenant-Id` 与现网约定一致 |
| API | 已有 `/appreviewsync/overview|today|question|answer|mock/*|finish|progress|explain` |
| 页面 | 新建页族（勿复用 promote 答题页强绑 exam_type） |
| 数据 | SaaS `ai_review_*`；异步回写 PHP `exam_result` type=5 |
| 与作业 | **默认弱耦合**；可选后期「作业错题注入错题池」（开关默认关） |

**App 页建议**

1. `sync-practice/home` — 计划概览 / 阶段  
2. `sync-practice/today` — 今日任务清单  
3. `sync-practice/answer` — 单题作答  
4. `sync-practice/mock` — 模考  
5. `sync-practice/progress` — 学情  

**依赖**：`platform.sync` 常开；账号在 PHP 可登录。

---

### 3.2 通道 A — 班级自助刷题

**目标**：在本班练习范围内自由刷题；后台可看班级学情。

| 项 | 设计 |
|----|------|
| 范围 | 管理端已配 `train_class_practice_*`；App 进班刷题前拉 scope |
| 抽题/交卷 | **保持**现有 `/appsmart`（PHP 或现网路径不变） |
| 班级上下文 | 请求增加可选 `classId`（及后续落库 `class_id`）；**无 classId 时行为与今天完全一致** |
| 入口 | 仅「我的班级 → 刷题」，不替换 promote 全局刷题 |

**最小改动策略**

1. 班级首页「刷题」带上当前 `classId` 跳进现有刷题页或薄封装页。  
2. 封装层把 `classId` 传给 appsmart；旧入口不传。  
3. 学情展示可先做管理端；App 学员侧可不展示班级排行（二期）。

---

### 3.3 通道 B — 老师布置作业 / 学员交作业

**目标**：下课按课次布置客观题；学员待办交卷；即时判分。

#### 3.3.1 后端（SaaS，App 之前或并行）

须先完成 M3-a（否则 App 无接口）：

- 表：`train_follow_task` / `_question` / `_item` / `_answer`  
- API 前缀：`/training/follow/**`（或文档已定路径）  
- 发布时花名册快照；插班补发  
- 挂载：`schedule_id` 优先；否则 `class_id + biz_date`

#### 3.3.2 老师 App 流程

```text
我的授课班级
  → 选班级
  → 选课次（train_schedule）或「按日」
  → 组题（首版自拟客观题；二期题库选题拷贝快照）
  → 预览 → 发布
  → 看进度（已交/未交）
```

身份：`train_people_list` 中本班 teacher / 主讲绑定；接口侧校验「可布置」。

#### 3.3.3 学员 App 流程

```text
我的班级 → 待办列表（homework + practice）
  → 拉卷 paper（不含答案）
  → 作答 → submit（服务端判分）
  → 结果页（按 show_analysis_after_submit）
```

身份：花名册 `people_id` ↔ `platform_user_id` ↔ 登录用户。

#### 3.3.4 网络层

```js
// 伪代码约定
saasRequest({
  url: '/training/follow/student/tasks',
  method: 'GET'
  // 自动 Bearer + X-Tenant-Id；不走 request()/PHP
})
```

新建 `api/follow-homework.js`，禁止写入 `api` 里混用 PHP promote。

---

## 4. 鉴权与账号（与登录问题对齐）

```text
登录（不变）     PHP /appauth/login → itoken
PHP 业务         request + istate
SaaS 同步学/作业 saasRequest + Bearer itoken
```

| 问题 | 设计要求 |
|------|----------|
| SaaS 单开账号登不上 App | 开户须写入 **PHP** `platform_user`（同 id 再镜像 SaaS），或先 PHP 再建 |
| 登录后立刻调 SaaS 401 | `saasRequest` 已 401 短重试；产品可提示稍候 |
| 老师无班级 | 接口返回空列表，引导联系教务挂师资 |

---

## 5. 页面与模块清单（建议目录）

```text
gjjd_appnext/
  api/
    follow-class.js          # 我的班级列表、课次（SaaS）
    follow-homework.js       # 布置/待办/拉卷/交卷（SaaS）
    sync-practice.js         # 同步学（SaaS /appreviewsync）
    # appsmart 仍用现有 PHP request 封装
  pages/
    sync-practice/           # 通道 C
    follow/                  # 通道 A+B 容器
      class-list.vue
      class-home.vue
      homework-todo.vue
      homework-answer.vue
      homework-result.vue
      teacher-assign.vue
      teacher-progress.vue
      class-practice.vue     # 跳转/包装 appsmart + classId
  utils/
    saasRequest.js           # 已支持 Bearer（方案 A）
```

`pages.json`：注册分包，避免打进 promote 主包体积过大。

---

## 6. 角色与权限矩阵（App）

| 能力 | 学员 | 主讲/教务老师 | 纯游客 |
|------|------|---------------|--------|
| 同步学 | ✓（有考试计划/工种） | 可选只读 | ✗ |
| 班级刷题 | ✓（在籍班级） | 可选试刷 | ✗ |
| 待办作业 | ✓ | ✗（看进度即可） | ✗ |
| 布置作业 | ✗ | ✓ | ✗ |
| promote 原功能 | 不变 | 不变 | 不变 |

---

## 7. 分期交付（推荐）

| 阶段 | 内容 | 依赖 | App 影响面 |
|------|------|------|------------|
| **P0** | 同步学页族接通 `/appreviewsync` | 账号 PHP 可登 + sync 开 | 仅新增页；promote 零改 |
| **P1** | 我的班级列表 + 班级刷题带 `classId` | practice-scope；appsmart 可选参 | 薄封装，旧刷题入口不变 |
| **P2** | SaaS M3-a 作业 API + 管理端作业 Tab | Flyway + 跟班 API | App 尚可不发 |
| **P3** | 老师布置 + 学员答题（作业闭环） | P2 | 新增 follow 分包 |
| **P4** | 可选：作业错题 → 同步学错题池 | 产品开关 | 默认关 |

建议发版节奏：**先 P0 同步学**（后端已齐）→ **P1 班级刷题上下文** → **等 P2 后端就绪再发 P3 作业**。

---

## 8. 与「上完课布置对应练习」的对应关系

| 产品话术 | 落在哪条通道 |
|----------|----------------|
| 老师按本节课布置练习 | **B**，`schedule_id` = 本节 `train_schedule.id` |
| 学员课后完成老师布置 | **B** 待办 |
| 学员自己按班级范围多刷 | **A** |
| 学员备考认定每日练习 | **C** 同步学 |
| 提升里原来的课后作业 | **仍是 promote**，与 B/C 文案区分：「考前练习」vs「课堂作业」vs「同步练习」 |

---

## 9. 验收标准（App）

### 9.1 互不影响

- [ ] 未登录 / 仅 PHP 业务：提升、考试、商城与改前一致  
- [ ] 同步学、跟班页崩溃不影响 promote Tab  
- [ ] 无 `classId` 的刷题路径与现网一致  

### 9.2 同步学

- [ ] PHP 登录后进入同步练习 → overview 成功  
- [ ] 今日题作答、模考、学情可走通  

### 9.3 班级刷题

- [ ] 从班级进入刷题请求带 classId；学情侧能关联（若后端已落库）  
- [ ] 未配 scope 时有明确提示或回退策略  

### 9.4 作业

- [ ] 老师发布后学员待办可见；交卷得分；管理端跟班 Tab 可见  
- [ ] 插班生自动出现待办  

---

## 10. 风险与前置

| 风险 | 处理 |
|------|------|
| 学员只在 SaaS 开户 | App 登不上 → 开户改「先 PHP 后 SaaS」或迁号（独立任务） |
| 作业 API 未就绪就做 App | 空转；务必 P2 先于 P3 |
| 文案混淆三通道 | 统一命名：同步练习 / 班级刷题 / 课堂作业 |
| 老师身份不准 | 以花名册 type + 开班师资为准，缺数据时后台先挂人 |

---

## 11. 推荐落地顺序（一句话）

1. **账号可登**（PHP 有人）+ **sync 常开**  
2. App **同步学页**（P0）  
3. App **我的班级 + 刷题带班**（P1）  
4. 后端 **作业四表+API**（P2）  
5. App **布置/答题**（P3）  

三条通道、两套网络（`request` vs `saasRequest`）、三套文案入口——这是在不影响现有 App 功能前提下的唯一稳妥设计。
