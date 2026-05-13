# AGENTS.md

本文件给在本项目中工作的 AI Agent / Codex 使用。进入仓库后先阅读本文件，再阅读对应目录下的 `README.md` 和需求文档。

## 项目定位

本项目是“千铝国昱云平台”资料、原型、代码和交付物的统一工作区。目标是建设覆盖培训、课程、题库、考试、认定、竞赛、补贴、项目管理、文件归档、AI Agent、MCP 工具和工作流的综合业务平台。

当前阶段以“平台底座 + 培训学习考试闭环”为一期 MVP，不追求一次性完成全业务大平台。

## 首要阅读顺序

1. 根目录 `README.md`：了解目录放置规则。
2. `docs/README.md`：了解文档分区。
3. `docs/04_requirements/phase1/README.md`：了解一期可编码规格。
4. `docs/04_requirements/phase1/01_一期MVP功能清单.md`：确认一期范围和不做范围。
5. `docs/04_requirements/phase1/02_基础表结构设计.md`：涉及数据库时必须阅读。
6. `docs/04_requirements/phase1/03_第一批API合同.md`：涉及接口时必须阅读。
7. `docs/04_requirements/phase1/04_前端页面清单.md`：涉及前端页面时必须阅读。
8. `docs/04_requirements/phase1/05_测试验收清单.md`：提交实现前对照验收。
9. `docs/05_decisions/开发框架选型建议.md` 与 `docs/05_decisions/云平台技术路线选型建议.md`：涉及技术选型、架构边界或新增工程时必须遵守。

## 目录规则

| 目录 | 用途 |
| --- | --- |
| `docs/` | 需求、方案、交付文档、台账、技术决策 |
| `prototypes/` | HTML 原型、系统截图、流程演示页面 |
| `apps/` | 正式前端应用 |
| `services/` | 正式后端服务 |
| `packages/` | 共享类型、UI 组件、内部 SDK |
| `ai/` | Prompt、Skill、Agent 配置、知识库目录规范 |
| `workflows/` | BPMN、n8n、流程自动化规则 |
| `infra/` | Docker、Nginx、数据库、环境变量模板 |
| `scripts/` | 文档生成、数据处理、检查和转换脚本 |
| `data/` | 导入模板、脱敏样例、业务字典 |
| `assets/` | 公共图片、图标、演示素材和素材索引 |
| `outputs/` | 生成产物、渲染检查、PDF、截图 |
| `archive/` | 历史资料和暂未整理文件 |

放置约束：

1. 新需求、流程图、业务表格优先放入 `docs/04_requirements/`。
2. 技术路线、架构决策、选型记录放入 `docs/05_decisions/`。
3. HTML 原型放入 `prototypes/`，不要迁入正式前端源码。
4. 正式 React 等前端代码放入 `apps/`。
5. Java、AI Gateway、MCP、文件、工作流等后端代码放入 `services/`。
6. 生成物放入 `outputs/`，不要把生成物当作需求源头。
7. 不提交真实密钥、本地缓存、`.DS_Store`、临时文件和大型二进制素材。

## 一期默认范围

一期目标是跑通“平台底座 + 培训学习考试闭环”：

1. 平台管理员能创建租户、组织、用户、角色和菜单权限。
2. 课程人员能维护课程、章节、题库和试卷。
3. 培训负责人能发布考试安排。
4. 学员能登录、学习章节、参加考试、查看成绩和错题。
5. 课程发布支持提交、通过、驳回和记录留痕。
6. AI 样板能生成课程大纲草稿，但不能自动发布。

一期不默认做：

1. 竞赛全流程、认定全流程、补贴全流程。
2. 复杂驾驶舱、领导大屏、经营分析。
3. 短信登录、微信登录、企微登录、SSO。
4. 多租户计费、套餐、客户自助开通。
5. 自动组卷、随机抽题策略、题目难度算法。
6. 完整 Agent 编排和自动执行。
7. 客户侧 n8n 配置能力。
8. Kubernetes、全量微服务、细业务服务拆分。

## 技术路线

默认技术栈：

```text
Java 21
Spring Boot 3.5.x
Maven
MyBatis-Plus
Spring Security + Sa-Token
Flyway
MapStruct
MySQL 8.4
Redis 协议缓存
Flowable 7
React + Vite + TypeScript + Ant Design Pro
Ant Design + ProComponents
TanStack Query
Zustand
ECharts
TypeScript MCP SDK
Docker Engine / Docker Compose
```

总体架构口径：

1. Java / Spring Boot 做核心业务主干。
2. 一期采用可控服务化，不做全量微服务。
3. 核心业务集中在 `services/platform-api/`。
4. AI、MCP、文件、工作流等能力保持清晰边界。
5. Flowable 管核心业务流程状态。
6. n8n 只做内部自动化、通知、定时和低风险胶水层。
7. AI Gateway 是统一 AI 能力入口。
8. MCP 是 AI 调用受控业务工具的协议层。

## 服务边界

| 服务 | 目录 | 职责 |
| --- | --- | --- |
| 核心业务主服务 | `services/platform-api/` | 账号权限、租户组织、课程、题库、考试、学习记录、成绩、错题、统一审计和核心业务 API |
| AI Gateway | `services/ai-gateway/` | 模型调用、Prompt 编排、课程大纲草稿生成、材料检查、AI 任务记录 |
| MCP Server | `services/mcp-server/` | 暴露受控 MCP Tool，只调用平台 API，不直连数据库 |
| 文件服务 | `services/file-service/` | 文件元数据、上传、下载、预签名 URL、删除、OSS/COS 适配 |
| 工作流服务 | `services/workflow-service/` | 流程定义、审批任务、流程记录；一期可先集成在 `platform-api`，但按独立边界设计 |

不要一开始拆出 `course-service`、`exam-service`、`tenant-service`、`user-service` 等细业务服务。账号、租户、权限、审计、核心业务规则必须统一。

## 后端规则

后端主工程默认放在 `services/platform-api/`。

推荐包结构：

```text
services/platform-api/
  src/main/java/
    com/guoyu/platform/
      PlatformApplication.java
      common/
      config/
      security/
      tenant/
      system/
      file/
      workflow/
      course/
      question/
      exam/
      training/
      ai/
      integration/
```

分层规则：

| 层 | 命名 | 说明 |
| --- | --- | --- |
| Controller | `*Controller` | REST API，只做参数接收、权限入口和结果返回 |
| Application | `*ApplicationService` | 用例编排、事务边界、跨领域协调 |
| Domain | `*DomainService` / 领域对象 | 核心业务规则 |
| Repository | `*Repository` / `*Mapper` | 数据访问接口和 MyBatis-Plus 数据访问 |
| Entity | `*Entity` | 数据库表映射 |
| DTO | `*CreateReq`、`*UpdateReq`、`*QueryReq` | 请求对象 |
| VO | `*VO`、`*DetailVO` | 返回对象 |
| Converter | `*Converter` | MapStruct 转换 |

执行规则：

1. Controller 不直接写 SQL。
2. Entity 不直接暴露给前端。
3. 所有列表接口使用统一分页结构。
4. 所有写操作记录操作日志。
5. 涉及企业、项目、用户的数据必须带 `tenant_id` 和数据权限过滤。
6. 后端是权限最终裁决点，前端只做展示控制。
7. 文件上传只返回文件 ID、文件名、大小、类型和访问策略，不把文件内容存数据库。
8. AI / MCP 调用必须走受控 API 或 Application 层能力，不能绕过权限直接访问 Mapper。

## 数据库规则

1. 默认使用 MySQL。
2. 主键使用雪花 ID，数据库类型统一 `BIGINT`。
3. 多租户字段统一使用 `tenant_id`。
4. 业务主表默认包含 `id`、`tenant_id`、`created_at`、`updated_at`、`created_by`、`updated_by`、`deleted`。
5. 使用 Flyway 管理迁移，迁移脚本放在 `services/platform-api/src/main/resources/db/migration/`。
6. 逻辑删除通过统一字段实现，不在业务代码里手动拼接删除条件。
7. 空库必须能执行全部 Flyway 迁移成功。

迁移命名示例：

```text
V20260501_001__init_system_tables.sql
V20260501_002__init_tenant_tables.sql
V20260501_003__init_course_question_exam.sql
```

## 前端规则

管理端工程默认放在 `apps/admin-web/`。

默认技术栈：

```text
React + Vite + TypeScript + Ant Design Pro + ProComponents
React Router + TanStack Query + Zustand + ECharts
```

建议目录：

```text
apps/admin-web/
  src/
    app/
    routes/
    layouts/
    pages/
      dashboard/
      system/
      course/
      question/
      exam/
      training/
      workflow/
      ai/
    components/
    services/
    stores/
    hooks/
    utils/
    types/
```

执行规则：

1. 页面级数据请求使用 TanStack Query。
2. 用户信息、菜单、主题、折叠状态等使用 Zustand。
3. 表格优先使用 ProTable。
4. 表单优先使用 Ant Design Form / ProForm。
5. 图表统一使用 ECharts。
6. 接口类型放入 `types/`，或后续由 OpenAPI 生成。
7. 菜单从后端动态加载，不在前端写死。
8. HTML 原型只作为参考，不在原型文件上继续堆正式功能。

学员端工程默认放在 `apps/student-web/`。一期可先用 H5 实现学习、练习、考试、错题、证书、通知等入口。

## AI、Agent 与 MCP 规则

AI Gateway 放在 `services/ai-gateway/`，MCP Server 放在 `services/mcp-server/`。

通用规则：

1. AI Gateway 和 MCP Server 不直连业务数据库。
2. AI Gateway 和 MCP Server 只调用 `platform-api` 暴露的受控 API。
3. `platform-api` 可以保留 `ai/` 包，用于 AI 业务记录、权限校验和草稿落库入口。
4. 写操作默认生成草稿或待办。
5. 课程发布、审批通过、删除核心数据等关键动作必须人工确认。
6. AI 生成内容必须标记来源、模型、生成时间和操作人。
7. MCP Tool 必须定义名称、用途、参数 schema、权限要求和审计日志。
8. 所有工具调用必须能追踪到用户、租户、工具名和请求 ID。

一期优先 Agent：

1. 课程生成 Agent：生成课程大纲草稿、讲义草稿、题库草稿。
2. 材料检查 Agent：检查材料缺失和格式问题。
3. 项目周报 Agent：生成项目周报、风险和需协调事项。

## 工作流与 n8n 规则

1. Flowable 管核心业务流程状态、审批、驳回、留痕和状态回放。
2. n8n 只做内部自动化、通知、定时、接口同步和 AI 草稿触发。
3. n8n 不作为客户可配置工作流能力。
4. 不向客户开放 n8n 编辑器、credentials 管理或执行后台。
5. 如果未来需要对客户开放 n8n 能力，必须先完成商业授权、OEM / Embed 授权和法律审查。

BPMN 文件放在：

```text
workflows/bpmn/
```

n8n 导出文件放在：

```text
workflows/n8n/
```

## 文件、Redis 与部署规则

文件：

1. 一期不使用 MinIO 作为默认生产方案。
2. 生产环境优先使用阿里云 OSS 或腾讯云 COS。
3. 业务模块只调用平台文件服务或 Cloud OSS Adapter。
4. NAS 只作为历史资料和内部大文件索引来源，不作为平台标准上传文件主存储。

Redis：

1. 代码层使用 Redis 协议、Spring Cache 和 Spring Data Redis。
2. Redis 只用于缓存、会话、验证码、任务状态、限流等基础设施。
3. 业务主状态以 MySQL 为准。
4. 生产优先使用托管 Redis / Tair，不默认自建 Redis 集群。

部署：

1. 测试、预生产和生产服务器使用 Linux Docker Engine / Docker Compose。
2. Docker Desktop 只可能用于个人开发机，不写成生产依赖。
3. 一期不默认引入 Kubernetes。

## 测试与验收

后端默认测试：

1. JUnit 5。
2. Mockito。
3. Spring Boot Test + MockMvc。
4. Testcontainers。
5. Spring Security Test。
6. Flowable Test。

前端默认测试：

1. Vitest。
2. React Testing Library。
3. Playwright。
4. MSW。

最低验收要求：

1. 登录、菜单、权限、数据权限必须有测试。
2. 数据库迁移脚本必须能在空库执行成功。
3. 文件上传必须验证成功和失败路径。
4. AI 和 MCP 工具调用必须验证权限失败、参数错误和人工确认。
5. 一期主链路必须能跑通：管理员配置基础数据、课程人员建课建题、课程发布审批、培训负责人安排考试、学员学习答题并查看成绩错题。

## Agent 工作方式

1. 修改前先确认任务属于文档、原型、正式前端、后端服务、脚本、数据、基础设施或生成产物中的哪一类。
2. 优先遵循当前目录 `README.md` 和已有文档，不引入与既定技术路线冲突的新框架。
3. 涉及一期功能时，以 `docs/04_requirements/phase1/` 为准。
4. 发现需求文档、API 合同、表结构、页面清单不一致时，先指出冲突并给出最小修正建议。
5. 生成或修改正式代码时，同时补充必要的启动说明、环境变量模板、健康检查和测试。
6. 不把原型 HTML 当作正式前端工程继续扩写。
7. 不把 AI、MCP、n8n 设计成绕过平台权限和审计的通道。
8. 不提交真实密钥、生产账号、客户敏感数据或未脱敏资料。

