# AGENTS.md

> **本文件被以下 Agent 工具读取**(优先级从上到下):
> - **MiniMax Code / Mavis** — 团队阶段,owner + producer + verifier 协作
> - **Claude Code** — 单 agent 阶段,本文件等同 CLAUDE.md
> - **Cursor** — 单 agent 阶段,通过 `Cursor → Settings → Rules` 自动同步
> - **Qoder** — 通过 `.qoder/agents/builtin/*.md` 引用本文件核心段
> - **OpenCode / Codex / Gemini CLI** — 直接读取根目录 `AGENTS.md`
>
> **若你是 producer**:只读"必读文档优先级 + 编码规范 + 已知坑",不要碰协作协议段。
> **若你是 verifier**:必读"多 Agent 协作协议 + 测试与验收 + 硬上限规则"。
> **若你是 owner / plan 编排**:必读全部。

本文件给在本项目中工作的 AI Agent / Codex 使用。进入仓库后先阅读本文件,再按需加载对应目录下的文档。

> **项目 Wiki**：`.qoder/repowiki/` 目录下存放项目知识库（Wiki 文章 + Knowledge Cards），包含架构概览、技术选型、编码规范、常见踩坑等内容。Agent 在处理任务前应优先查阅相关 Wiki 条目以获取上下文。

## 阅读顺序

按优先级从高到低，Agent 在处理任务时应按需逐层深入：

1. `README.md` — 项目概述、目录结构、技术栈、一期范围、一键启动
2. `docs/README.md` — 文档分区说明
3. `docs/04_requirements/phase1/README.md` — 一期可编码规格
4. `docs/04_requirements/phase1/01_一期MVP功能清单.md` — 一期范围与不做范围
5. `docs/04_requirements/phase1/02_基础表结构设计.md` — 涉及数据库时必须先读
6. `docs/04_requirements/phase1/03_第一批API合同.md` — 涉及接口时必须先读
7. `docs/04_requirements/phase1/04_前端页面清单.md` — 涉及前端页面时必须先读
8. `docs/04_requirements/phase1/05_测试验收清单.md` — 提交实现前对照验收
9. `docs/05_decisions/开发框架选型建议.md` 与 `docs/05_decisions/云平台技术路线选型建议.md` — 技术选型或架构变更时必须遵守

---

## 当前项目状态

### 已实现
- **平台底座**：租户管理、组织管理、用户管理、角色权限、菜单权限（`system/`、`auth/`、`tenant/`、`security/`）
- **权限框架**：Sa-Token Token 管理（3天过期、并发登录），Spring Security `@PreAuthorize` 鉴权，SaTokenBridgeFilter 桥接（`security/filter/`）
- **多租户**：`TenantContextHolder`（ThreadLocal） + `TenantContextFilter`（Header 解析）+ MyBatis-Plus `TenantLineInnerInterceptor`（SQL 自动拼 tenant_id）
- **全局异常处理**：`GlobalExceptionHandler` 覆盖 Sa-Token 异常（NotLogin/NotPermission）、参数校验、业务异常
- **知识库**：知识库 CRUD、文档管理、权限控制（`knowledge/`）
- **题库管理**：
  - 工种管理：树形结构 CRUD、职业编码/级别管理（`exam/`）
  - 科目管理：分页 CRUD、工种关联（`exam/`）
  - 专家授权管理：专家-科目多对多授权（命题/审题/终审）、聚合分页（`exam/`）

- **数据库**：13 个 Flyway 迁移已就绪（含初始化表、Demo 数据、知识库表、工种表、科目表、授权表、查证表）
- **查证管理**：查证用户管理、证书查询、Excel 导入导出、API 查证
  - 用户管理、Excel 导入导出、API 查证接口（`chazheng/`）
  - 证书查询与管理（`chazheng/`）
- **前端管理端**：
  - 登录页 + AuthGate 权限门控
  - AppShell 布局 + 侧栏菜单
  - 知识库页面
  - 题库管理：工种管理、科目管理、专家授权管理页面（`apps/admin-web/src/pages/exam/`）
  - 查证管理：用户列表页面（`apps/admin-web/src/pages/chazheng/`）
  

### 空壳/待实现
- 学员端（`apps/student-web/`）未创建

### 独立服务状态
- `services/ai-gateway/`：仅 README，无代码
- `services/mcp-server/`：仅 README，无代码
- `services/file-service/`：仅 README，无代码
- `services/workflow-service/`：仅 README，无代码

---

## 目录放置约束

目录结构详见 `README.md`。Agent 必须遵守以下放置规则：

1. 新需求、流程图、业务表格 → `docs/04_requirements/`
2. 技术路线、架构决策、选型记录 → `docs/05_decisions/`
3. HTML 原型 → `prototypes/`，不可混入正式前端源码
4. 正式 React 等前端代码 → `apps/`
5. Java、AI Gateway、MCP、文件、工作流等后端代码 → `services/`
6. 生成产物 → `outputs/`，不可当作需求源头
7. 不提交真实密钥、本地缓存、`.DS_Store`、临时文件和大型二进制素材

---

## 技术约束

技术栈详见 `README.md` 和 `docs/05_decisions/`。以下是与代码实现直接相关的约束。

### 总体架构口径

1. Java / Spring Boot 做核心业务主干，一期采用可控服务化，不做全量微服务
2. 核心业务集中在 `services/platform-api/`，不要拆出 `course-service`、`exam-service` 等细业务服务
3. AI、MCP、文件、工作流保持独立工程边界，但一期可先在 `platform-api` 内用包隔离
4. Flowable 管核心业务流程状态，n8n 只做内部自动化/通知/定时

### 编码和文件规范
- 所有源码、配置、Markdown 文件使用 UTF-8 without BOM
- 生成后验证中文内容在 git diff 中正常显示

### 防御性编程
- 所有 MyBatis Mapper 返回值做 null 检查后再使用
- 所有外部 API/网络请求的响应先做空值和类型检查
- 认证、授权、权限校验路径必须打印 warn/error 级别日志

### 后端规则

包结构（`services/platform-api/src/main/java/com/guoyu/platform/`）：

```
common/  config/  security/  tenant/  system/
file/  workflow/  course/  question/  exam/  training/
ai/  chazheng/  integration/
```

分层约束：

| 层 | 命名 | 职责 |
|---|---|---|
| Controller | `*Controller` | REST API，参数接收、权限入口、结果返回 |
| Application | `*ApplicationService` | 用例编排、事务边界、跨领域协调 |
| Domain | `*DomainService` | 核心业务规则 |
| Repository | `*Repository` / `*Mapper` | MyBatis-Plus 数据访问 |
| Entity | `*Entity` | 数据库表映射（不直接暴露给前端） |
| DTO | `*CreateReq`、`*UpdateReq`、`*QueryReq` | 请求对象 |
| VO | `*VO`、`*DetailVO` | 返回对象 |
| Converter | `*Converter` | MapStruct 转换 |

执行规则：
#### API 响应规范
- 统一返回结构 `{ code, message, data }`，分页数据包裹在 `data` 中，含 `total`、`page`、`size`
- 所有列表接口使用同一分页参数（PageQuery），响应统一分页结构（PageResponse）

#### 审计日志
- 所有写操作记录操作日志

#### 数据访问与隔离
- Controller 不直接写 SQL，Entity 不直接暴露给前端（[分层约束](#分层约束)）
- 涉及企业/项目/用户的数据必须带 `tenant_id` 和数据权限过滤
- 文件上传只返回文件 ID、文件名、大小、类型和访问策略，不存文件内容到数据库
- 逻辑删除统一用 MyBatis-Plus 的 deleteById / deleteBatchIds，禁止手动 setDeleted=1 + updateById
- 多租户过滤通过 MyBatis-Plus 多租户插件自动处理，不要在业务代码里手动拼接 tenant_id 条件
- 所有列表接口返回统一分页结构 { total, page, size, records }

#### 认证与权限
- 后端是权限最终裁决点，前端只做展示控制
- AI / MCP 调用必须走受控 API 或 Application 层，不能绕过权限直接访问 Mapper
- 使用 Sa-Token 时，确认当前 token 类型是否支持 extra 持久化（tik/jwt/redis），不支持的从数据库实时查
- buildPrincipal 中对 roles/permissions 做 List.of() 兜底

#### 异常处理
- 全局异常处理器覆盖：认证失败、权限不足、参数校验失败、业务异常
- 不要在 filter/interceptor 中静默吞异常

### 数据库规则

1. MySQL 8.4，主键使用雪花 ID，类型统一 `BIGINT`
2. 多租户字段统一 `tenant_id`，逻辑删除通过 `deleted` 字段实现（MyBatis-Plus 自动处理，不手写拼接条件）
3. 业务主表必须包含：`id`、`tenant_id`、`created_at`、`updated_at`、`created_by`、`updated_by`、`deleted`
4. 使用 Flyway 管理迁移，脚本放在 `services/platform-api/src/main/resources/db/migration/`,涉及数字串等配置值时，YAML 中的版本号、数字串字段使用引号包裹
5. 包含种子数据的 Flyway 迁移必须幂等，可重复执行不报错,不要修改已提交/已应用的 Flyway 迁移文件内容，新增迁移应使用新版本号
6. 迁移命名：`V{YYYYMMDD}_{序号}__{描述}.sql`
7. 空库必须能执行全部 Flyway 迁移成功
8. 业务主状态以 MySQL 为准，Redis 只用于缓存/会话/验证码/限流

### 前端规则

管理端（`apps/admin-web/`）技术栈见 README。建议目录：

```
src/
  app/  routes/  layouts/  components/  services/
  stores/  hooks/  utils/  types/
  pages/
    dashboard/  system/  course/  question/
    exam/  training/  workflow/  ai/
```

#### HTTP 错误处理
- apiClient 响应拦截器必须覆盖 401/403/500 三种错误码：
  - 401：清除 store 中 token + currentUser，让 AuthGate 展示登录表单
  - 403：展示权限不足提示，不自动跳转
  - 500：区分代理错误和业务错误

#### 登录态管理
- AuthGate 使用 store 中 currentUser（同步）作为主判定条件，避免仅依赖异步 /api/auth/me
- 页面刷新后：先取 localStorage token → 同步展示 UI → 后台异步验证 /api/auth/me
- /api/auth/me 返回 401 时静默清除本地 token，展示登录表单
- 登录成功后同时写入 token、tenantId、currentUser 到 store

#### 多租户
- 请求拦截器在 Authorization header 之外附加 X-Tenant-Id header
- tenantId 持久化到 localStorage，页面刷新后恢复

执行规则：

1. 页面级数据请求使用 TanStack Query
2. 用户信息、菜单、主题、折叠状态使用 Zustand
3. 表格优先 ProTable，表单优先 ProForm，图表统一 ECharts
4. 菜单从后端动态加载，不在前端写死
5. HTML 原型仅作参考，不在原型文件上堆正式功能
6. 接口类型放入 `types/`（后续可用 OpenAPI 生成）
7. 需兼容局域网 HTTP 访问

### AI、Agent 与 MCP 规则

1. AI Gateway 和 MCP Server 不直连业务数据库，只调用 `platform-api` 的受控 API
2. `platform-api` 可保留 `ai/` 包用于 AI 业务记录、权限校验和草稿落库
3. 写操作默认生成草稿或待办，课程发布/审批通过/删除核心数据必须人工确认
4. AI 生成内容必须标记来源、模型、生成时间和操作人
5. MCP Tool 必须定义名称、用途、参数 schema、权限要求和审计日志
6. 所有工具调用必须可追踪到用户、租户、工具名和请求 ID

### 工作流与 n8n 规则

1. Flowable 管核心业务流程状态、审批、驳回、留痕
2. n8n 只做内部自动化、通知、定时、接口同步和 AI 草稿触发
3. 不向客户开放 n8n 编辑器或执行后台
4. BPMN 文件放 `workflows/bpmn/`，n8n 导出放 `workflows/n8n/`

---

## 测试与验收

后端：JUnit 5 / Mockito / Spring Boot Test + MockMvc / Testcontainers / Spring Security Test

前端：Vitest / React Testing Library / Playwright / MSW

最低验收：

1. 登录、菜单、权限、数据权限必须有测试
2. 数据库迁移脚本必须能在空库执行成功
3. 文件上传必须验证成功和失败路径
4. AI 和 MCP 工具调用必须验证权限失败、参数错误和人工确认
5. 一期主链路可跑通：管理员配置基础数据 → 课程人员建课建题 → 课程发布审批 → 安排考试 → 学员学习答题查看成绩错题

---

## 本地开发

### 环境路径

| 工具        | 路径                                                      |
|-----------|---------------------------------------------------------|
| JDK 21    | `D:\Program Files\Java\oracleJdk-21`                    |
| Maven     | `C:\Program Files\Java\apache-maven-3.9.11\bin\mvn.cmd` |
| Node.js   | `D:\Program Files\nodejs`                               |
| MySQL cli | `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe` |

### 环境变量

模板文件：`infra/env/platform-api.env.example`

本地开发必需变量：

| 变量 | 说明                           |
|---|------------------------------|
| `SERVER_PORT` | 后端端口，默认 8080                 |
| `MYSQL_URL` | 本地 MySQL jdbc:mysql://192.168.1.200:3308/gjjd_test |
| `MYSQL_USERNAME` | 数据库用户 root                   |
| `MYSQL_PASSWORD` | 数据库密码 guoyu_dev!513          |
| `JWT_SECRET` | 至少 32 位随机字符串                 |

Redis 不是当前代码路径的硬依赖，可跳过。

### 常用命令

```bash
# 后端构建
$env:JAVA_HOME = "D:\Program Files\Java\oracleJdk-21"
mvn.cmd compile -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml

# 后端直接启动（使用预构建 JAR）
java -jar services/platform-api/target/platform-api-0.1.0-SNAPSHOT.jar

# 前端开发服务器
cd apps/admin-web && npm run dev

# 访问
# 前端: http://localhost:5173
# 后端: http://localhost:8080/api
```

### Demo 账号

- 租户：`demo`
- 用户：`admin`
- 密码：`Admin@123456`

---

## Agent 工作方式

1. 修改前先确认任务属于文档、原型、正式前端、后端服务、脚本、数据、基础设施或生成产物中的哪一类
2. 优先遵循当前目录 `README.md` 和已有文档，不引入与既定技术路线冲突的新框架
3. 发现需求文档、API 合同、表结构、页面清单不一致时，先指出冲突并给出最小修正建议
4. 生成或修改正式代码时，同步补充启动说明、环境变量、健康检查和测试
5. 不把原型 HTML 当作正式前端工程继续扩写
6. 不把 AI、MCP、n8n 设计成绕过平台权限和审计的通道
7. 不提交真实密钥、生产账号、客户敏感数据或未脱敏资料

---

## 多 Agent 协作协议(Mavis Team / Claude Code Team 适用)

### 角色

| 角色 | 干啥的 | 不要干啥 |
|---|---|---|
| **owner** | 拆 task、派活、看 CycleReport、决断 PASS/FAIL | 不要亲自写生产代码 |
| **producer** | 按 task 写代码 / 跑构建 / 写 deliverable | 不要自己决定 PASS;每写完一个文件就 append 一行到 deliverable.md,给引擎心跳 |
| **verifier** | 独立审查产物;跑 `mvn compile` / `tsc --noEmit` / `DESCRIBE table`;给 PASS/FAIL | 不要顺手改 producer 代码;不要靠"看着对"判 PASS |
| **tester** | 跑单测 / 集成测 / 端到端 | 不要跳过手动冒烟 |

### 硬上限

- **producer 阶段:15min(系统硬限,撞了必被 kill)**
- verifier 阶段:30min(可申请延长一次)
- **生产者别在 thinking 里反复权衡方案** —— 先写代码 → 跑构建 → 错再改
- 全链路 E2E 单 task 装不下,**拆成"接口代码 + 编译"和"单测 + 真实启动"两个 sub-task**

### 交棒 / 上报

- producer 完成后:在 deliverable.md 写**已完成文件清单 + 编译/测试日志关键行 + 已知遗留**
- verifier 完成后:在 verdict 里写**PASS / FAIL + 一句话证据**(不能只说"看起来对")
- 失败 owner 重派时:steer 一次,不要反复 abort + spawn(浪费 session)

### 并行约束

- 同一 controller / 同一 page 文件,**同一时间只允许 1 个 agent 改**
- 改 `BaseEntity` / `pom.xml` / `tsconfig.json` / Flyway 主干迁移 —— **必须先在 plan 里 mark 为单 task**
- producer A 完成后,producer B 才能开始读 A 的产物 —— 不要并发读未提交的中间状态
- 文件所有权默认按目录划分:`services/platform-api/exam/` 归题库模块 owner;`apps/admin-web/src/pages/exam/` 归前端对应模块 owner;跨模块改动必须 owner 显式授权

---

## Git / 分支 / 提交规范

### 分支命名

- 普通 task:`feat/<task-id>-<slug>`,例如 `feat/tk-001-question-bank-tree`
- 修 bug:`fix/<task-id>-<slug>`
- 文档:`docs/<slug>`

### 提交格式

```
<type>(<scope>): <subject>

<body: 关联 task id + verifier verdict>
```

- **type** 取值:`feat` / `fix` / `refactor` / `test` / `docs` / `chore` / `perf`
- **scope** 取值:`backend` / `frontend` / `db` / `flyway` / `agt`(agent 配置) / 具体模块如 `exam` / `chazheng`

### 合并前必须

1. `mvn compile -DskipTests` 0 错(主代码)
2. `npm run tsc` 0 错
3. verifier task 写过且 verdict = PASS
4. 不带 BOM(检查脚本见下方"项目级踩坑" #3)
5. 不带 `setDeleted(1); mapper.updateById(...)` 这种假删写法
6. 不修改已应用的 Flyway 迁移;新增迁移必须用新 version 号

---

## 项目级踩坑(摘自 Mavis memory)

新进项目的 Agent **必须**先读这 6 条,能少走 3 天弯路:

1. **MyBatis-Plus 默认 snake_case** — Flyway 迁移的列名必须 snake_case,跟 Java 命名策略对齐(2026-06-04)
2. **`setDeleted(1) + updateById` 永远是假删** — 软删统一用 `mapper.deleteById(...)`,全项目禁止 `setDeleted(1)`(2026-06-09)
3. **PowerShell Set-Content 默认带 UTF-8 BOM** — Java 编译会报非法字符;改源码用 Edit/Write 工具,不要走 shell 管道(2026-06-09)
4. **Flyway 批量重命名迁移要按 description 还原真实 version** — 不要靠 description 猜 version,先 `SELECT description, version FROM flyway_schema_history` 反查(2026-06-05)
5. **前端 tsx 引用同目录 css 是约定** — 移植 Vue → React 时别漏 import 对应 css(2026-06-05)
6. **producer 阶段 25min 硬上限装不下全链路 E2E** — 拆 sub-task,或 owner 自己跑 E2E,或明确不让 verify-as-task 进 plan(2026-06-09)
