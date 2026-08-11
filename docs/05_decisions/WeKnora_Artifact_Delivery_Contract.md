# WeKnora Generated Artifact 与文件交付契约

版本：`guoyu.generated-artifacts/v1`

## 责任边界

- platform-api 只负责业务文件、元数据、权限、租户校验和受控内容读取。
- MCP Tool 只返回结构化文件引用，不读取或向模型返回文件二进制。MCP 服务进程另提供仅供 WeKnora Worker 调用的内部 HTTP 二进制桥接，复用其平台服务账号与签名密钥；该响应不进入 MCP Tool Result 或模型上下文。
- WeKnora 负责把引用落成交付任务、按当前 ChannelSession 发送，并负责重试和恢复。

## 结构化结果

```json
{
  "schema": "guoyu.generated-artifacts/v1",
  "businessType": "contract",
  "businessId": "2084569567062556674",
  "taskId": "2084569567062556675",
  "artifacts": [
    {
      "provider": "platform-file",
      "documentId": "2084569567062556676",
      "fileId": "2084569567062556677",
      "filename": "软件技术服务框架协议-草稿.docx",
      "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "size": 328476,
      "sha256": "...",
      "version": 1,
      "draft": true
    }
  ]
}
```

所有雪花 ID 使用字符串。MCP/Agent 参数不得出现 `receiver`、`chatId` 或 `userId` 作为文件发送目标。

## BotActorContext

WeKnora 在已验证的企微长连接消息和 ChannelSession 上下文中注入 MCP `_meta.guoyu/bot-actor-context`：

```json
{
  "tenant_id": "1",
  "bot_id": "wecom-bot-001",
  "wecom_user_id": "zhangsan",
  "chat_id": "wmxxxxxxxx",
  "chat_type": "private",
  "channel_session_id": "channel-session-id",
  "weknora_session_id": "weknora-session-id",
  "request_id": "req-uuid"
}
```

MCP Server 从 `_meta` 读取后使用 `BOT_ACTOR_CONTEXT_SECRET` 生成 HMAC-SHA256，向 platform-api 发送 `X-Bot-Actor-Context` 和 `X-Bot-Actor-Signature`。platform-api 不接受模型普通参数中的同名字段替代该上下文。

## platform-file 受控读取

- 元数据：`GET /api/internal/artifacts/files/{fileId}/metadata`
- 二进制：`GET /api/internal/artifacts/files/{fileId}/content`
- 两个接口均要求平台服务账号的 `file:artifact:read` 权限、`X-Tenant-Id`、有效平台 Token 和有效 BotActorContext。
- content 接口返回 `X-File-Id`、`X-File-Name`、`X-File-Mime-Type`、`X-File-Size`、`X-File-SHA256` 可信响应头。
- 当前 MVP 只允许 `chat_type=private`/`single` 的会话读取合同交付文件。

### WeKnora 内部二进制桥接

- Worker 调用：`GET /internal/artifacts/platform-file/{fileId}/content`，地址与产出该引用的 MCP Service 同源。
- 鉴权复用该 MCP Service 已配置的 Bearer/API Key；WeKnora 不保存平台账号、平台 Token 或 `BOT_ACTOR_CONTEXT_SECRET`。
- Worker 在 `X-WeKnora-Bot-Actor-Context` 中发送 Base64URL 编码的已验证 BotActorContext。MCP 服务校验后重新使用 HMAC 签名，再调用 platform-api content 接口。
- 桥接响应最多 20 MiB，并返回 `X-File-Id`、`X-File-Name-Base64`、`X-File-Mime-Type`、`X-File-Size`、`X-File-SHA256`。Worker 必须复核文件 ID、大小和 SHA-256 后才能上传企微临时素材。
- 该路由是服务间数据面，不注册为 MCP Tool，不允许模型传入接收人或会话目标。

## WeKnora 交付任务建议

WeKnora 端落库表 `im_file_deliveries`，只保存上述引用与状态，不保存文件字节和永久 `media_id`：

`pending -> sending -> sent`，失败进入 `retry_wait`，超过上限进入 `failed`；幂等键为 `channel_session_id + document_id + version + delivery_purpose`。服务重启后继续领取 `pending/retry_wait`，实际发送必须绑定持有对应长连接 Adapter 的实例。

企微单聊的主动发送目标使用当前入站用户的 `userid`，群聊使用 `chatid`；合同 MVP 只创建单聊交付任务。Worker 依据引用来源记录的 `mcp_service_id` 调用上述内部桥接，最多尝试 3 次，且只在发送瞬间把字节保存在内存中。
