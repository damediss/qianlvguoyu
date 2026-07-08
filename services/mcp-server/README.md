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
- 平台鉴权：向平台发送 `Authorization: Bearer ${PLATFORM_API_TOKEN}` 和 `X-Tenant-Id: ${PLATFORM_TENANT_ID}`

已提供工具：

- `certification.searchTasks`
- `certification.getTaskOverview`
- `certification.getCollectionProgress`
- `certification.getPracticalScoreProgress`

## 本地启动

参见 `.env.example` 配置环境变量。缺少必需 Token 时，服务会主动快速失败，避免以不完整配置启动。

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
