# 现有培训移动端源码与新竞赛 H5 逐文件对比

## 1. 文档范围

- 前端工程：`D:\qianlvguoyu\qianlvguoyu_saas-dev\gjjd_appnext`
- 项目来源：`https://gitee.com/guangxi-guoyu-digital_admin/gjjd_appnext.git`
- 对比对象：现有培训移动端功能与新增竞赛 H5 功能。
- 后端/Agent/MCP 工程：`D:\qianlvguoyu\qianlvguoyu_saas-git`
- 本文定位：为下一步设计竞赛选手端 App 提供前端结构、接口边界和改造依据。

## 2. 总体结论

新竞赛 H5 不是一个独立 App，而是现有 uni-app 前端中的一个业务模块。它与培训移动端共用以下基础设施：

- `App.vue`、`main.js`、`manifest.json`、`pages.json`
- 用户登录态、Pinia 用户 Store、全局样式和构建配置
- H5、App、微信小程序等多端编译入口
- 部分通用页面：用户中心、登录、上传、商品/公共能力

竞赛模块主要通过 `pages/competition`、`api/competition.js`、`store/competition.js` 和 `utils/java.js` 扩展出来。后续设计竞赛选手端 App 时，建议继续沿用现有工程的“共享壳层 + 竞赛业务模块”模式，不建议另起一套前端工程。

## 3. 工程与运行关系

```text
gjjd_appnext
├─ App.vue / main.js / manifest.json / pages.json   共享 App 壳层
├─ pages/index、promote、exam、task、team、registration  培训移动端
├─ pages/competition                              新竞赛 H5
├─ pages/user、pages/shop                          共享或公共业务
├─ api/competition.js                              竞赛接口封装
├─ api/promote.js                                  培训推广接口封装
├─ store/user.js                                   共享用户状态
├─ store/competition.js                            竞赛状态
├─ utils/index.js、utils/util.js                   原有通用请求与工具
└─ utils/java.js、utils/response.js                竞赛侧请求与响应处理

qianlvguoyu_saas-git
├─ services/platform-api                           Spring Boot 后端接口
└─ services/mcp-server                             Agent / MCP Server
```

当前平台后端的主要关系如下：

| 层级 | 工程/路径 | 主要职责 | 当前状态 |
|---|---|---|---|
| 前端 | `gjjd_appnext` | 培训移动端、竞赛 H5、用户交互 | 已包含竞赛页面与接口调用 |
| 平台 API | `services/platform-api` | 登录、首页、培训、用户、报名、智能/教练等接口 | 默认本地端口 8080，Context Path 为 `/api` |
| MCP Server | `services/mcp-server` | Agent 工具与 MCP 通信 | 默认端口 3001，提供 `/mcp`、`/sse`、`/messages` |

平台 API 当前已有 `/appauth`、`/appsms`、`/apphome`、`/appuser`、`/appregistration`、`/appsmart`、`/appcoach` 等接口，但未发现完整的 `appcompetition` Controller。前端已经调用大量 `/appcompetition/*` 接口，因此竞赛选手端正式落地前，需要补齐后端接口契约或确认这些接口由其他服务提供。

MCP Server 当前主要提供认证相关的只读工具：

- `certification.searchTasks`
- `certification.getTaskOverview`
- `certification.getCollectionProgress`
- `certification.getPracticalScoreProgress`

它目前不是竞赛业务 API 的替代品。竞赛报名、赛程、答题、排名、PK 等核心交互仍应由平台 API 或竞赛专用服务承载；Agent/MCP 可作为智能问答、学习诊断、任务推荐等增强能力。

## 4. 入口级文件对比

| 文件 | 培训移动端作用 | 新竞赛 H5 作用 | 结论 |
|---|---|---|---|
| `App.vue` | 全局登录态、WebSocket、用户状态初始化 | 同样生效 | 共享，不应复制 |
| `main.js` | Vue3 + Pinia 主入口，同时保留 Vue2 兼容代码 | 竞赛页面复用同一入口 | 共享 |
| `manifest.json` | H5/App/小程序打包配置 | 没有单独竞赛 App ID | 竞赛不是独立应用 |
| `pages.json` | 注册全部页面和底部 Tab | 注册约 25 个竞赛路由 | 共享路由表 |
| `utils/util.js` | 通用导航、登录、域名、配置 | 竞赛部分也会使用 | 共享工具，但存在多套请求配置 |
| `utils/index.js` | 原培训请求、上传、用户/考试能力 | 个别竞赛页面仍直接使用 | 旧请求层 |
| `utils/java.js` | 非原培训主路径 | 竞赛接口请求、上传、鉴权处理 | 竞赛请求层 |
| `utils/response.js` | 原培训页面使用较少 | 竞赛页面较多使用 | 竞赛响应适配层 |
| `store/user.js` | 用户 ID、用户信息、Token、Client ID | 竞赛报名和答题依赖 | 共享用户 Store |
| `store/competition.js` | 无对应培训状态 | 当前赛事、赛事信息、报名/参与/阶段/风采等 | 竞赛专属 Store |
| `api/index.js` | 汇总公共 API | 暴露竞赛和推广 API | 共享 API 出口 |

## 5. 模块规模对比

按当前源码目录统计：

| 范围 | 规模/特征 |
|---|---|
| `pages/competition` | 约 39 个 Vue 文件，另有 2 个 CSS 文件 |
| 培训核心目录：`index`、`promote`、`exam`、`task`、`team`、`registration` | 约 44 个 Vue 文件 |
| `pages.json` | 共约 94 个注册路由 |
| 竞赛注册路由 | 约 25 个 |
| `api/competition.js` | 42 个竞赛接口函数 |
| `api/promote.js` | 4 个培训推广/任务聚合接口 |

这说明竞赛模块已经具备较完整的业务页面规模，但目前仍处于“功能模块嵌入既有培训 App”的状态，尚未完成独立的选手端产品抽象。

## 6. 页面逐文件映射

### 6.1 首页与业务入口

| 培训移动端 | 新竞赛 H5 | 对比说明 |
|---|---|---|
| `pages/index/index.vue` | `pages/competition/home/index.vue` | 培训综合发现页 vs 赛事首页 |
| `pages/promote/index.vue` | `pages/competition/entrance/index.vue` | 培训内容聚合入口 vs 竞赛功能入口 |
| `pages/promote/home.vue` | `pages/competition/home/index.vue` | 培训推广首页与赛事首页存在入口职责重叠 |
| `pages/promote/task.vue` | `pages/competition/entrance/index.vue` | 培训任务聚合与赛事参赛入口 |
| `pages/promote/learn.vue` | `pages/competition/learning/content-list.vue` | 培训课程列表 vs 竞赛学习内容列表 |
| `pages/promote/kejian.vue` | `pages/competition/learning/content-detail.vue` | 培训课件详情 vs 竞赛内容详情 |

### 6.2 学习、考试与错题

| 培训移动端 | 新竞赛 H5 | 对比说明 |
|---|---|---|
| `pages/exam/index.vue` | `pages/competition/learning/exam-mock.vue` | 培训考试入口 vs 竞赛模拟考试 |
| `pages/exam/detail.vue` | `pages/competition/learning/exam-mock.vue` | 考试详情/答题能力在竞赛侧合并 |
| `pages/exam/result.vue` | `pages/competition/learning/exam-result.vue` | 结果页对应关系较直接 |
| `pages/exam/rand.vue` | `pages/competition/pk/*`、`pages/competition/adventure/*` | 随机练习 vs 竞赛 PK/闯关玩法 |
| `pages/exam/sort.vue` | `pages/competition/pk/*` | 分类考试与竞赛专项挑战 |
| `pages/promote/wrong.vue` | `pages/competition/learning/wrong-list.vue` | 错题入口对应 |
| `pages/promote/wrongList.vue` | `pages/competition/learning/wrong-list.vue` | 错题列表能力对应 |
| `pages/promote/answer.vue` | `pages/competition/learning/exam-mock.vue` | 培训答题页与竞赛答题页不是一一复用 |

### 6.3 报名、团队与用户

| 培训移动端 | 新竞赛 H5 | 对比说明 |
|---|---|---|
| `pages/registration/index.vue` | `pages/competition/home/components/RegistrationSection.vue` | 培训报名页面 vs 首页报名区块 |
| `pages/registration/detail.vue` | `pages/competition/home/detail.vue` | 报名/赛事详情能力有重叠 |
| `pages/registration/result.vue` | `pages/competition/home/detail.vue` 或报名结果逻辑 | 竞赛报名流程部分嵌入赛事详情 |
| `pages/team/index.vue` | `pages/competition/pk/team-pk.vue` | 培训团队 vs 竞赛团队 PK |
| `pages/team/detail.vue` | `pages/competition/pk/team-pk.vue` | 团队详情能力在竞赛玩法中重组 |
| `pages/user/*` | `pages/user/*` | 竞赛与培训共用用户中心、登录态和资料能力 |
| `pages/shop/*` | 无明显竞赛对应页 | 主要为公共/商业功能，暂不属于选手端核心 |

### 6.4 竞赛页面内部结构

竞赛模块包含以下主要目录：

```text
pages/competition
├─ home
│  ├─ index.vue
│  ├─ detail.vue
│  └─ components
│     ├─ SearchBar.vue
│     ├─ RegistrationSection.vue
│     ├─ ParticipationSection.vue
│     ├─ CurrentStageSection.vue
│     ├─ NewsSection.vue
│     └─ StyleDisplaySection.vue
├─ entrance
│  ├─ index.vue
│  ├─ components/PageHeader.vue
│  ├─ components/CompetitionList.vue
│  ├─ components/KnowledgePKModule.vue
│  ├─ components/OnlineLearningModule.vue
│  ├─ components/CommunityQAModule.vue
│  ├─ components/UserScorePanel.vue
│  └─ components/RulesModal.vue
├─ learning
│  ├─ content-list.vue
│  ├─ content-detail.vue
│  ├─ article-detail.vue
│  ├─ wrong-list.vue
│  ├─ exam-mock.vue
│  └─ exam-result.vue
├─ pk
│  ├─ index.vue
│  ├─ team-pk.vue
│  └─ pk.vue
├─ adventure
├─ ranking.vue
├─ notification.vue
├─ register.vue
├─ checkin.vue
├─ qa.vue
└─ ...
```

与培训页面普遍采用单文件、大量 Options API、页面内直接请求相比，竞赛页面已经开始使用组件拆分、Pinia、专用 API 模块和响应适配器，结构更接近可持续扩展的产品模块。

## 7. API 逐层对比

### 7.1 培训/推广 API

`api/promote.js` 当前主要包含：

- `/apppromote/index`
- `/apppromote/trainingList`
- `/apppromote/trainingPlanList`
- `/apppromote/competitionTaskList`

这些接口偏向培训内容、训练计划和任务聚合，关注“学什么、练什么、任务入口在哪里”。

### 7.2 竞赛 API

`api/competition.js` 当前约有 42 个导出函数，覆盖以下域：

| 领域 | 典型能力 |
|---|---|
| 赛事首页 | 赛事列表、赛事详情、赛事信息、搜索 |
| 赛事资讯 | 通知、新闻、文章、风采 |
| 参赛流程 | 报名、报名信息、报名校验、签到 |
| 参与状态 | 当前阶段、用户参与状态、得分面板 |
| 竞赛玩法 | 知识 PK、团队 PK、闯关/冒险 |
| 学习内容 | 课程、课件、内容详情 |
| 题库能力 | 错题、模拟考试、考试结果 |
| 互动能力 | 社区问答、排行榜 |

竞赛 API 关注“参加哪场赛事、当前处于什么阶段、成绩如何、下一步做什么”，与培训 API 的目标不同。

### 7.3 当前存在的协议不统一

竞赛代码并非全部通过 `api/competition.js`，目前至少存在三类调用方式：

1. `api/competition.js` 中的 `/appcompetition/*` 接口。
2. `pages/competition/home/detail.vue` 直接通过通用 `request` 调用 `/match/info`。
3. `pages/competition/ranking.vue`、`pages/competition/pk/pk.vue` 等页面直接调用 `/appcompetition/ranking` 或 `/match/exam-pk`、`/match/exam-pk-submit`。

因此，竞赛选手端继续开发前应先确定统一 API 契约，避免同一业务同时存在 `/appcompetition/*` 与 `/match/*` 两套协议。

## 8. 请求层与响应层对比

| 文件 | 主要使用方 | 特征 |
|---|---|---|
| `utils/index.js` | 培训、用户、旧考试页面 | 历史通用请求封装，兼容功能较多 |
| `utils/util.js` | 全局页面与工具 | 路由、登录、环境、域名等公共逻辑 |
| `utils/java.js` | 竞赛页面 | 面向竞赛 Java 服务的请求/上传封装 |
| `utils/response.js` | 竞赛页面 | 对竞赛接口响应做统一判断或转换 |

当前配置中存在两个重要后端域名：

- 普通业务域名：`https://gjjdp.gjjd168.com`
- 竞赛 Java 域名：`https://java.app.gjjd168.com`

本地调试时普通平台 API 可通过 `http://localhost:8080/api` 验证；竞赛接口是否已在本地或远程服务部署，需要单独确认。正式 App 设计时建议将 API Host、认证方式、超时、错误码和文件上传策略集中到环境配置中。

## 9. 状态管理对比

### 9.1 共享用户状态

`store/user.js` 保存：

```text
userId
userInfo
token
clientId
```

培训和竞赛均依赖该 Store，竞赛报名、签到、答题、成绩查询都需要共享登录态。

### 9.2 竞赛专属状态

`store/competition.js` 主要保存：

- `currentMatchId`
- 赛事列表和赛事信息
- 报名状态、参与状态
- 当前阶段
- 风采/资讯等列表状态

后续选手端建议继续增加明确的领域状态，例如：

- 当前赛事与当前赛季
- 选手报名资料草稿
- 当前赛程/阶段状态
- 答题会话、倒计时、提交状态
- 个人积分、团队积分、排行榜快照
- 消息未读数与赛事通知

答题过程中的临时状态不宜全部放入全局 Store，可按页面生命周期或独立 composable 管理，避免切换赛事后残留旧数据。

## 10. 已发现的页面与路由问题

### 10.1 竞赛详情路由引用不一致

以下培训入口页面引用了：

```text
/pages/competition/detail
```

但 `pages.json` 中注册的竞赛详情路由是：

```text
/pages/competition/home/detail
```

涉及页面包括：

- `pages/promote/index.vue`
- `pages/promote/home.vue`
- `pages/promote/task.vue`

建议统一使用已注册路由，或新增兼容路由后再逐步收敛。

### 10.2 未注册页面

- `pages/competition/learning/article-detail.vue` 存在，但当前未在 `pages.json` 中注册。
- `pages/team/index_.vue`、`pages/team/detail_.vue` 存在，但看起来是历史备份/未启用文件。

建议清理未使用文件，或明确标记为实验页面，避免后续误用。

### 10.3 竞赛请求层旁路

部分竞赛页面绕过 `api/competition.js` 直接调用请求函数。这会导致：

- API 路径和参数定义分散。
- 鉴权、错误处理和响应结构不一致。
- 测试、Mock 和接口替换成本上升。

建议将所有竞赛请求收敛到 `api/competition.js` 或按领域拆分为 `api/competition/*.js`。

## 11. 样式与资源对比

培训移动端的样式更多分散在页面文件、公共样式和历史组件中，整体偏传统业务页面；竞赛模块已经出现局部样式文件：

- `pages/competition/home/styles/home.css`
- `pages/competition/entrance/styles/cards.css`

竞赛页面具有更明显的卡片化、赛事阶段、成绩面板和模块化内容区。后续选手端设计可以把竞赛视觉规范抽取为统一 Token：

- 赛事主色、阶段色、成绩色
- 卡片圆角、阴影、间距
- 按钮、标签、进度条、排行榜单元格
- 空状态、加载状态、错误状态
- 移动端安全区和底部操作栏

这样可以避免竞赛首页、答题页、PK 页和排行榜各自定义一套视觉规则。

## 12. 现有培训移动端与竞赛 H5 的核心差异

| 维度 | 培训移动端 | 新竞赛 H5 |
|---|---|---|
| 产品目标 | 学习、训练、考试、任务完成 | 赛事参与、阶段推进、竞技、排名、荣誉展示 |
| 信息架构 | 内容/任务/考试导向 | 赛事/赛程/成绩/互动导向 |
| 页面形态 | 单页面功能较多，历史代码较重 | 入口页 + 模块组件 + 赛事流程 |
| API 组织 | 通用请求或页面内直接调用较多 | 已有竞赛 API 层，但仍有旁路调用 |
| 状态 | 用户状态为主，页面内状态较多 | 用户状态 + 当前赛事/阶段/成绩状态 |
| 互动强度 | 以学习记录和任务为主 | 报名、签到、答题、PK、排名、问答 |
| 结果反馈 | 考试分数、学习进度 | 赛事积分、阶段晋级、团队排名、荣誉 |
| 设计重点 | 可读性、内容浏览、任务完成 | 强反馈、强节奏、状态清晰、操作可靠 |

## 13. 面向竞赛选手端 App 的建议架构

建议将当前代码逐步整理为以下结构：

```text
src/
├─ app/                    # App 启动、全局配置、权限、环境
├─ shared/                 # 通用组件、请求、错误码、用户、上传
├─ features/training/      # 培训移动端
│  ├─ pages/
│  ├─ api/
│  └─ store/
├─ features/competition/   # 竞赛选手端
│  ├─ pages/
│  │  ├─ home/
│  │  ├─ entrance/
│  │  ├─ learning/
│  │  ├─ pk/
│  │  ├─ adventure/
│  │  └─ profile/
│  ├─ components/
│  ├─ api/
│  ├─ store/
│  ├─ composables/
│  └─ styles/
└─ pages.json
```

在不立即大规模迁移目录的前提下，也可以先采用渐进式方案：

1. 保持现有路由和页面位置不变。
2. 统一竞赛 API 封装和请求层。
3. 抽取赛事卡片、阶段条、成绩面板、答题操作栏等组件。
4. 为竞赛增加页面级权限和赛事上下文校验。
5. 再逐步把竞赛页面迁移到 `features/competition`。

## 14. 后续开发优先级

### P0：先打通工程和接口契约

- 修正 `/pages/competition/detail` 与 `/pages/competition/home/detail` 路由不一致。
- 清点并确认 `/appcompetition/*`、`/match/*` 两套接口协议。
- 与 `platform-api` 对齐竞赛接口 Controller、DTO、错误码和鉴权方式。
- 明确竞赛 Java 服务的本地、测试、生产地址。
- 确认报名、签到、考试提交、PK 提交等接口的幂等策略。

### P1：形成选手端完整闭环

推荐最小闭环：

```text
登录
  → 赛事列表/赛事首页
  → 赛事详情
  → 报名与报名结果
  → 赛程/当前阶段
  → 学习或练习
  → 模拟考试/正式答题
  → 提交与成绩
  → 排名/团队 PK
  → 通知与复盘
```

### P2：增加 Agent/MCP 增强能力

- 根据选手学习记录推荐训练内容。
- 根据错题和成绩生成阶段性复习建议。
- 提供赛事规则、赛程和报名材料问答。
- 通过 MCP 查询认证/任务进度，并在前端形成智能提醒。
- 对接人工教练或赛事运营人员的辅助工作流。

## 15. 开发和部署现状

当前前端已完成本地 H5 构建与静态部署验证：

```text
pnpm install
pnpm run build:h5
pnpm run dev:h5
```

开发服务地址：`http://localhost:5174`

静态构建产物可通过本地静态服务器验证，例如：`http://localhost:4174`。

平台后端健康检查已验证：

```text
http://localhost:8080/api/actuator/health
```

需要注意：前端能成功构建不代表竞赛业务接口已经完整可用。当前最主要的部署前置条件是确认竞赛接口服务、跨域配置、Token 认证、文件上传地址以及 H5/APP 环境变量。

## 16. 最终判断

现有代码已经提供了竞赛选手端的页面雏形和较完整的业务 API 调用面，但仍需要完成三项关键工作：

1. 把竞赛功能从“嵌入式页面集合”整理成清晰的选手端产品流程。
2. 统一竞赛接口、请求层、状态管理和路由，消除历史培训代码与新竞赛代码之间的边界混乱。
3. 与 `qianlvguoyu_saas-git` 中的 `platform-api`、MCP Server 及可能存在的竞赛服务完成接口契约和部署拓扑对齐。

完成以上工作后，当前 `gjjd_appnext` 可以作为竞赛选手端 App 的前端基础继续演进，培训移动端则作为共享学习能力和用户体系保留。
