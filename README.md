# 千铝国昱云平台项目

本仓库采用"资料与代码同根目录、分区管理"的方式组织。根目录只保留项目说明、代码工作区配置和一级目录，业务资料、原型、代码、脚本、产物分别进入固定目录。

## 目录结构

| 目录 | 用途 |
| --- | --- |
| `docs/` | 需求、方案、正式交付、项目台账、技术决策 |
| `prototypes/` | HTML 原型、系统截图、流程演示页面 |
| `apps/` | 正式前端应用（管理后台、学员端） |
| `services/` | 正式后端服务（Java 主平台、AI Gateway、MCP Server） |
| `packages/` | 共享类型、UI 组件、内部 SDK |
| `ai/` | Prompt、Skill、Agent 配置、知识库目录规范 |
| `workflows/` | BPMN、流程自动化规则（一期不引入 n8n，自动化由平台自实现） |
| `infra/` | Docker、Nginx、数据库、环境变量模板 |
| `scripts/` | 文档生成、数据处理、检查和转换脚本 |
| `data/` | 导入模板、脱敏样例、业务字典 |
| `assets/` | 公共图片、图标、演示素材和素材索引 |
| `outputs/` | 生成产物、渲染检查、PDF、截图 |
| `archive/` | 历史资料和暂未整理文件 |

## 放置规则

1. 新需求、流程图、业务表格 → `docs/04_requirements/`
2. 技术路线、架构决策、选型记录 → `docs/05_decisions/`
3. HTML 原型 → `prototypes/`，正式 React 等前端代码 → `apps/`
4. Java、AI Gateway、MCP 等后端代码 → `services/`
5. 生成产物 → `outputs/`，不要作为需求源头
6. 临时文件、`.DS_Store`、本地缓存和真实密钥不进入仓库
7. 大型视频、模型、NAS 素材不直接放入 Git，建议在 `assets/` 或 `docs/` 中保存索引说明


---

## 技术栈

```text
Java 21
Spring Boot 3.5.x
Maven
MyBatis-Plus
Spring Security + Sa-Token
Flyway
MapStruct
MySQL 8.4
Redis 
Flowable 7
Lombok
Logback
Apache POI

React + Vite + TypeScript + Ant Design Pro
Ant Design + ProComponents
TanStack Query
Zustand
ECharts

TypeScript MCP SDK
Docker Engine / Docker Compose
```

---

## 服务边界

| 服务 | 目录 | 职责 |
| --- | --- | --- |
| 核心业务主服务 | `services/platform-api/` | 账号权限、租户组织、课程、题库、考试、学习记录、成绩、错题、审计和核心业务 API |
| AI Gateway | `services/ai-gateway/` | 模型调用、Prompt 编排、课程大纲草稿生成、材料检查、AI 任务记录 |
| MCP Server | `services/mcp-server/` | 暴露受控 MCP Tool，只调用平台 API，不直连数据库 |
| 文件服务 | `services/file-service/` | 文件元数据、上传、下载、预签名 URL、删除、OSS/COS 适配 |
| 工作流服务 | `services/workflow-service/` | 流程定义、审批任务、流程记录；一期可先集成在 `platform-api` |

当前状态：核心业务主服务已实现平台底座和知识库模块，其余四个独立服务仅含 README，尚未编码。

---

## 部署

- 测试、预生产和生产服务器使用 Linux Docker Engine / Docker Compose
- Docker Desktop 仅用于个人开发机，不作为生产依赖
- 一期不引入 Kubernetes
- 生产环境文件存储优先使用阿里云 OSS 或腾讯云 COS（一期不使用 MinIO）
- 生产 Redis 优先使用托管 Redis / Tair，不自建集群

---

## 一键启动开发环境

macOS 下可双击根目录 `start-dev.command` 启动一期开发环境。
启动时添加 -Dspring.profiles.active=dev 或 -Dspring.profiles.active=prod
，logback-spring.xml 中的 <springProfile> 自动切换。默认无 profile 时行为等同于 dev

脚本会先检查本机端口：

1. `3306` 已有服务时，跳过本项目 MySQL 容器
2. `6379` 已有服务时，跳过本项目 Redis 容器
3. 端口未占用时，通过 `infra/docker/docker-compose.platform-api-dev.yml` 启动 MySQL、Redis 和 RabbitMQ
4. 再启动 `services/platform-api` 和 `apps/admin-web`
5. 竞赛公告和选手首页实时提醒共用 `COMPETITION_MESSAGING_ENABLED=true`；实时广播默认使用 `competition.player.realtime` 交换机，可通过 `COMPETITION_PLAYER_REALTIME_EXCHANGE` 覆盖。反向代理需为 `/api/ws/competition-player` 转发 WebSocket Upgrade。
6. 如果 `8080` 被占用，后端自动改用 `18080`；如果 `5173` 被占用，前端自动改用 `15173`

启动后访问：

```text
http://localhost:5173
```

如果默认端口被占用，以脚本窗口实际输出的地址为准。

后端健康检查：

```text
http://localhost:8080/api/actuator/health
```

RabbitMQ 管理页面：

```text
http://localhost:15672
```

日志输出：

```text
outputs/logs/platform-api.log
outputs/logs/admin-web.log
```

### Demo 账号

- 租户：`demo`
- 用户：`admin`
- 密码：`Admin@123456`
