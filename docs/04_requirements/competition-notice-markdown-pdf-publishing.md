# 竞赛公告 Markdown / PDF 双类型发布需求与 API 合同

> 状态：已确认，进入实现阶段
> 日期：2026-07-24
> 适用范围：`services/platform-api` 竞赛公告、管理端公告页面及选手消息 API
> 前置条件：现有“草稿 → 发布 → Outbox/MQ → 选手收件箱”链路已通过联调

## 1. 目标与范围

现有竞赛公告扩展为两种互斥内容类型：

| 类型 | `contentType` | 内容 | 资产 |
| --- | --- | --- | --- |
| 图文公告 | `markdown` | Markdown 正文，发布时必填，最多 100,000 字符 | 0～多张 JPG/PNG/WebP 图片，单张不超过 10MB |
| PDF 赛事文件 | `pdf` | 可选纯文本简介，最多 2,000 字符 | 必须且只能有 1 个 PDF，不超过 50MB |

本需求包括：

1. 公告类型、正文容量和资产关系的数据模型。
2. 管理端公告草稿、资产上传/读取/删除、发布及类型筛选 API。
3. 选手消息返回的公告类型与资产信息，以及选手身份下的受控资产读取 API。
4. 管理端 Markdown 编辑、图片插入、PDF 上传/预览及已发布公告只读展示。
5. 兼容现有公告、现有 Outbox/MQ 事件和收件箱投递规则。

本期不新建或改造选手 H5 页面，只交付选手 API 与渲染/读取契约，供后续 H5 接入。

## 2. 核心业务规则

### 2.1 类型互斥

1. 一条公告只能是 `markdown` 或 `pdf`，不能同时发布 Markdown 图片资产和 PDF 资产。
2. 创建、更新请求未传 `contentType` 时按 `markdown` 处理，以兼容旧客户端。
3. 草稿允许内容暂不完整；发布动作执行完整性终检。
4. 已发布公告继续保持不可修改，正文、类型和资产均不可新增、替换或删除。

### 2.2 Markdown 公告

1. 草稿阶段正文可以为空；发布时去除首尾空白后必须非空。
2. 正文最大 100,000 个字符，以服务端 Java 字符串长度校验为准。
3. 只允许绑定 `inline_image` 资产，不得绑定 PDF。
4. 图片只允许 JPG、PNG、WebP，单张最大 10MB。
5. 正文中的内部图片统一保存为 `notice-asset://{assetId}`，不得保存文件系统路径、对象存储地址、预签名地址或通用文件直链。
6. 发布时解析全部 `notice-asset://` 引用，每个 `assetId` 必须属于当前租户、当前公告且未删除；跨公告、跨租户及不存在的引用均拒绝发布。
7. Markdown 原始 HTML、外链图片、`javascript:` URL 和 `data:` URL 不受支持。前后端均不得信任 Markdown 生成的 HTML，展示前必须清理。
8. 已上传但未被正文引用的当前公告图片不阻断草稿保存；发布前由管理端提示，发布成功后可由后续文件清理任务处理。本期发布结果不因未引用图片失败。

### 2.3 PDF 公告

1. 草稿阶段可以暂未上传文件；发布时必须且只能绑定 1 个 `pdf` 资产。
2. `content` 表示可选纯文本简介，不解释为 Markdown 或 HTML，最多 2,000 个字符。
3. PDF 文件最大 50MB。上传时同时校验扩展名 `.pdf`、允许的 MIME 类型和文件头 `%PDF-`，任一不匹配均拒绝。
4. 上传新 PDF 时替换草稿已有 PDF；新文件成功落库并绑定后再删除旧绑定，失败时保留旧 PDF。
5. PDF 公告不得绑定 `inline_image`，简介也不得引用 `notice-asset://`。

### 2.4 草稿切换类型

1. 管理端切换类型前必须二次确认，明确提示不兼容资产将被删除。
2. `markdown → pdf`：保存类型变更前删除该公告的全部 `inline_image` 绑定并清空 Markdown 正文；随后允许填写 PDF 简介和上传 PDF。
3. `pdf → markdown`：保存类型变更前删除 PDF 绑定并清空 PDF 简介；随后允许编辑 Markdown。
4. 后端更新类型时也必须清理不兼容绑定，不能依赖前端正确执行；相关资产删除与公告类型更新应具备一致性，失败不得形成类型与资产混用的草稿。
5. 文件已上传但绑定或类型切换失败时，文件服务执行删除补偿；补偿失败需记录可追踪的错误日志，禁止向响应暴露存储路径。

### 2.5 发布与投递

1. 发布服务先以行锁读取草稿，校验版本、公告类型、正文和资产完整性，再更新为 `published`。
2. 发布状态更新与写入现有 `competition.notice.published.v1` Outbox 必须处于同一事务。
3. 事件名称、路由键、Payload 和 MQ 消费逻辑不变；Payload 仍只包含 `tenantId`、`eventId`、`noticeId`、`publishedAt`。
4. 接收人冻结、幂等收件箱、已读状态和“新激活选手不自动补收历史公告”的既有规则不变。
5. 重复或并发发布返回冲突，且同一公告只写入一次有效 Outbox 事件。

## 3. 数据合同

### 3.1 `competition_notice`

新增/调整：

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `content_type` | `VARCHAR(32)` | 非空，默认 `markdown`，仅允许 `markdown` / `pdf` |
| `content` | `MEDIUMTEXT` | 草稿可为空字符串；长度由应用按类型校验 |

历史数据全部回填为 `markdown`，不得改变标题、正文、版本、状态、发布时间、收件箱或已读状态。增加包含 `tenant_id`、`event_id`、`content_type`、`status`、`deleted` 的查询索引。

### 3.2 `competition_notice_asset`

新增公告资产表：

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `id` | `BIGINT` | 雪花 ID，API 中以字符串序列化 |
| `tenant_id` | `BIGINT` | 租户隔离 |
| `notice_id` | `BIGINT` | 所属公告 |
| `file_id` | `BIGINT` | 文件服务文件标识，不对客户端暴露通用下载能力 |
| `asset_type` | `VARCHAR(32)` | `inline_image` / `pdf` |
| `sort_order` | `INT` | 非负，默认 0 |
| `deleted` | `TINYINT` | MyBatis-Plus 逻辑删除 |
| `created_by` / `updated_by` | `BIGINT` | 审计人 |
| `created_at` / `updated_at` | `DATETIME` | 审计时间 |

约束与索引：

- 唯一约束：`(tenant_id, notice_id, file_id, deleted)`。
- 查询索引：`(tenant_id, notice_id, asset_type, sort_order, deleted)`。
- 单 PDF 规则由锁定公告草稿后的服务校验保证。
- Entity 必须继承 `BaseEntity`，DDL 必须包含完整的 6 个继承列：`tenant_id`、`deleted`、`created_by`、`updated_by`、`created_at`、`updated_at`。
- 逻辑删除使用 `deleteById` / `deleteBatchIds`，禁止手动 `setDeleted(1)`。

Flyway 必须使用新版本迁移，不得修改已提交或已应用迁移。实施前以联调库 `flyway_schema_history` 为准核对最大版本；已知最大版本为 `20260724.001` 且仓库可能缺少对应源脚本，需先同步历史脚本，再使用下一可用版本。

## 4. API 合同

所有 JSON 响应沿用 `{ code, message, data }`，ID 按字符串传输。后台写接口使用 `competition:notice:publish`，后台查看接口使用 `competition:notice:view`。

### 4.1 公告对象

公告创建、详情和分页记录扩展如下：

```json
{
  "id": "1900000000000000001",
  "eventId": "1800000000000000001",
  "title": "决赛赛事手册",
  "contentType": "pdf",
  "content": "请参赛选手提前阅读赛事手册。",
  "assets": [
    {
      "assetId": "1910000000000000001",
      "assetType": "pdf",
      "originalName": "决赛赛事手册.pdf",
      "contentType": "application/pdf",
      "sizeBytes": 1048576,
      "sourceUri": "/api/competition/notices/1900000000000000001/assets/1910000000000000001"
    }
  ],
  "version": 1,
  "status": "draft",
  "publishedBy": null,
  "publishedAt": null,
  "createdBy": "1001",
  "createdAt": "2026-07-24T10:00:00",
  "updatedAt": "2026-07-24T10:00:00"
}
```

`sourceUri` 是当前身份对应的受控应用 API 路径，不是对象存储 URL。客户端仍须携带当前鉴权凭据和 `X-Tenant-Id`，不得将其视为公开链接。

### 4.2 创建、更新和查询

| 方法 | 路径 | 变更 |
| --- | --- | --- |
| `POST` | `/competition/events/{eventId}/notices` | 请求增加可选 `contentType`；首次资产上传前可自动创建不完整草稿 |
| `PUT` | `/competition/notices/{noticeId}` | 请求增加可选 `contentType`；仅草稿可更新 |
| `GET` | `/competition/events/{eventId}/notices` | 增加可选查询参数 `contentType=markdown|pdf` |
| `GET` | `/competition/notices/{noticeId}` | 返回 `contentType` 和 `assets` |
| `POST` | `/competition/notices/{noticeId}/publish` | 发布前执行类型与资产终检 |

创建/更新请求：

```json
{
  "title": "赛程调整通知",
  "contentType": "markdown",
  "content": "## 调整说明\n\n![赛程图](notice-asset://1910000000000000002)",
  "version": 1
}
```

校验规则：

- `title` 必填，最多 256 字符。
- `contentType` 省略时默认 `markdown`。
- 草稿保存时 `content` 可为空；Markdown 最多 100,000 字符，PDF 最多 2,000 字符。
- 更新继续使用 `version` 做乐观并发控制。
- 非法类型、长度超限和文件格式错误返回统一参数/业务错误；已发布或版本冲突返回 `409`。

### 4.3 管理端资产

| 方法 | 路径 | 权限 | 行为 |
| --- | --- | --- | --- |
| `POST` | `/competition/notices/{noticeId}/assets` | `competition:notice:publish` | `multipart/form-data` 上传并绑定资产 |
| `GET` | `/competition/notices/{noticeId}/assets/{assetId}` | `competition:notice:view` | 流式返回图片或 PDF |
| `DELETE` | `/competition/notices/{noticeId}/assets/{assetId}` | `competition:notice:publish` | 仅草稿允许逻辑删除绑定并清理文件 |

上传请求字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `file` | 是 | 上传文件 |
| `assetType` | 是 | `inline_image` / `pdf`，必须与公告类型相容 |
| `sortOrder` | 否 | 非负整数，默认 0 |

上传成功返回单个资产对象。`inline_image` 上传成功后，管理端将 `notice-asset://{assetId}` 插入 Markdown；`pdf` 上传成功后返回新的唯一 PDF 资产。

读取接口以流式响应返回文件：

- `Content-Type` 使用经服务端校验并保存的媒体类型。
- 默认 `Content-Disposition: inline; filename*=UTF-8''...`，支持浏览器预览；管理端下载时可用 Blob 主动保存。
- 响应不得包含物理存储路径或通用对象存储直链。
- 公告、资产或租户不匹配时按资源不可见处理。

### 4.4 选手消息与资产读取

现有 `CompetitionPlayerMessageVO` 保留 `content`，并增加 `contentType` 和 `assets`。选手消息中的 `assets[].sourceUri` 必须使用消息归属路径：

```json
{
  "messageId": "1920000000000000001",
  "eventId": "1800000000000000001",
  "noticeId": "1900000000000000001",
  "type": "notice",
  "title": "决赛赛事手册",
  "contentType": "pdf",
  "content": "请参赛选手提前阅读赛事手册。",
  "assets": [
    {
      "assetId": "1910000000000000001",
      "assetType": "pdf",
      "originalName": "决赛赛事手册.pdf",
      "contentType": "application/pdf",
      "sizeBytes": 1048576,
      "sourceUri": "/api/competition/player/messages/1920000000000000001/assets/1910000000000000001"
    }
  ],
  "publishedAt": "2026-07-24T10:30:00",
  "receivedAt": "2026-07-24T10:30:01",
  "readAt": null
}
```

新增：

| 方法 | 路径 | 鉴权 | 行为 |
| --- | --- | --- | --- |
| `GET` | `/competition/player/messages/{messageId}/assets/{assetId}` | 选手 Token | 流式预览/下载该消息对应公告资产 |

每次读取必须实时校验：

1. Token 对应的选手身份为 `active`，且 Token 租户与请求租户一致。
2. `messageId` 属于当前选手和当前租户。
3. 消息关联公告存在且状态为 `published`。
4. 当前选手对公告所属赛事仍具有有效的 `eligible_activated` 资格；名单撤回、资格过期或选手停用后禁止读取。
5. `assetId` 属于该消息关联公告、当前租户且未删除。

不得仅凭 `noticeId`、`assetId` 或曾经存在的收件箱记录授权文件访问。失败路径不得泄露其他租户、选手、公告或文件是否存在。

## 5. 文件、安全与审计要求

1. PDF 与图片均需校验文件大小、扩展名、声明 MIME 和魔数，不能只信任浏览器上传的 `Content-Type`。
2. JPG 识别 JPEG SOI，PNG 识别 PNG 签名，WebP 识别 `RIFF....WEBP`，PDF 识别 `%PDF-`。
3. 文件上传、保存和响应使用流式或临时文件路径，禁止将 50MB PDF 整体读入 JVM byte array。
4. 文件服务内部读取必须接收显式 `tenantId` 和 `fileId`；若进入异步或 Worker 线程，租户与操作者在 HTTP 线程捕获后通过参数传递，完整下游调用链不得读取 `SecurityContext`。
5. Worker 线程访问 Mapper 前必须设置自己的 `TenantContextHolder`，并在 `finally` 中清理；禁止依赖请求线程的 ThreadLocal。
6. 资产创建、替换、删除、类型切换和发布均记录操作日志。认证、授权和越权拒绝路径记录 warn/error 日志，但不记录文件内容、存储路径或敏感 Token。
7. Markdown 展示使用 `sanitize-html` 白名单清理；只解析内部图片协议，拒绝外链、脚本、事件属性和内联数据 URL。
8. Blob URL 仅在页面生命周期内使用，关闭预览、替换资源或组件卸载时调用 `URL.revokeObjectURL`。

## 6. 管理端交互要求

1. 将现有公告编辑弹窗改为宽抽屉，先选“图文公告”或“PDF 赛事文件”。
2. Markdown 模式使用 `md-editor-rt` 源码/预览编辑器：
   - 图片上传回调先确保公告草稿已创建，再上传并插入内部 URI。
   - 预览阶段把内部 URI 转换为带后台凭据读取的 Blob URL。
   - 禁用原始 HTML和外链图片，渲染结果经 `sanitize-html` 清理。
3. PDF 模式提供拖拽上传、文件名、文件大小、替换、删除、Blob 预览/下载和纯文本简介。
4. 新公告首次上传资产时，先以当前标题、类型和空内容自动创建草稿并取得 `noticeId`，再上传绑定；创建失败不得继续上传。
5. 切换类型时二次确认并调用草稿更新流程清理不兼容内容和资产。
6. 公告列表增加类型标签和类型筛选；发布确认框展示类型，PDF 公告同时展示文件名。
7. 已发布公告只读：Markdown 安全渲染，PDF 显示简介并提供受控预览/下载。
8. 401 清理后台登录态；403 提示权限不足；文件校验失败、409 冲突和 500/代理错误使用现有统一错误处理。

## 7. 兼容性与明确不变项

1. 迁移前公告全部视为 `markdown`，原正文原样保留。
2. 旧创建/更新请求不传 `contentType` 时仍能创建/更新 Markdown 公告。
3. 现有选手消息 `content` 字段继续存在；新字段是向后兼容扩展。
4. `competition.notice.published.v1` 名称、Payload、Outbox 调度、MQ 路由、消费者和收件箱唯一规则不变。
5. 不新增权限码，继续复用 `competition:notice:publish` 与 `competition:notice:view`。
6. 不改变发布后不可编辑、接收人冻结、资格实时复核、已读和历史公告投递规则。
7. 不创建选手 H5 工程或页面；H5 后续必须按本合同的内部 URI、消息资产和受控读取接口接入。

## 8. 验收案例

### 8.1 后端与 API

1. 创建/更新 Markdown 与 PDF 草稿成功；旧请求缺少 `contentType` 时结果为 `markdown`。
2. 草稿允许空内容；空 Markdown、超 100,000 字符 Markdown、超 2,000 字符 PDF 简介均在发布/保存相应阶段正确拒绝。
3. Markdown 发布时，合法当前公告图片成功；不存在、跨公告、跨租户图片引用失败；绑定 PDF 或外链/数据图片失败。
4. PDF 发布时，恰好一个合法 PDF 成功；缺失、多 PDF、混入图片失败。
5. JPG/PNG/WebP 合法文件上传成功；超 10MB、伪扩展名、错误 MIME 或魔数失败。
6. 合法 PDF 上传成功；超 50MB、非 `.pdf`、错误 MIME 或非 `%PDF-` 文件失败。
7. 新 PDF 替换成功后旧文件不可读；新文件绑定失败时旧 PDF 仍可读，孤儿文件进入删除补偿。
8. 类型切换清理不兼容内容与资产；已发布公告的类型、正文和资产增删改均返回冲突。
9. 重复/并发发布只有一次状态迁移和一次有效 Outbox；MQ Payload 与既有版本完全一致。
10. 管理员读取资产需正确权限和租户；无权限、跨租户及错误归属不可读。
11. 选手只能读取本人收件箱对应公告的资产；其他选手、其他消息、跨租户、未发布公告、名单撤回、资格过期或选手停用均拒绝。
12. 文件响应为流式响应，中文文件名正确，日志和响应不暴露物理路径、Token 或对象存储凭据。

### 8.2 数据库

1. 空库可顺序执行全部 Flyway；历史联调库可无损升级。
2. `SHOW COLUMNS` 证明资产表字段与 Entity（含完整 BaseEntity 字段）一一对应。
3. 历史公告均为 `markdown`，标题、正文、状态、收件箱和已读状态不变。
4. 公告—文件唯一约束、租户/公告/类型查询索引生效。

### 8.3 管理端与联调

1. 管理端构建和 TypeScript 检查通过；后端测试、编译通过。
2. 跑通 Markdown + 多图的“创建草稿 → 上传 → 编辑/预览 → 发布 → MQ → 选手消息查询 → 受控图片读取 → 已读”链路。
3. 跑通 PDF 的“创建草稿 → 上传/替换 → 预览 → 发布 → MQ → 选手消息查询 → 受控 PDF 读取 → 已读”链路。
4. 验证中文标题、正文和文件名，401/403/404/409、超限与损坏文件、局域网 HTTP，以及 Blob URL 回收。
5. 源码、配置、SQL 与 Markdown 均为 UTF-8 without BOM，Git diff 中文正常，不覆盖工作区无关改动。
