# 竞赛端登录与扫码激活

## 登录链路

旧 H5 登录页的“进入竞赛端”只打开竞赛账号密码页，不依赖旧 PHP 登录态，也不先扫描二维码。账号是手机号，初始密码是身份证后六位。

1. 已激活选手输入正确账号密码后，平台签发独立竞赛 token，直接进入竞赛首页。
2. 未激活的已导入选手输入正确账号密码后，返回 `pending_activation`，页面提示账号未激活并展示扫码激活按钮。
3. 扫码获取赛事入口码后，选手验证短信验证码。校验成功会更新报名资格为 `eligible_activated`，并直接返回竞赛 session；H5 保存该 session 后进入竞赛首页。
4. 未导入手机号返回“账号不存在”；已导入但密码错误返回“账号或密码错误”。这两种情况均不展示激活入口。

竞赛 token 使用独立的 `competition_player_token`、`competition_player_tenant_id` 存储键，与旧 PHP 登录 token 隔离。

## 表设计

| 表 | 用途 | 关键字段 / 约束 |
| --- | --- | --- |
| `competition_player_identity` | 竞赛账号主体 | 身份证、手机号仅保存密文和检索哈希；`password_hash`、`password_initialized_at`、状态与激活时间均归属该表。`active_mobile_lookup_hash` 是由手机号哈希和逻辑删除状态生成的列，并带全局唯一索引，确保全部租户内仅一个有效手机号账号。保留完整 BaseEntity 审计字段。 |
| `competition_registration` | 选手与赛事资格关系 | `player_id`、`event_id`、`org_id`、`roster_item_id`、`source`、`status`。短信激活成功后从 `eligible_unactivated` 更新为 `eligible_activated`。 |
| `competition_roster_import` / `competition_roster_item` | 企业导入来源与追溯 | 导入名单通过身份证和手机号创建或关联 `player_id`；创建身份时即写入身份证后六位的 BCrypt 初始密码。保留批次、姓名、工号、校验状态和审计字段。 |
| `competition_player_sms_challenge` | 激活短信验证码审计 | `player_id`、`purpose=activation`、手机号哈希、验证码哈希、有效期、失败次数、投递状态、供应商消息 ID、来源哈希和审计字段。`challengeId` 绑定发送与校验。 |

不新增独立账号表：账号、密码状态与手机号唯一性都属于 `competition_player_identity`，赛事资格仍属于 `competition_registration`。

## 数据迁移前置条件

`V20260723_001__enforce_global_competition_player_mobile.sql` 会先检查未逻辑删除的跨租户重复手机号哈希。存在重复时迁移以明确错误终止，管理员处理冲突后才能再次执行。迁移不包含测试账号或明文账号数据。
