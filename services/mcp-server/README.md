# mcp-server

MCP 工具服务，供 AI 在授权范围内调用平台能力。本目录是仓库内统一的 MCP Server 工程，当前已纳入认定管理只读查询试点服务，面向 WeKnora Agent 提供受控 MCP Tool。

## 职责

1. 暴露查询、生成草稿、创建待办等低风险 MCP Tool。
2. 为每个工具定义名称、用途、参数 schema、权限要求和审计要求。
3. 调用 `platform-api` 的受控 API 完成业务动作。

## 边界

1. 不直连业务数据库。
2. 不绕过平台权限、租户隔离和审计。
3. 不执行高风险最终动作，关键写操作需要人工确认。

## 当前试点

认定管理试点服务用于只读查询认定管理模块数据：

- 传输方式：Streamable HTTP 使用 `/mcp`；兼容旧版 SSE，使用 `/sse` 和 `/messages`
- 入站认证：`Authorization: Bearer <MCP_BEARER_TOKEN>`
- 上游调用：只调用 `platform-api`，不直接连接 MySQL
- 平台鉴权：使用服务账号自动登录拿 token；请求带 `Authorization: Bearer <accessToken>` 和 `X-Tenant-Id`
- Token 过期（HTTP 401）时 MCP 自动重新登录并重试一次，无需手工更新 `PLATFORM_API_TOKEN`

已提供工具：

- `certification.searchTasks`
- `certification.getTaskOverview`
- `certification.getCollectionProgress`
- `certification.getPracticalScoreProgress`
- `file.getArtifactMetadata`

artifact/合同工具通过 MCP `_meta.guoyu/bot-actor-context` 接收 WeKnora 服务端注入的企微发起人和当前会话，不接受模型参数中的 `receiver`、`chatId` 或 `userId` 作为发送目标。MCP Server 使用 `BOT_ACTOR_CONTEXT_SECRET` 对上下文签名后透传给 platform-api；文件二进制不经过 MCP 返回。

## 本地启动

参见 `.env.example` 配置环境变量。

必需项：

| 变量 | 说明 |
| --- | --- |
| `MCP_BEARER_TOKEN` | WeKnora / 调用方访问本 MCP 的入站 Bearer |
| `PLATFORM_API_BASE_URL` | 平台 API 根路径，例如 `http://localhost:8080/api` |
| `PLATFORM_TENANT_CODE` | 租户 **编码**（如 `demo`），不是展示名「千铝工投」 |
| `PLATFORM_API_USERNAME` | 服务账号用户名，需具备 `certification.dashboard.view` |
| `PLATFORM_API_PASSWORD` | 服务账号密码 |
| `BOT_ACTOR_CONTEXT_SECRET` | 与 platform-api 相同的至少 32 位 HMAC 密钥；artifact/合同工具必填 |

可选项：

| 变量 | 说明 |
| --- | --- |
| `PLATFORM_API_TOKEN` | 启动时缓存的平台 token；可空，首次请求会自动登录 |
| `PLATFORM_TENANT_ID` | 启动时缓存的租户 ID；登录成功后以响应为准覆盖 |
| `PLATFORM_REQUEST_TIMEOUT_MS` | 上游超时，默认 10000 |

缺少必需账号配置时，服务会主动快速失败，避免以不完整配置启动。

生产若用 PM2：`ecosystem.config.cjs` 的 `env` 与 `.env` 需同步写入上述账号变量；`config` 会在启动时从 cwd `.env` 补齐空缺项。`PLATFORM_API_TOKEN` 可留空或填任意失效值，首次业务请求会自动登录。

```powershell
cd services/mcp-server
npm install
npm run build
npm start
```

健康检查：

```text
GET /health
```

WeKnora 配置：

- 服务名称：`certification-data-assistant`
- 描述：用于只读查询认定管理业务数据的助手
- HTTP Streamable 地址：`http://<host>:3001/mcp`
- SSE URL：`http://<host>:3001/sse`
- 认证方式：`Authorization: Bearer <MCP_BEARER_TOKEN>`

## 知识库工具合同

企业 RAG 知识库第一版由 `platform-api` 提供受控 REST API。MCP Server 后续只做工具适配，不直连 MySQL、对象存储或向量库。

| 工具名 | 权限码 | 平台 API | 说明 |
| --- | --- | --- | --- |
| `knowledge.search` | `knowledge:search` | `POST /api/knowledge/search` | 按用户、租户、角色过滤后检索知识库片段 |
| `knowledge.answer_draft.create` | `knowledge:answer` | `POST /api/knowledge/answer-drafts` | 基于检索片段创建问答草稿 |
| `file.metadata.get` | 登录 | `GET /api/files/{id}` | 查询文件元数据，不返回越权文件内容 |

工具调用必须透传平台 JWT 或受控服务身份、`X-Request-Id`、用户 ID 和租户上下文，由 `platform-api` 做最终权限裁决和审计。

敏感资料规则：

1. 平台内只保留一套 RAG 能力，MCP 不为不同业务另建独立 RAG。
2. `knowledge.search` 和 `knowledge.answer_draft.create` 只能返回 `platform-api` 权限过滤后的片段。
3. 合同金额、报价、薪酬等敏感片段必须来自 `accessScope=restricted` 文档，只有签署人、负责人或高权限角色在文档 ACL 中时才能被召回。
