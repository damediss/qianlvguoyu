# WeKnora 合同 MVP P0 基线报告

日期：2026-08-11

## 工作区

- 功能分支：`feat/contract-001-smart-file-mvp`
- 原工作区在 `dev` 分支存在未提交修改，涉及认证、考评员导入、MCP 配置和前端验收产物；本轮未覆盖、未回退、未纳入本轮功能文件。
- 当前仓库包含 `platform-api` 和 TypeScript `mcp-server`，不包含 WeKnora Go 服务源码，因此 WeKnora 端 P2 交付 Worker 以跨仓库契约文档交付。

## 数据库只读检查

- 数据库：`gjjddb2026`
- `flyway_schema_history` 最大版本：`20260811.005`
- 本轮新增迁移版本：`V20260811_007`
- `sys_file` 已包含：租户、存储提供商、对象键、原始文件名、MIME、大小、SHA-256、访问策略、状态及 BaseEntity 审计字段。
- `sys_audit_log` 已包含：租户、平台用户、模块、动作、目标、请求 ID、客户端信息、详情 JSON 及逻辑删除字段。

## 文件与对象存储

- `FileApplicationService` 已统一封装 local/COS、文件元数据和 SHA-256。
- 本轮不在合同代码中直接依赖腾讯云 SDK；受控 artifact 内容接口仍通过 `FileApplicationService` 读取。
- 生产 COS 密钥、服务账号密码和真实 Bot 密钥不写入仓库。

## 运行环境检查

- 本轮未部署、未重启服务，未修改数据库实例。
- LibreOffice/PDF 转换器属于合同 P3-P4 范围，本轮不以其是否安装作为 P0 阻塞条件。

## P0 结论

P0 只读基线完成，可以进入 P1-P2 公共底座编码。后续部署前仍需为 MCP 与 platform-api 配置同一份 `BOT_ACTOR_CONTEXT_SECRET`，并为 Bot 服务账号授予最小化的 `file:artifact:read` 权限。
