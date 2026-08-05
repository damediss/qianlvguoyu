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


## 阅读顺序

按优先级从高到低，Agent 在处理任务时应按需逐层深入：

1. `README.md` — 项目概述、目录结构、技术栈、一期范围、一键启动
2. `docs/README.md` — 文档分区说明
3. `docs/05_decisions/开发框架选型建议.md` 与 `docs/05_decisions/云平台技术路线选型建议.md` — 技术选型或架构变更时必须遵守

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

### 异步与多租户陷阱(必读,2026-07-13 新增)

#### 1. BaseEntity 字段必须完整映射到 DDL
所有后端 Entity 继承 `BaseEntity`(`com.guoyu.platform.common.domain.BaseEntity`),
自动包含以下字段: `tenant_id`、`deleted`、`created_by`、`updated_by`、`created_at`、`updated_at`。

**新增表或修改实体时,必须执行**:
1. 确认实体 `extends BaseEntity`
2. 在 DDL 中包含上述 6 列(顺序不限,建议按 `tenant_id / deleted / created_by / updated_by / created_at / updated_at`)
3. 已有表的 ALTER 迁移也必须补全缺失的 BaseEntity 列
4. **检查方式**:`SHOW COLUMNS FROM table` 对比实体 getter,每个 getter 都应对应一个表列

**典型踩坑**:实体继承 BaseEntity → MyBatis-Plus 生成的 INSERT/SELECT 包含所有继承字段 → 表中缺少对应列 → `Unknown column 'xxx' in 'field list'`。

#### 2. 异步线程禁止直接调用 SecurityContext
`SecurityContext.currentUser()` 基于 `ThreadLocal`,在以下场景中为 null:
- `@Async` 注解的方法
- `ThreadPoolTaskExecutor` 提交的 Worker 线程
- `CompletableFuture.supplyAsync()` 的回调
- 任何不在原始 HTTP 请求线程中执行的代码

**涉及异步的场景必须遵守**:
1. 在 HTTP 请求线程中提前取出 `tenantId`/`userId`,通过方法参数传递到异步方法
2. 整个异步调用链(**包括所有下游 Service、Mapper、ConfigService**)都不能直接调 `SecurityContext`
3. 写方案时画清"线程边界",标注哪些方法跑在 HTTP 线程、哪些跑在 Worker 线程
4. 遍历被异步方法调用的**完整调用链**,逐一确认每个方法都不依赖 SecurityContext

**典型踩坑**:
- Service A 在 HTTP 线程调用 SecurityContext → OK
- Service A 内部提交任务到线程池 → 线程池 Worker 调用 Service B → Service B 调 SecurityContext → `IllegalStateException: No authenticated platform user`
- 即使 Service 本身不调 SecurityContext,它的**下游依赖**(如 `CertCheckConfigService.findApiUrl()`)调了也一样报错

**正确写法**:
```java
// HTTP 请求线程:捕获 tenantId
Long tenantId = SecurityContext.currentUser().getTenantId();
executor.submit(() -> processAsync(tenantId, userId));

// Worker 线程:通过参数接收,不调 SecurityContext
private void processAsync(Long tenantId, Long userId) {
    // 整个调用链内所有方法都通过参数传递 tenantId
    certCheck(userId, tenantId);
}
```

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
2. 数据库迁移脚本必须能在空库执行成功；所有新增/修改表 DDL 必须包含完整 BaseEntity 字段（tenant_id / deleted / created_by / updated_by / created_at / updated_at），与实体 getter 一一对应
3. 涉及异步线程（线程池 / @Async / CompletableFuture）的功能，整个调用链禁止直接调用 SecurityContext，tenantId 必须通过 HTTP 请求线程捕获后参数传递
4. 文件上传必须验证成功和失败路径
5. AI 和 MCP 工具调用必须验证权限失败、参数错误和人工确认
6. 一期主链路可跑通：管理员配置基础数据 → 课程人员建课建题 → 课程发布审批 → 安排考试 → 学员学习答题查看成绩错题

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

| 变量 | 说明                                                 |
|---|----------------------------------------------------|
| `SERVER_PORT` | 后端端口，默认 8080                                       |
| `MYSQL_URL` | 本地 MySQL jdbc:mysql://43.138.163.7:3308/gjjddb2026 |
| `MYSQL_USERNAME` | 数据库用户 gjjd                                         |
| `MYSQL_PASSWORD` | 数据库密码 w-#Y+=g7jdsJLjD8                             |
| `JWT_SECRET` | 至少 32 位随机字符串                                       |

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

- 租户：`千铝工投`
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

新进项目的 Agent **必须**先读这 18 条,能少走 3 天弯路:

1. **MyBatis-Plus 默认 snake_case** — Flyway 迁移的列名必须 snake_case,跟 Java 命名策略对齐(2026-06-04)
2. **`setDeleted(1) + updateById` 永远是假删** — 软删统一用 `mapper.deleteById(...)`,全项目禁止 `setDeleted(1)`(2026-06-09)
3. **PowerShell Set-Content 默认带 UTF-8 BOM** — Java 编译会报非法字符;改源码用 Edit/Write 工具,不要走 shell 管道(2026-06-09)
4. **Flyway 批量重命名迁移要按 description 还原真实 version** — 不要靠 description 猜 version,先 `SELECT description, version FROM flyway_schema_history` 反查(2026-06-05)
5. **前端 tsx 引用同目录 css 是约定** — 移植 Vue → React 时别漏 import 对应 css(2026-06-05)
6. **producer 阶段 25min 硬上限装不下全链路 E2E** — 拆 sub-task,或 owner 自己跑 E2E,或明确不让 verify-as-task 进 plan(2026-06-09)
7. **新建 Flyway 迁移前先查最大 version** — `SELECT MAX(version) FROM flyway_schema_history`,新迁移版本号 = 最大 version + 0.001;不要靠文件名猜,数据库里可能已有同 version 的迁移(2026-07-14)
8. **权限系统三步链:菜单→权限码→角色授权** — 只建菜单+权限码不够,还必须 INSERT `sys_role_permission` 把权限码授予目标角色(role_id=2000 是平台管理员),否则接口 403(2026-07-14)
9. **Redis 缓存序列化:返回 mutable collection** — `@Cacheable` 方法的返回值必须用 `ArrayList::new` 或 `new ArrayList<>(list)`,禁止用 `.toList()` / `List.of()` / `Map.of()` 等 immutable collection,否则 Redis 反序列化报 `Unexpected token (START_ARRAY), expected VALUE_STRING`(2026-07-14)
10. **前端 axios `responseType: 'blob'` 下载文件** — `apiClient.get(url, { responseType: 'blob' })` 返回 `AxiosResponse`,blob 数据在 `.data` 里;API 函数签名应 `return (await apiClient.get(...)).data` 并声明 `Promise<Blob>`(2026-07-14)
11. **可能处理大数据量的导入接口必须拆分为"快速返回 + 异步执行"两步** —— 如果接口涉及文件上传/批量导入/批量处理等**数据量不确定**的场景,禁止在一个接口里同步完成"接收数据 + 后台处理"。需开发人员确认是否拆为:第一步只接收数据并入库/返回标识(秒回),第二步单独触发后台处理(秒回 batchId),第三步轮询进度。否则数据量大时前端会超时(axios 默认 20s),前端拿不到 batchId 就无法轮询(2026-07-15)
12. **线程池拒绝策略禁止用 CallerRunsPolicy** —— 队列满时 CallerRunsPolicy 会把任务退化为调用者线程(HTTP 线程)同步执行,前端表现为"卡死"。必须用 `AbortPolicy` + 全局异常处理返回 503"服务繁忙,请稍后重试",让客户端感知到限流(2026-07-15)
13. **调用第三方 API(LLM/支付/短信等)前必须核对目标文档,不能假设兼容** —— OpenAI 兼容 ≠ 完全兼容。LongCat 不支持 `response_format` 字段,强发会导致模型返回空 content。调用任何第三方接口前,先确认目标平台文档明确支持该字段/参数,不支持的字段一律不传(2026-07-15)
14. **多个同类型线程池 Bean 必须用具体类型 + @Qualifier 注入** — `@Bean` 方法返回类型必须用 `ThreadPoolTaskExecutor`(不能用 `Executor` 接口),否则 Spring 按返回类型注册导致找不到 Bean。注入端配合 `@Qualifier("beanName")` 区分。Flowable 还需要名为 `applicationTaskExecutor` 的 `AsyncTaskExecutor` Bean,缺少会启动报错(2026-07-20)
15. **TenantContextHolder 不跨线程传播** — 并行/异步场景中,编排线程 `setTenantId()` 对 Worker 线程无效。每条 Worker 线程必须独立 `TenantContextHolder.setTenantId(tenantId)` + `finally clear()`。编排线程仅在加载期 set/clear,不传给子任务(2026-07-20)
16. **Flyway 迁移脚本必须编译后才生效** — Flyway 读取 `target/classes/db/migration/`,不是源码目录。新加 `.sql` 后必须 `mvn compile` 再启动,否则 Flyway 看不到新脚本(2026-07-20)
17. **MyBatis-Plus 多租户拦截器依赖 TenantContextHolder** — Worker 线程中 Mapper 查询自动拼 `tenant_id` 的前提是该线程已 `setTenantId()`。未设置时查询不带租户条件,可能跨租户漏数据(2026-07-20)
18. **异步批处理 pause 不中断已提交的 LLM 长调用** — 暂停只阻止新任务块继续提交,当前块内已启动的 Worker 线程会跑完。设计时缩小块粒度、在 resume 时重置 `processing` → `pending`,并把批次内不变数据(如知识点全表)一次性加载到内存复用,避免每条记录都查库导致连接池被长耗时压垮(2026-07-20)
19. **多数据源:任何 DataSource Bean 都会让主数据源自动配置整体退让** — 注册第二 DataSource Bean 后,`DataSourceAutoConfiguration` 按 `@ConditionalOnMissingBean` 退让,主数据源(SaaS 库)消失,Flyway 会对第二数据源(如 PHP 库)执行全量迁移(2026-08-04 冒烟事故,差点打挂 PHP 生产库,已清理并复盘)。正确做法:第二数据源用"持有器模式"(`PhpDataSourceHolder` 内部自建 DruidDataSource、不以 DataSource 类型暴露),主数据源自动配置完全不受影响(2026-08-04)
20. **注册第二 JdbcTemplate 后必须显式 `@Primary` 主模板** — `JdbcTemplateAutoConfiguration` 同样按 MissingBean 退让,未标注 `@Qualifier` 的 JdbcTemplate 注入点会全部拿到第二数据源模板(主库 SQL 打到 PHP 库报 bad SQL grammar)。多数据源项目:`@Bean @Primary JdbcTemplate` 钉住默认注入语义 + 第二模板必须 `@Qualifier` 引用(2026-08-04)
21. **主库 Druid wall 过滤器不支持 MySQL 8.0.19+ `AS new` 行别名** — `INSERT ... VALUES (...) AS new ON DUPLICATE KEY UPDATE c = new.c` 会被 wall filter 按 SQL 注入拦截(sql injection violation)。upsert 统一用旧式 `ON DUPLICATE KEY UPDATE c = VALUES(c)`(8.4 仍支持,仅弃用警告)(2026-08-04)
22. **镜像表唯一约束必须与源库一致** — PHP `platform_user_token.token` 无唯一约束(现网 85 组重复),SaaS 镜像表加 UNIQUE 后,`ON DUPLICATE KEY UPDATE` 把同 token 多行折叠,镜像行数永远少于源库且每周期反复覆盖(thrashing,checkpoint 恒见 updated≠0 不收敛)。镜像表索引按源库实际约束建;鉴权查询按 id 升序取首行对齐 PHP findFirst 语义(2026-08-04)
23. **mysql-connector-j 默认把 TINYINT(1) 映射为 Boolean(tinyInt1isBit=true)** — 会丢失 level=2/3 这类真实数值(`value instanceof Number` 为 false → 解析得 null),PHP 老库大量 tinyint(1) 存枚举/层级值。凡接 PHP 库的 JDBC URL 必须加 `tinyInt1isBit=false`;解析层对 Boolean 做 0/1 兜底。2026-08-04 权重解析因此全空、计划生成权重兜底失效(2026-08-04)
24. **雪花 ID 超 JS Number 安全整数(2^53)** — App 端接口响应中的雪花 id(planId/dayId/mockPaperId)必须以字符串下发,前端 JSON.parse 会把 2e18 级整数四舍五入成别的值(2084569567062556674 → ...6700),回传即"记录不存在"。参照竞赛模块 `@JsonSerialize(using=ToStringSerializer.class)` 或视图层直接 String.valueOf(2026-08-04)
