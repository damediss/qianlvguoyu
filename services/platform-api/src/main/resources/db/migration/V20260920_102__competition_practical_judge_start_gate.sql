-- 竞赛实操裁判端统一开考：保留组长身份，记录三台裁判设备的逐人确认。
-- 版本号同时核对测试库与生产库后取当前最大版本 20260920.101 的下一号。

ALTER TABLE competition_practical_judge_group
  ADD COLUMN leader_referee_member_id BIGINT NULL COMMENT '场次裁判组组长对应的赛事成员ID' AFTER official_count,
  ADD KEY idx_comp_practical_judge_group_leader (
    tenant_id, leader_referee_member_id, status, deleted
  );

ALTER TABLE competition_practical_assignment
  ADD COLUMN start_confirmed_at DATETIME NULL COMMENT '当前裁判在本设备确认选手和试卷的时间' AFTER assigned_at,
  ADD COLUMN start_confirmed_by BIGINT NULL COMMENT '执行开考确认的平台用户ID' AFTER start_confirmed_at,
  ADD COLUMN started_at DATETIME NULL COMMENT '组长统一开考时间' AFTER start_confirmed_by,
  ADD COLUMN started_by BIGINT NULL COMMENT '执行统一开考的组长平台用户ID' AFTER started_at;

-- 为已经初始化的正式比赛回填组长，避免升级后现有场次无人可以开考。
UPDATE competition_practical_judge_group runtime_group
JOIN competition_practical_assignment runtime_assignment
  ON runtime_assignment.tenant_id = runtime_group.tenant_id
 AND runtime_assignment.judge_group_id = runtime_group.id
 AND runtime_assignment.deleted = 0
JOIN competition_referee_assignment configured_assignment
  ON configured_assignment.tenant_id = runtime_group.tenant_id
 AND configured_assignment.contestant_member_id = runtime_assignment.contestant_member_id
 AND configured_assignment.referee_member_id = runtime_assignment.referee_member_id
 AND configured_assignment.referee_group_id IS NOT NULL
 AND configured_assignment.status = 'active'
 AND configured_assignment.deleted = 0
JOIN competition_referee_group configured_group
  ON configured_group.tenant_id = configured_assignment.tenant_id
 AND configured_group.id = configured_assignment.referee_group_id
 AND configured_group.leader_referee_member_id = runtime_assignment.referee_member_id
 AND configured_group.deleted = 0
SET runtime_group.leader_referee_member_id = configured_group.leader_referee_member_id,
    runtime_group.updated_at = NOW()
WHERE runtime_group.leader_referee_member_id IS NULL
  AND runtime_group.deleted = 0;
