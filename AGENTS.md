# AGENTS.md

> **本文件被以下 Agent 工具读取**（优先级从上到下）：
> - **MiniMax Code / Mavis** — 团队阶段，owner + producer + verifier 协作
> - **Claude Code** — 单 agent 阶段，本文件等同 CLAUDE.md
> - **Cursor** — 单 agent 阶段，通过 `Cursor → Settings → Rules` 自动同步
> - **Qoder** — 通过 `.qoder/agents/builtin/*.md` 引用本文件核心段
> - **OpenCode / Codex / Gemini CLI** — 直接读取根目录 `AGENTS.md`
>
> **子目录规则**：`apps/admin-web/AGENTS.md`（前端）、`services/platform-api/AGENTS.md`（后端）承载局部规则，进入对应目录工作前必读。Codex / Claude Code 会自动加载子目录文件；Cursor / Qoder 不会，做对应目录任务时需手动打开。
>
> **若你是 producer**：只读本文件 + 任务涉及目录的子目录 AGENTS.md，不要碰协作协议。
> **若你是 verifier**：必读 `docs/agent/多Agent协作协议.md` + 本文件「测试与验收」。
> **若你是 owner / plan 编排**：必读全部。

本文件给在本项目中工作的 AI Agent / Codex 使用。进入仓库后先阅读本文件，再按需加载对应目录下的文档。

## 阅读顺序

1. `README.md` — 项目概述、目录结构、技术栈、一键启动
2. `docs/README.md` — 文档分区说明
3. `docs/05_decisions/开发框架选型建议.md` 与 `docs/05_decisions/云平台技术路线选型建议.md` — 技术选型或架构变更时必须遵守
4. 项目现状（模块清单 / 迁移基线 / 服务状态）→ `docs/agent/项目现状.md`（脚本生成，勿手改；刷新：`node scripts/gen-agents-context.mjs`）

## 目录放置约束

1. 新需求、流程图、业务表格 → `docs/04_requirements/`
2. 技术路线、架构决策、选型记录 → `docs/05_decisions/`
3. HTML 原型 → `prototypes/`，不可混入正式前端源码
4. 正式 React 等前端代码 → `apps/`
5. Java、AI Gateway、MCP、文件、工作流等后端代码 → `services/`
6. 生成产物 → `outputs/`，不可当作需求源头
7. 不提交真实密钥、本地缓存、`.DS_Store`、临时文件和大型二进制素材

## 技术约束

技术栈详见 `README.md` 和 `docs/05_decisions/`。前端细则见 `apps/admin-web/AGENTS.md`，后端细则（分层约束 / 防御性编程 / 异步陷阱完整版 / LLM 接入规范）见 `services/platform-api/AGENTS.md`。

### 总体架构口径

1. Java / Spring Boot 做核心业务主干，一期采用可控服务化，不做全量微服务
2. 核心业务集中在 `services/platform-api/`，不要拆出细业务服务
3. AI、MCP、文件、工作流保持独立工程边界，但一期可先在 `platform-api` 内用包隔离
4. Flowable 管核心业务流程状态，n8n 只做内部自动化/通知/定时；BPMN 文件放 `services/platform-api/src/main/resources/workflows/bpmn/`，n8n 导出放 `workflows/n8n/`

### 全栈硬规则

1. 所有源码、配置、Markdown 文件使用 UTF-8 without BOM；改源码用 Edit/Write 工具，不走 PowerShell 管道（Set-Content 默认带 BOM）
2. API 统一返回 `{ code, message, data }`；列表接口统一 PageQuery / PageResponse（`{ total, page, size, records }`）
3. 涉及企业/项目/用户的数据必须带 `tenant_id` 和数据权限过滤；多租户过滤由 MyBatis-Plus 插件自动处理，不手拼条件
4. 逻辑删除统一 `deleteById` / `deleteBatchIds`，**禁止 `setDeleted(1) + updateById`**
5. 后端是权限最终裁决点，前端只做展示控制；AI / MCP 调用必须走受控 API，不绕过权限直达 Mapper
6. 所有写操作记录操作日志；filter/interceptor 不静默吞异常
7. 不提交真实密钥、生产账号、客户敏感数据；本机真实连接信息写 `AGENTS.local.md`（已 gitignore）
8. 文件上传只返回文件 ID、文件名、大小、类型和访问策略，不存文件内容到数据库

### 数据库铁律

1. MySQL 8.4，主键雪花 ID（`BIGINT`）；业务主表必含 BaseEntity 六列（`id / tenant_id / deleted / created_by / updated_by / created_at / updated_at`），与实体 getter 一一对应
2. Flyway 迁移放 `services/platform-api/src/main/resources/db/migration/`，命名 `V{YYYYMMDD}_{序号}__{描述}.sql`；**不改已应用迁移，新增必须新版本号——测试库与生产库已分叉，取号先同时查两库 `MAX(version)` 取最大并留间隔**；种子迁移必须幂等；空库必须能跑通全部迁移
3. 菜单/权限/角色授权种子的 `tenant_id` 写 **10159**（千铝工投运行租户），不照抄历史文件里的 `tenant_id=1`；权限三步链：菜单 → 权限码 → `sys_role_permission` 授权，缺最后一步接口 403
4. 业务主状态以 MySQL 为准，Redis 只用于缓存/会话/验证码/限流；`@Cacheable` 返回值用 `new ArrayList<>(list)`，禁用 immutable collection（Redis 反序列化会炸）
5. 新迁移必须 `mvn compile` 后才会被 Flyway 看到（读 `target/classes`）；Druid wall 拦 `PREPARE` 与 `AS new` 别名——迁移走独立直连（`spring.flyway.url/user/password`，属性名是 `user` 不是 `username`），upsert 用旧式 `ON DUPLICATE KEY UPDATE c = VALUES(c)`

### 异步与多租户红线

1. `@Async` / 线程池 / `CompletableFuture` 等 Worker 线程中 `SecurityContext.currentUser()` 为 null——tenantId/userId 必须在 HTTP 线程捕获后经参数传递，整条调用链（含下游 Service/Mapper/ConfigService）禁止再调 SecurityContext；Worker 线程自行 `TenantContextHolder.setTenantId` + `finally clear()`
2. 线程池拒绝策略用 `AbortPolicy`（队列满返回 503），**禁用 CallerRunsPolicy**（退化为 HTTP 线程同步执行，前端"卡死"）
3. 大数据量导入/批处理接口拆「快速返回 + 异步执行 + 进度轮询」三步，禁止单接口同步完成
4. 异步批处理 pause 断不了已提交的长调用，块粒度要小，resume 时 `processing` → `pending`

### AI / LLM 硬规则

- AI 生成内容必须标记来源、模型、生成时间和操作人；写操作默认草稿/待办，发布、审批、删除必须人工确认
- LLM 调用收敛在 `ai/` 包，密钥/模型走配置，调用前逐字段核对目标平台文档（OpenAI 兼容 ≠ 完全兼容）
- 思考型模型（deepseek/glm thinking）思维链**计入 max_tokens**：开 thinking 必须放大预算或显式关闭
- 完整 11 条接入规范见 `services/platform-api/AGENTS.md`

### 高频雷区速查（完整案例库：`docs/agent/踩坑库.md`）

- MyBatis-Plus 默认 snake_case，迁移列名对齐 Java 命名策略（#1）
- 雪花 ID 超 JS Number 安全整数，接口响应中的雪花 id 以字符串下发（#24）
- 第三方 API 不假设兼容，不支持的参数一律不传（#13）
- 服务端不要自回环 HTTP 拉自己的文件，本机文件直接读盘（#26）
- 第二 DataSource Bean 会让主数据源自动配置退让（#19/#20，PHP 数据源 2026-08-27 已拆除，归档备查）

## 测试与验收

后端：JUnit 5 / Mockito / Spring Boot Test + MockMvc / Testcontainers / Spring Security Test
前端：Vitest / React Testing Library / Playwright（**无 MSW**，勿假定存在）

最低验收：

1. 登录、菜单、权限、数据权限必须有测试
2. 数据库迁移脚本必须能在空库执行成功；新增/修改表 DDL 必须包含完整 BaseEntity 六列，与实体 getter 一一对应
3. 涉及异步线程的功能，整个调用链禁止直接调用 SecurityContext
4. 文件上传必须验证成功和失败路径
5. AI 和 MCP 工具调用必须验证权限失败、参数错误和人工确认
6. 一期主链路可跑通：管理员配置基础数据 → 课程人员建课建题 → 课程发布审批 → 安排考试 → 学员学习答题查看成绩错题

**验证边界诚实报告**：无法运行检查时，明确列出未验证项、原因和风险，禁止声称验证通过。

## Git / 分支 / 提交规范

分支命名：普通 task `feat/<task-id>-<slug>`；修 bug `fix/<task-id>-<slug>`；文档 `docs/<slug>`

提交格式：`<type>(<scope>): <subject>`，body 关联 task id + verifier verdict。type ∈ `feat/fix/refactor/test/docs/chore/perf`；scope ∈ `backend/frontend/db/flyway/agt` 或具体模块。

合并前必须：

1. `mvn compile -DskipTests` 0 错（主代码）
2. `npm run build` 0 错（前端无 `tsc` script，build 内含 `tsc -b`）
3. verifier task 写过且 verdict = PASS
4. 不带 BOM（检查：`head -c 3 <file> | xxd | grep efbbbf`）；不带 `setDeleted(1)+updateById` 假删；不改已应用 Flyway 迁移
5. 构建命令、目录结构、迁移规则变化时，本文件及子目录 AGENTS.md 纳入同一个 PR 更新

## Agent 工作方式

1. 修改前先确认任务属于文档、原型、正式前端、后端服务、脚本、数据、基础设施或生成产物中的哪一类
2. 优先遵循当前目录 `README.md` 和已有文档，不引入与既定技术路线冲突的新框架
3. 发现需求文档、API 合同、表结构、页面清单不一致时，先指出冲突并给出最小修正建议
4. 生成或修改正式代码时，同步补充启动说明、环境变量、健康检查和测试
5. 不把原型 HTML 当作正式前端工程继续扩写
6. 不把 AI、MCP、n8n 设计成绕过平台权限和审计的通道
7. 多 Agent 协作（角色 / 硬上限 / 交棒 / 并行约束）→ `docs/agent/多Agent协作协议.md`

## 子文档索引

| 文档 | 内容 | 何时读 |
|---|---|---|
| `apps/admin-web/AGENTS.md` | 前端规则全量（命令 / 错误处理 / 登录态 / 多租户） | 改前端前 |
| `services/platform-api/AGENTS.md` | 后端细则（分层 / 防御性编程 / 异步完整版 / DB 细则 / LLM 11 条） | 改后端前 |
| `docs/agent/项目现状.md` | 模块清单、迁移基线、apps/services 状态（脚本生成） | 需要现状快照时 |
| `docs/agent/踩坑库.md` | 26 条事故案例（含已失效归档） | 碰相关领域前 |
| `docs/agent/多Agent协作协议.md` | 角色 / 硬上限 / 交棒 / 并行约束 | Mavis / Team 阶段 |
| `docs/agent/本地开发环境.md` | 环境路径 / 变量 / 命令 / Demo 账号 | 搭环境、起服务时 |
| `AGENTS.local.md`（gitignore） | 本机工具路径、真实数据库连接 | 本机调试时 |
