# platform-api 后端规则

> 根目录 `AGENTS.md` 的子文件，承载后端局部规则。Codex / Claude Code 进入本目录会自动加载；其他工具请先手动阅读。
> 通用硬规则（统一响应结构、多租户、软删、数据库铁律、Git 规范等）见根目录 `AGENTS.md`，本文件不重复。

## 包结构

以 `com.guoyu.platform` 下**实际目录**为准（各包文件数见 `docs/agent/项目现状.md`，脚本生成）。
历史文档中的 `tenant / workflow / course / question / training` 包**已不存在**，不要按旧名建包。

## 分层约束

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

### API 响应规范
- 统一返回结构 `{ code, message, data }`，分页数据包裹在 `data` 中，含 `total`、`page`、`size`
- 所有列表接口使用同一分页参数（PageQuery），响应统一分页结构（PageResponse）

### 审计日志
- 所有写操作记录操作日志

### 数据访问与隔离
- Controller 不直接写 SQL，Entity 不直接暴露给前端
- 涉及企业/项目/用户的数据必须带 `tenant_id` 和数据权限过滤
- 文件上传只返回文件 ID、文件名、大小、类型和访问策略，不存文件内容到数据库
- 逻辑删除统一用 MyBatis-Plus 的 deleteById / deleteBatchIds，禁止手动 setDeleted=1 + updateById
- 多租户过滤通过 MyBatis-Plus 多租户插件自动处理，不要在业务代码里手动拼接 tenant_id 条件

### 认证与权限
- 后端是权限最终裁决点，前端只做展示控制
- AI / MCP 调用必须走受控 API 或 Application 层，不能绕过权限直接访问 Mapper
- 使用 Sa-Token 时，确认当前 token 类型是否支持 extra 持久化（tik/jwt/redis），不支持的从数据库实时查
- buildPrincipal 中对 roles/permissions 做 List.of() 兜底

### 异常处理
- 全局异常处理器覆盖：认证失败、权限不足、参数校验失败、业务异常
- 不要在 filter/interceptor 中静默吞异常

## 防御性编程

- 所有 MyBatis Mapper 返回值做 null 检查后再使用
- 所有外部 API/网络请求的响应先做空值和类型检查
- 认证、授权、权限校验路径必须打印 warn/error 级别日志

## 异步与多租户陷阱（完整版）

### 1. BaseEntity 字段必须完整映射到 DDL

所有后端 Entity 继承 `BaseEntity`（`com.guoyu.platform.common.domain.BaseEntity`），自动包含：`tenant_id`、`deleted`、`created_by`、`updated_by`、`created_at`、`updated_at`。

**新增表或修改实体时必须执行**：
1. 确认实体 `extends BaseEntity`
2. DDL 中包含上述 6 列（顺序不限，建议 `tenant_id / deleted / created_by / updated_by / created_at / updated_at`）
3. 已有表的 ALTER 迁移也必须补全缺失的 BaseEntity 列
4. 检查方式：`SHOW COLUMNS FROM table` 对比实体 getter，每个 getter 都应对应一个表列

**典型踩坑**：实体继承 BaseEntity → MyBatis-Plus 生成的 INSERT/SELECT 包含所有继承字段 → 表中缺少对应列 → `Unknown column 'xxx' in 'field list'`。

### 2. 异步线程禁止直接调用 SecurityContext

`SecurityContext.currentUser()` 基于 `ThreadLocal`，在以下场景中为 null：
- `@Async` 注解的方法
- `ThreadPoolTaskExecutor` 提交的 Worker 线程
- `CompletableFuture.supplyAsync()` 的回调
- 任何不在原始 HTTP 请求线程中执行的代码

**涉及异步的场景必须遵守**：
1. 在 HTTP 请求线程中提前取出 `tenantId`/`userId`，通过方法参数传递到异步方法
2. 整个异步调用链（**包括所有下游 Service、Mapper、ConfigService**）都不能直接调 `SecurityContext`
3. 写方案时画清"线程边界"，标注哪些方法跑在 HTTP 线程、哪些跑在 Worker 线程
4. 遍历被异步方法调用的**完整调用链**，逐一确认每个方法都不依赖 SecurityContext

**典型踩坑**：
- Service A 在 HTTP 线程调用 SecurityContext → OK
- Service A 内部提交任务到线程池 → Worker 调用 Service B → Service B 调 SecurityContext → `IllegalStateException: No authenticated platform user`
- 即使 Service 本身不调 SecurityContext，它的**下游依赖**调了也一样报错

**正确写法**：
```java
// HTTP 请求线程：捕获 tenantId
Long tenantId = SecurityContext.currentUser().getTenantId();
executor.submit(() -> processAsync(tenantId, userId));

// Worker 线程：通过参数接收，不调 SecurityContext
private void processAsync(Long tenantId, Long userId) {
    // 整个调用链内所有方法都通过参数传递 tenantId
    certCheck(userId, tenantId);
}
```

### 3. Worker 线程的租户上下文

`TenantContextHolder` 不跨线程传播。每条 Worker 线程必须独立 `TenantContextHolder.setTenantId(tenantId)` + `finally clear()`；编排线程仅在加载期 set/clear，不传给子任务。未设置时 MyBatis-Plus 多租户拦截器查询不带租户条件，可能跨租户漏数据（踩坑 #15/#17）。

### 4. 线程池与批处理

- 多个同类型线程池 Bean：`@Bean` 返回类型必须用 `ThreadPoolTaskExecutor`（不能用 `Executor` 接口），注入端配合 `@Qualifier("beanName")`；Flowable 还需要名为 `applicationTaskExecutor` 的 `AsyncTaskExecutor` Bean（踩坑 #14）
- 拒绝策略用 `AbortPolicy`（踩坑 #12）；批处理 pause 不中断已提交的长调用，块粒度要小，resume 时把 `processing` 重置为 `pending`（踩坑 #18）
- 大数据量接口拆「快速返回 + 异步执行 + 进度轮询」（踩坑 #11）

## 数据库规则细则

1. MySQL 8.4，主键雪花 ID，类型统一 `BIGINT`；多租户字段统一 `tenant_id`；逻辑删除通过 `deleted` 字段（插件自动处理）
2. Flyway 脚本放 `src/main/resources/db/migration/`，命名 `V{YYYYMMDD}_{序号}__{描述}.sql`；涉及数字串等配置值时，YAML 中的版本号、数字串字段用引号包裹
3. **新迁移必须 `mvn compile` 后才生效**——Flyway 读 `target/classes/db/migration/`，不是源码目录（踩坑 #16）
4. Druid wall 过滤器约束：业务 SQL 禁 `INSERT ... AS new ON DUPLICATE KEY UPDATE`（被 wall 按注入拦截），upsert 统一旧式 `ON DUPLICATE KEY UPDATE c = VALUES(c)`；迁移脚本走独立直连（`spring.flyway.url/user/password`，**属性名是 `user` 不是 `username`**，写错静默回退 OS 用户名报 Access denied），不要给 wall 开 none-base-statement-allow（踩坑 #21/#25）
5. 历史：PHP 第二数据源（`integration/php`、`integration/sync`）已于 2026-08-27 拆除，相关多数据源防御条款归档至 `docs/agent/踩坑库.md` #19/#20/#22/#23，当前代码不再适用

## AI、Agent 与 MCP 规则

1. AI Gateway 和 MCP Server 不直连业务数据库，只调用 `platform-api` 的受控 API
2. `platform-api` 可保留 `ai/` 包用于 AI 业务记录、权限校验和草稿落库
3. 写操作默认生成草稿或待办，课程发布/审批通过/删除核心数据必须人工确认
4. AI 生成内容必须标记来源、模型、生成时间和操作人
5. MCP Tool 必须定义名称、用途、参数 schema、权限要求和审计日志
6. 所有工具调用必须可追踪到用户、租户、工具名和请求 ID

### LLM 接入规范（新增 LLM 能力前必读）

1. **调用入口唯一**：LLM 调用统一收敛在 `ai/` 包（一期）/ ai-gateway（二期），业务代码只依赖 `*AiService` 领域接口；禁止在业务 Service 里各自创建 HTTP client、直拼 prompt 调模型 API
2. **密钥与模型配置化**：API Key、base URL、模型名走环境变量/配置，禁止硬编码、禁止明文入库、禁止提交仓库；切换模型只改配置，不写代码分支
3. **不假设兼容**：接入新模型/新平台前逐字段核对其官方文档，不支持的参数一律不传（踩坑 #13）
4. **显式超时与降级**：所有调用必须设连接/读取超时；重试限次数并指数退避；LLM 不可用时业务主流程必须有降级路径，不得阻断
5. **输出零信任**：模型返回先做空值、类型、结构（JSON Schema）校验再使用；解析失败按失败处理，不猜、不硬兜底
6. **确定性规则不让位**：考核、认定、计费、权限等核心判定以确定性代码为准，LLM 只做生成与模糊判断辅助；产出落入正式数据前必须经规则校验或人工确认
7. **慢调用不堵 HTTP**：批量/长耗时场景一律拆"快速返回+异步执行+进度轮询"（踩坑 #11）；线程池拒绝策略用 AbortPolicy（#12）；pause 断不了已提交的长调用，块粒度要小（#18）
8. **异步链租户安全**：tenantId/userId 在 HTTP 线程捕获后经参数传递，整条调用链禁止再调 SecurityContext；Worker 线程自行 `TenantContextHolder.set` + `finally clear`
9. **上下文最小化**：拼 prompt 前按当前用户数据权限过滤，不把他租户或无权限数据喂进上下文；身份证、手机号等敏感字段脱敏后再送第三方模型
10. **全程可审计**：调用落库记录用户、租户、模型、prompt 摘要、token 用量、耗时、请求 ID；生成内容标记来源/模型/时间/操作人；写操作默认草稿/待办，发布、审批、删除必须人工确认
11. **测试不依赖真实模型**：LLM client 层必须可 mock，单测/集测用桩响应；权限失败、参数错误、人工确认路径必须有测试

### 思考型模型预算

deepseek/glm 等思考型模型的思维链**计入 max_tokens**：开 thinking 必须放大输出预算，或在该链路显式关 thinking；新增 LLM 调用点禁止把 thinking 硬编码为 null（应透传 config）。

## 工作流与 n8n

1. Flowable 管核心业务流程状态、审批、驳回、留痕
2. n8n 只做内部自动化、通知、定时、接口同步和 AI 草稿触发；不向客户开放 n8n 编辑器或执行后台
3. BPMN 文件放 `src/main/resources/workflows/bpmn/`，n8n 导出放仓库根 `workflows/n8n/`
