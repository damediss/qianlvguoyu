-- 竞赛实操考评表、正式比赛任务及裁判端账号关联。
-- 考评表由已发布实操试卷快照派生，选手分卷时同步保存考评表关联。

CREATE TABLE competition_practical_assessment_sheet (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  version_no INT NOT NULL DEFAULT 1,
  sheet_code VARCHAR(96) NOT NULL,
  title VARCHAR(255) NOT NULL,
  question_count INT NOT NULL,
  total_score DECIMAL(12,2) NOT NULL,
  sheet_json JSON NOT NULL COMMENT '总评分表和逐题评分表不可变快照',
  source_hash VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'generated',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_assessment_version CHECK (version_no > 0),
  CONSTRAINT chk_comp_practical_assessment_question_count CHECK (question_count > 0),
  CONSTRAINT chk_comp_practical_assessment_total_score CHECK (total_score > 0),
  CONSTRAINT chk_comp_practical_assessment_status CHECK (status IN ('generated', 'published', 'superseded')),
  UNIQUE KEY uk_comp_practical_assessment_scope_paper (
    tenant_id, stage_id, event_work_type_id, event_work_type_level_id,
    paper_snapshot_id, version_no, deleted
  ),
  KEY idx_comp_practical_assessment_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, status, deleted
  ),
  KEY idx_comp_practical_assessment_paper (tenant_id, paper_snapshot_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操考评表快照';

ALTER TABLE competition_stage_paper_assignment
  ADD COLUMN assessment_sheet_id BIGINT NULL COMMENT '实操考评表快照ID' AFTER paper_snapshot_id,
  ADD KEY idx_comp_stage_paper_assignment_assessment (tenant_id, assessment_sheet_id, deleted);

ALTER TABLE competition_practical_session
  ADD COLUMN assessment_sheet_id BIGINT NULL COMMENT '本场实操考评表快照ID' AFTER paper_snapshot_id,
  ADD KEY idx_comp_practical_session_assessment (tenant_id, assessment_sheet_id, deleted);

ALTER TABLE competition_practical_assignment
  ADD COLUMN assessment_sheet_id BIGINT NULL COMMENT '裁判-选手-考评表任务关联' AFTER referee_member_id,
  ADD KEY idx_comp_practical_assignment_assessment (tenant_id, assessment_sheet_id, status, deleted);

ALTER TABLE competition_practical_score_sheet
  MODIFY COLUMN rule_id BIGINT NULL,
  ADD COLUMN assessment_sheet_id BIGINT NULL COMMENT '评分使用的考评表快照ID' AFTER rule_id,
  ADD KEY idx_comp_practical_score_sheet_assessment (tenant_id, assessment_sheet_id, deleted);

ALTER TABLE competition_referee_profile
  ADD COLUMN user_id BIGINT NULL COMMENT '裁判端登录用户ID' AFTER person_id,
  ADD COLUMN account_username VARCHAR(128) NULL COMMENT '裁判端登录账号' AFTER user_id,
  ADD UNIQUE KEY uk_comp_referee_profile_user (tenant_id, user_id, deleted);

SET @competition_tenant_id := 10159;
SET @seed_operator_id := COALESCE((
  SELECT id FROM sys_user
  WHERE tenant_id = @competition_tenant_id AND username = 'admin' AND deleted = 0
  ORDER BY id LIMIT 1
), 1);

SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_foundation' AND deleted = 0
  LIMIT 1
);
SET @next_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_menu);
SET @practical_console_menu_id := COALESCE((
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_tenant_id AND code = 'competition_practical_exam' AND deleted = 0
  LIMIT 1
), (@next_menu_id := @next_menu_id + 1));

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @practical_console_menu_id, @competition_tenant_id, @competition_root_menu_id,
  'page', 'competition_practical_exam', '实操比赛控制台', '/competition/practical-exam',
  'competition/CompetitionPracticalConsolePage', 'competition:practical-console:view',
  'ControlOutlined', 11, 1, 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id), name = VALUES(name), path = VALUES(path),
  component = VALUES(component), permission_code = VALUES(permission_code),
  icon = VALUES(icon), status = VALUES(status), deleted = 0,
  updated_at = NOW(), updated_by = VALUES(updated_by);

SET @next_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  ((@next_permission_id := @next_permission_id + 1), @competition_tenant_id, @practical_console_menu_id,
    'competition:practical-assessment:view', '查看实操考评表', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  ((@next_permission_id := @next_permission_id + 1), @competition_tenant_id, @practical_console_menu_id,
    'competition:practical-assessment:manage', '生成实操考评表', 'manage', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  ((@next_permission_id := @next_permission_id + 1), @competition_tenant_id, @practical_console_menu_id,
    'competition:practical-console:view', '查看实操比赛控制台', 'view', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  ((@next_permission_id := @next_permission_id + 1), @competition_tenant_id, @practical_console_menu_id,
    'competition:practical-console:manage', '初始化和控制实操比赛', 'manage', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0),
  ((@next_permission_id := @next_permission_id + 1), @competition_tenant_id, NULL,
    'competition:practical-judge:score', '竞赛实操裁判评分', 'score', 'enabled', NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0);

SET @next_role_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role);
SET @practical_referee_role_id := COALESCE((
  SELECT id FROM sys_role
  WHERE tenant_id = @competition_tenant_id AND code = 'COMPETITION_PRACTICAL_REFEREE' AND deleted = 0
  LIMIT 1
), (@next_role_id := @next_role_id + 1));
INSERT INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @practical_referee_role_id, @competition_tenant_id, 'COMPETITION_PRACTICAL_REFEREE',
  '竞赛实操裁判', 'business', 'enabled', '仅用于竞赛实操裁判 WebView 端',
  NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status), deleted = 0,
  updated_at = NOW(), updated_by = VALUES(updated_by);

SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1), @competition_tenant_id, r.id,
       @practical_console_menu_id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0 AND r.id = 2000;

SET @next_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1), @competition_tenant_id,
       r.id, p.id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_role r
JOIN sys_permission p ON p.tenant_id = @competition_tenant_id AND p.deleted = 0
WHERE r.tenant_id = @competition_tenant_id AND r.deleted = 0
  AND r.id = 2000
  AND p.code IN (
    'competition:practical-assessment:view',
    'competition:practical-assessment:manage',
    'competition:practical-console:view',
    'competition:practical-console:manage'
  );

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_permission_id := @next_role_permission_id + 1), @competition_tenant_id,
       @practical_referee_role_id, p.id, NOW(), NOW(), @seed_operator_id, @seed_operator_id, 0
FROM sys_permission p
WHERE p.tenant_id = @competition_tenant_id
  AND p.code = 'competition:practical-judge:score' AND p.deleted = 0;
