# 企业内部竞赛选手激活与鉴权实现设计

> 状态：P1-P7 代码已完成；P8 H5 接入待实施；RabbitMQ 联调归入 P9 真实 E2E。
> 日期：2026-07-21  
> 上游合同：docs/04_requirements/competition-player-activation-auth-contract.md

## 1. 实现结论

选手端采用独立的 competition_player_identity 主体和独立 Sa-Token loginType。核心业务仍位于 services/platform-api 的 competition 领域，选手端继续使用 D:\qianlvguoyu\qianlvguoyu_saas-dev\gjjd_appnext 的 uni-app H5。

以下历史能力不得复用：

- 管理后台 sys_user：避免选手获得后台角色、菜单和数据权限。
- 培训 App platform_user 和 platform_user_token：该链路含历史明文身份字段及 SHA-1 Token。
- AppSmsApplicationService：当前开发路径会保存并记录验证码，且包含万能验证码。

公告可靠投递采用 MySQL Outbox + RabbitMQ + 选手收件箱；H5 不直接消费 MQ。

## 2. 基线与关键实现决策

| 基线 | 实现决策 |
| --- | --- |
| competition_registration 仅关联 roster_item_id | 新增 player_id，以选手身份关联多场赛事资格 |
| 赛事编码仅在 tenant_id + code 内唯一 | 新增随机、全局唯一 player_entry_code，不把普通赛事编码作为跨租户登录入口 |
| TenantContextFilter 对公开 App 路径默认 tenant=1 | 新增专用选手入口解析；登录后严格校验 Header 租户与 Token 租户一致 |
| Sa-Token 已使用 UUID Token 与 Redis | 新增 StpLogic(competition-player)，不复用后台默认 loginType |
| 当前未引入 MQ | 引入 Spring AMQP 与 RabbitMQ；未部署 MQ 时不能以 Spring 内存事件替代 |
| 历史 App SMS 仅服务旧系统 | 新建 CompetitionSmsSender Provider Adapter；测试 Fake Provider 不输出验证码 |

### 2.1 选手入口与租户解析

赛事发布前生成 128 bit、URL-safe、全局唯一且不可猜测的 player_entry_code。管理端把 `qianlvguoyu-player://competition/activate?entry=<entryCode>` 作为二维码载荷；选手在预装 App 内扫码后解析 entry 并调用公开端点：

GET /api/competition/player-entry/resolve?entry={playerEntryCode}

该端点只返回 tenantId、eventId、eventName 与入口有效状态。它按入口码精确查询并显式绕过租户拦截器，不返回组织、名单、公告或联系人。H5 将 tenantId 写入竞赛专用 Store，后续请求携带 X-Tenant-Id。

选手 Token Bridge 必须比较 Token tenantId 与 Header tenantId；不一致返回 403 并记录 warn 日志。Header 从来不是租户授权依据。

## 3. 模块与共享文件

新增目录：

    competition/player/
      controller/ application/ domain/ entity/ mapper/ dto/ vo/ converter/ enums/
      security/  CompetitionPlayerPrincipal、CompetitionPlayerSecurityContext
      sms/       CompetitionSmsSender、Provider Adapter、Fake Provider
    competition/messaging/
      entity/ mapper/ publisher/ consumer/
      CompetitionOutboxEventEntity、CompetitionPlayerMessageEntity
    competition/roster/
      敏感名单导入与单个录入编排

SecurityConfig、TenantContextFilter、Sa-Token 配置和 Flyway 迁移均为共享文件，必须按单任务独占修改。H5 仅在 gjjd_appnext 的 competition-app 页面、API、Store 与新增竞赛工具内演进，不改历史培训鉴权链路。

## 4. 数据库与迁移设计

### 4.1 迁移规则

编码开始前必须查询目标 MySQL flyway_schema_history 最大版本；不得依据源码文件名猜版本。只新增迁移，禁止修改 V20260715_005__create_competition_foundation_tables.sql。

迁移按单一 Flyway 任务依次创建：

1. 选手身份、选手入口码与报名资格关联。
2. 短信挑战、短信发送投影与选手身份字段。
3. Outbox、公告接收人与选手消息收件箱。
4. 权限种子与现有角色授权。

### 4.2 选手身份与赛事资格

competition_player_identity 最小字段：

    id BIGINT PK
    tenant_id BIGINT NOT NULL
    id_card_ciphertext TEXT NOT NULL -- 兼容历史列名，实际保存规范化身份证号
    id_card_lookup_hash CHAR(64) NOT NULL
    mobile_ciphertext TEXT NOT NULL -- 兼容历史列名，实际保存规范化手机号
    mobile_lookup_hash CHAR(64) NOT NULL
    crypto_key_version VARCHAR(32) NOT NULL -- 兼容历史列，固定 plain-v1
    status VARCHAR(32) NOT NULL
    activated_at, disabled_at DATETIME NULL
    disabled_reason VARCHAR(500) NULL
    created_by, updated_by, created_at, updated_at, deleted

约束：唯一键 tenant_id + id_card_lookup_hash + deleted；唯一键 tenant_id + mobile_lookup_hash + deleted；索引 tenant_id + status + deleted。状态只允许 pending_activation、active、disabled。

competition_registration 新增 player_id 及索引 tenant_id + player_id + event_id + status + deleted。迁移回填期可临时为 NULL；名单确认完成后，有效资格不得缺少 player_id。选手首次激活时，将其所有有效资格更新为 eligible_activated。

competition_event 新增 player_entry_code VARCHAR(64) 并建立全局唯一约束。入口码只用于解析租户和赛事，不构成访问授权。

### 4.3 短信挑战、发送投影与消息

competition_sms_challenge 核心字段：id、tenant_id、player_id（激活前可空）、event_id（可空）、purpose、mobile_lookup_hash、code_bcrypt_hash、status、expires_at、verified_at、failed_attempts、send_attempts、request_ip_hash、device_id_hash 以及完整 BaseEntity 字段。状态为 pending_send、sent、verified、expired、locked、failed。

验证码为 6 位 SecureRandom 数字，仅在 challenge 保存 BCrypt 摘要。新增 competition_sms_delivery，在发送成功或过期前保存加密的手机号和验证码载荷；成功/过期后立即清除验证码密文。

新增 competition_outbox_event：业务聚合、事件类型、仅含业务标识的 Payload、投递状态、次数、下次重试、错误与 BaseEntity 字段。

新增 competition_player_message：player_id、event_id、notice_id、类型、送达和已读时间与 BaseEntity 字段。唯一键为 tenant_id + notice_id + player_id + deleted；公告正文不复制进收件箱。

## 5. 敏感数据、导入与单个录入

手机号规范化为 11 位中国大陆号码；身份证 trim 后统一大写 X。按当前产品决策，身份证号和手机号直接保存到现有历史列名中，不再使用 AES 加密，也不再依赖 AES 密钥或 lookupPepper。唯一匹配使用无配置的 SHA-256 摘要列。

PII 仅存在于导入/单录入请求 DTO 和身份实体；管理端 VO 继续只返回脱敏值，日志、异常、审计、错误报告、Outbox Payload 均禁止明文。

当前模板固定列：org_name、name、id_card_no、mobile、work_type、level；员工编号和企业编码不再属于选手录入或导入契约。详见
`docs/04_requirements/competition-player-registration-employee-no-and-time-conflict.md`。

导入或单录入的领域算法：

    解析单行（仅内存，禁止日志 PII）
    → 校验姓名、身份证、手机号
    → 计算两个 SHA-256 摘要
    → 锁定身份证或手机号命中的 player_identity
    → 无命中：创建 pending_activation Player
    → 同 Player 命中：复用 Player
    → 两个 hash 命中不同 Player：返回脱敏冲突
    → 创建 roster_item 与 registration(player_id)

批量导入按预检快速返回、确认后异步处理实现；单个录入可同步返回，但必须复用相同领域校验、组织范围、名额与审计逻辑。

## 6. 认证、鉴权与会话

### 6.1 独立选手主体

新增名为 competitionPlayerStpLogic 的 StpLogic(competition-player) Bean。登录 ID 为 competition_player_identity.id，Token 使用现有 Redis UUID 会话，最大时长沿用当前 3 天。Token 不含身份证、手机号、赛事 ID、资格或成绩。

新增 CompetitionPlayerTokenBridgeFilter，只匹配需认证的选手资源端点：

1. 读取 Bearer Token 的 competition-player loginType 会话。
2. 加载选手身份，检查 status=active、未删除、Token tenant 与 Header tenant 一致。
3. 设置 CompetitionPlayerPrincipal(playerId, tenantId) 与最小 ROLE_COMPETITION_PLAYER 到 Spring Security Context。
4. 将主体写入 CompetitionPlayerSecurityContext；ApplicationService 仅从该上下文获取 playerId，不使用后台 SecurityContext.currentUser()。

该 Filter 放在 SaTokenBridgeFilter 之前，后台 Token 与 Player Token 互不识别。异步消费者必须显式传递 tenantId、eventId、noticeId；Worker 自行设置并 finally 清理 TenantContextHolder，禁止读取 HTTP 线程安全上下文。

### 6.2 API 与事务边界

| 用例 | 公开性 | 核心规则 |
| --- | --- | --- |
| GET /player-entry/resolve | 公开 | 精确入口码、最小返回、限流 |
| POST /player-activation/send-code | 公开 | 锁定有效待激活资格；创建 challenge、delivery、outbox |
| POST /player-activation/verify | 公开 | 锁定 challenge/player；激活身份、更新有效资格、初始化 BCrypt 初始密码；不签发会话 |
| POST /player-auth/login | 公开 | 校验手机号与密码，签发独立选手会话 |
| POST /player-auth/send-code | 公开 | 仅 active player；不得枚举手机号 |
| POST /player-auth/login | 公开 | 码验证后创建 Player 会话 |
| GET /player-auth/me | 登录 | 返回脱敏手机号、身份状态、可访问赛事摘要 |
| POST /player-auth/change-mobile/* | 登录 | 旧号验证凭证 + 新号验证；更新后撤销其他会话 |
| POST /players/{playerId}/disable | 后台权限 | 停用、审计、事务后撤销全部会话 |

公开接口统一响应：如信息匹配，验证码将发送至企业登记手机号。错误、过期和不存在均不得透露身份是否存在。

### 6.3 风控

Redis 用于 60 秒重发、手机号/选手日上限、IP/设备频控；数据库 challenge 表是最终审计与失败次数事实。Redis 不可用时，短信发送接口返回 503，不降级为无限制发送。

规则：验证码 5 分钟有效；同用途重发间隔 60 秒；手机号与选手各 5 次/日；单挑战 5 次失败即失效；累计失败锁定 30 分钟。图形验证码能力未接入前，超阈值直接拒绝并审计。

### 6.4 外部短信 SDK 引入时点

外部短信 SDK 只在 P5“短信挑战与 Adapter”任务引入，前提是 P1 至 P4 已完成并通过迁移、身份摘要、独立 Player 会话和权限测试。不得在 Player 建表、名单导入、H5 页面或验证码领域规则开发阶段提前添加供应商 SDK。

P5 先定义 CompetitionSmsSender 接口和 FakeCompetitionSmsSender；本地与自动化测试默认使用 Fake 实现，既不调用外部网络也不在日志中输出验证码。仅当下列条件齐备时，才新增已选短信供应商的官方 SDK 和 Production Adapter：

1. 已书面确认供应商、区域、签名、验证码模板、内容变量、模板报备状态、单价和回执语义。
2. 已按供应商官方文档核对请求参数、频率限制、错误码、超时和重试边界，不假设 OpenAPI 兼容。
3. 已配置测试/预生产独立账号、签名和模板；生产 AccessKey、Secret、TemplateCode 仅从受控环境变量或密钥管理服务读取。
4. 已完成 Provider Adapter 的 Mock 测试、发送成功/拒绝/超时/限流/回执失败测试和 Outbox 重试测试。

Controller 和认证服务不得直接调用 SDK。短信发送 Worker 只能通过 CompetitionSmsSender 调用 Provider Adapter；SDK 故障更新 delivery/outbox 状态并触发重试或告警，绝不回滚已创建的验证码挑战。

## 7. 换绑、停用与名单撤回

换绑链路：验证旧号 → 签发短时 change_ticket → 验证新号 → 锁定 Player → 查重 → 更新密文/摘要 → 撤销其他会话。change_ticket 只保存挑战 ID、playerId、用途与短时 TTL，不能作为登录 Token。

旧号不可用时仅允许有权限的企业管理员人工重置，必须记录原因、复核依据和操作日志，并撤销所有会话。

| 动作 | Player 身份 | 当前赛事资格 | 其他赛事资格 | 会话 |
| --- | --- | --- | --- | --- |
| 撤回 A 赛事名单 | 不变 | withdrawn | 不变 | 不撤销；后续 A 资源访问 403 |
| A 资格到期 | 不变 | expired | 不变 | 不撤销；后续 A 资源访问 403 |
| 停用选手 | disabled | 保留历史 | 保留历史 | 立即撤销全部 |
| 恢复选手 | active | 不自动恢复 withdrawn/expired | 不变 | 用户重新登录 |
| 所有赛事撤回 | active | 均 withdrawn | 均 withdrawn | 允许登录，显示空赛事 |

## 8. 公告 Outbox 与 RabbitMQ

公告发布事务同时将公告状态更新为 published 并写入 outbox 事件 competition.notice.published.v1。Outbox Dispatcher 投递到 RabbitMQ topic exchange competition.events，路由键为 competition.notice.published.v1，消费者从 competition.notice.delivery 队列创建收件箱。

事件 Payload 仅包含 tenantId、eventId、noticeId、publishedAt，不带公告正文、手机号或身份证。Dispatcher 使用状态抢占、指数退避和 RabbitMQ Publisher Confirm；达到最大次数后告警。消费者在收件箱事务成功返回后由容器确认；失败消息拒绝并进入死信队列。

接收人是同租户、同赛事、资格 eligible_activated、选手 active、均未删除的 player。收件箱落库后冻结接收范围；新激活选手不自动补收历史公告，补发必须为明确后台动作。名单撤回或停用后，详情读取仍实时校验资格。

## 9. H5 接入

H5 仅维护竞赛专用 tenantId、entryCode、currentEvent、playerSession、unreadCount；不读取培训 User Store。

1. 打开入口链接，解析 entry 并调用 resolve。
2. 未登录时先展示扫码首页；扫码后仅提供手机号短信激活，日常登录使用手机号与密码；首次激活仅输入企业登记手机号，赛事由入口码确定，初始密码为身份证后 6 位的 BCrypt 哈希。
3. 激活成功后跳转密码登录；登录成功后立即请求 player-auth/me，恢复可访问赛事。
4. 首页通过聚合接口加载赛事卡、公告摘要、未读数；积分、排名、晋级继续隐藏或显示待规则发布。
5. 401 清理竞赛专用 Store 并回登录页；403 显示赛事资格失效。

生产交付前完成 HTTPS、CSP 与 XSS 审查。页面刷新后可先渲染壳层，但后台 me 校验失败必须静默清理会话。

## 10. 编码任务与门槛

| 顺序 | 任务 | 独占范围 | 最低验证 |
| --- | --- | --- | --- |
| P1 | Flyway、Entity、Mapper | 迁移、player/entity | 空库迁移；BaseEntity/唯一键检查 |
| P2 | 名单模板、预检、单个录入 | roster/registration | 冲突、名额、跨企业、脱敏错误 |
| P3 | Player 身份与脱敏 | player/domain、config | 明文字段规范化、SHA-256 唯一匹配、无 PII 日志 |
| P4 | Player Sa-Token 与 Filter | SecurityConfig、TenantContextFilter | 401/403、Header 篡改、停用失效 |
| P5 | 短信挑战与 Adapter | player/sms、challenge | 重发、过期、失败、Redis 503 |
| P6 | 激活、登录、换绑 API | player/controller/application | MockMvc 正反、并发验证 |
| P7 | Outbox、RabbitMQ、收件箱 | messaging、公告发布、选手消息 API | 已完成代码与单测；MQ 实例联调留待 P9 |
| P8 | uni-app H5 | competition-app 页面/API/Store | H5 构建、登录与消息冒烟 |
| P9 | 真实 E2E | 测试/部署说明 | 导入→激活→发布→MQ→H5 |

每个任务必须遵守分层、审计、Mapper 判空、异步 TenantContext、UTF-8 无 BOM 与不改历史 Flyway 规则。RabbitMQ 地址、TLS、死信策略与账号经环境变量配置，严禁提交真实密钥。

## 11. 阿里云短信 SDK 落地

P5 已选定阿里云国内短信 `Dysmsapi/2017-05-25`，使用官方 Java V2.0 Tea SDK `com.aliyun:dysmsapi20170525:4.6.0`。`AliyunCompetitionSmsSender` 仍通过 `CompetitionSmsSender` 接口隔离；仅在 `platform.competition.player.sms.aliyun.enabled=true` 时替代 Fake 实现。

生产环境仅通过 `ALIBABA_CLOUD_ACCESS_KEY_ID` 与 `ALIBABA_CLOUD_ACCESS_KEY_SECRET` 注入 RAM 最小权限凭证；还需配置已审核通过的 `COMPETITION_PLAYER_SMS_ALIYUN_SIGN_NAME` 和仅含 `code` 变量的 `COMPETITION_PLAYER_SMS_ALIYUN_TEMPLATE_CODE`。SDK 使用 `SendSms`、`cn-hangzhou` 与 `https://dysmsapi.aliyuncs.com`；只有响应 `Code=OK` 才记录受理并保存 `BizId`。不得记录手机号、验证码、AccessKey 或完整供应商响应。因短信接口不提供幂等能力，超时不会自动重试。
