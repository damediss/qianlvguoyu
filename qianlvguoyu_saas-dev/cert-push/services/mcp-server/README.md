# mcp-server

MCP 工具服务，供 AI 在授权范围内调用平台能力。

## 职责

1. 暴露查询、生成草稿、创建待办等低风险 MCP Tool。
2. 为每个工具定义名称、用途、参数 schema、权限要求和审计要求。
3. 调用 `platform-api` 的受控 API 完成业务动作。

## 边界

1. 不直连业务数据库。
2. 不绕过平台权限、租户隔离和审计。
3. 不执行高风险最终动作，关键写操作需要人工确认。

## 约定

1. 一期优先采用 TypeScript MCP SDK。
2. 健康检查接口预留为 `/health`。
3. 所有工具调用必须能追踪到用户、租户、工具名和请求 ID。

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
