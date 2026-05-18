# AGENTS.md

本文件给在本项目中工作的 AI Agent / Codex 使用。进入仓库后先阅读本文件，再按需加载对应目录下的文档。

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
- **知识库**：知识库 CRUD、文档管理、权限控制（`knowledge/`）
- **数据库**：5 个 Flyway 迁移已就绪（含初始化表、Demo 数据、知识库表）
- **前端管理端**：知识库页面（`apps/admin-web/src/pages/knowledge/`），登录页，AppShell 布局

### 空壳/待实现
- `course/`、`exam/`、`question/`、`training/`：后端包目录存在但无业务代码
- 前端对应页面（dashboard/、system/、course/、question/、exam/、training/、workflow/、ai/）均未创建
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

### 后端规则

包结构（`services/platform-api/src/main/java/com/guoyu/platform/`）：

```
common/  config/  security/  tenant/  system/
file/  workflow/  course/  question/  exam/  training/
ai/  integration/
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

1. Controller 不直接写 SQL，Entity 不直接暴露给前端
2. 所有列表接口使用统一分页结构，所有写操作记录操作日志
3. 涉及企业/项目/用户的数据必须带 `tenant_id` 和数据权限过滤
4. 后端是权限最终裁决点，前端只做展示控制
5. 文件上传只返回文件 ID、文件名、大小、类型和访问策略，不存文件内容到数据库
6. AI / MCP 调用必须走受控 API 或 Application 层，不能绕过权限直接访问 Mapper
7. API 统一返回结构：`{ code, message, data }`，分页数据包裹在 `data` 中，含 `total`、`page`、`size`

### 数据库规则

1. MySQL 8.4，主键使用雪花 ID，类型统一 `BIGINT`
2. 多租户字段统一 `tenant_id`，逻辑删除通过 `deleted` 字段实现（MyBatis-Plus 自动处理，不手写拼接条件）
3. 业务主表必须包含：`id`、`tenant_id`、`created_at`、`updated_at`、`created_by`、`updated_by`、`deleted`
4. 使用 Flyway 管理迁移，脚本放在 `services/platform-api/src/main/resources/db/migration/`
5. 迁移命名：`V{YYYYMMDD}_{序号}__{描述}.sql`
6. 空库必须能执行全部 Flyway 迁移成功
7. 业务主状态以 MySQL 为准，Redis 只用于缓存/会话/验证码/限流

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

执行规则：

1. 页面级数据请求使用 TanStack Query
2. 用户信息、菜单、主题、折叠状态使用 Zustand
3. 表格优先 ProTable，表单优先 ProForm，图表统一 ECharts
4. 菜单从后端动态加载，不在前端写死
5. HTML 原型仅作参考，不在原型文件上堆正式功能
6. 接口类型放入 `types/`（后续可用 OpenAPI 生成）

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

| 工具 | 路径 |
|---|---|
| JDK 21 | `D:\Program Files\Java\oracleJdk-21` |
| Maven | `C:\Program Files\Java\apache-maven-3.9.11\bin\mvn.cmd` |
| Node.js | `D:\Program Files\nodejs` |

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
3. 涉及一期功能时以 `docs/04_requirements/phase1/` 为准
4. 发现需求文档、API 合同、表结构、页面清单不一致时，先指出冲突并给出最小修正建议
5. 生成或修改正式代码时，同步补充启动说明、环境变量、健康检查和测试
6. 不把原型 HTML 当作正式前端工程继续扩写
7. 不把 AI、MCP、n8n 设计成绕过平台权限和审计的通道
8. 不提交真实密钥、生产账号、客户敏感数据或未脱敏资料
