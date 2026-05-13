# 千铝国昱云平台项目

本仓库采用“资料与代码同根目录、分区管理”的方式组织。根目录只保留项目说明、代码工作区配置和一级目录，业务资料、原型、代码、脚本、产物分别进入固定目录。

## 目录结构

| 目录 | 用途 |
| --- | --- |
| `docs/` | 需求、方案、正式交付、项目台账、技术决策 |
| `prototypes/` | HTML 原型、系统截图、流程演示页面 |
| `apps/` | 正式前端应用，如管理后台、学员端、桌面端助手 |
| `services/` | 正式后端服务，如 Java 主平台、AI Gateway、MCP Server |
| `packages/` | 共享类型、UI 组件、内部 SDK |
| `ai/` | Prompt、Skill、Agent 配置、知识库目录规范 |
| `workflows/` | n8n、BPMN、流程自动化规则 |
| `infra/` | Docker、Nginx、数据库、环境变量模板 |
| `scripts/` | 文档生成、数据处理、检查和转换脚本 |
| `data/` | 导入模板、脱敏样例、业务字典 |
| `assets/` | 公共图片、图标、演示素材和素材索引 |
| `outputs/` | 生成产物、渲染检查、PDF、截图 |
| `archive/` | 历史资料和暂未整理文件 |

## 放置规则

1. 新需求、流程图、业务表格放入 `docs/04_requirements/`。
2. 技术路线、架构决策、选型记录放入 `docs/05_decisions/`。
3. HTML 原型放入 `prototypes/`，正式 React 等前端代码放入 `apps/`。
4. Java、AI Gateway、MCP 等后端代码放入 `services/`。
5. 生成物放入 `outputs/`，不要作为需求源头。
6. 临时文件、`.DS_Store`、本地缓存和真实密钥不进入仓库。
7. 大型视频、模型、NAS 素材不直接放入 Git，建议在 `assets/` 或 `docs/` 中保存索引说明。

## 当前重点

后续主开发方向按既定技术路线推进：Java 后端、React 管理端、AI Gateway、MCP、n8n / 流程引擎。当前 HTML 原型继续作为需求参考，不作为正式前端工程继续堆功能。

## 一键启动开发环境

macOS 下可双击根目录 `start-dev.command` 启动一期开发环境。

脚本会先检查本机端口：

1. `3306` 已有服务时，跳过本项目 MySQL 容器。
2. `6379` 已有服务时，跳过本项目 Redis 容器。
3. 端口未占用时，通过 `infra/docker/docker-compose.platform-api-dev.yml` 启动对应容器。
4. 再启动 `services/platform-api` 和 `apps/admin-web`。
5. 如果 `8080` 被其他服务占用，后端会自动改用 `18080` 起；如果 `5173` 被其他服务占用，前端会自动改用 `15173` 起，并同步调整前端代理。

启动后访问：

```text
http://localhost:5173
```

如果默认端口被占用，以脚本窗口实际输出的地址为准。

后端健康检查：

```text
http://localhost:8080/api/actuator/health
```

日志输出在：

```text
outputs/logs/platform-api.log
outputs/logs/admin-web.log
```
