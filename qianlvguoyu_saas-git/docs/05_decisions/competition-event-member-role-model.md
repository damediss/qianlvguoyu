# 竞赛人员与赛事角色模型

## 决策

`competition_player_identity` 保持为竞赛域的全局自然人安全身份主体：身份证和手机号只在此表以 AES-GCM 密文及 HMAC 检索摘要保存。由于该表已被选手端认证接口使用，本阶段只修正表注释，不进行破坏性改名。

人员在赛事中的身份由 `competition_event_member` 表示，而不是写入人员主体或 `sys_user`。唯一约束为 `(tenant_id, event_id, person_id, role_type, deleted)`，所以同一人可以参与多个赛事、在同一赛事拥有多个不同角色，但不能重复拥有同一种角色。

角色代码：`contestant`、`expert`、`referee`、`event_admin`、`staff`。

## 表结构

```text
competition_player_identity (全局人员主体)
  └─ competition_event_member (赛事成员角色)
       ├─ competition_contestant_profile
       ├─ competition_expert_profile
       ├─ competition_referee_profile
       ├─ competition_member_stage_assignment
       └─ competition_member_avoidance_org

competition_event_role_conflict_rule (赛事内角色冲突/回避规则)
```

`competition_registration` 保留为选手参赛资格与报名审核的历史兼容表，新增 `member_id` 指向选手角色成员；选手赛程、分组、积分和晋级字段放入 `competition_contestant_profile`，避免挤入通用成员表。

## 迁移与兼容

Flyway `V20260727_002__refactor_competition_event_member_roles.sql` 会为已有且具备 `player_id` 的报名记录创建 `contestant` 成员和选手扩展，并回填 `competition_registration.member_id`。

历史上没有 `player_id` 的报名记录不会自动关联到任何人，因为名单表缺少可安全重建自然人主体的完整加密身份信息。此类记录需要在后台补录、验证身份证和手机号后，再创建人员主体和赛事成员。

现有“确认名单并生成参赛资格”服务已同步创建选手成员和选手扩展，确保后续数据不再脱离新模型。

## 权限与冲突规则

权限校验的目标形式是 `hasEventRole(eventId, roleType)`，审计日志应记录 `event_id` 与实际操作角色。`competition_event_role_conflict_rule` 只保存业务规则；在专家、裁判的授予接口上线时，应用层必须在写入 `competition_event_member` 前执行规则校验及回避单位、赛段授权校验。

默认业务建议：不同赛事允许任意角色组合；同一赛事允许专家和裁判同时存在，但选手与裁判默认应配置为 `forbidden`，如确需放开则应配置为 `recusal_required` 并在评分、试题和晋级流程中执行强制回避。
