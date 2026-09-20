-- 竞赛理论/实操细目表、考评点绑定及主题组卷规则。
-- 竞赛细目表按赛事范围独立版本化，不复用普通题库细目表。

CREATE TABLE competition_question_blueprint (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  content_type VARCHAR(16) NOT NULL COMMENT 'theory-理论, practical-实操',
  version_no INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  source_file_name VARCHAR(255) NOT NULL,
  source_file_id BIGINT NULL,
  point_count INT NOT NULL DEFAULT 0,
  topic_count INT NOT NULL DEFAULT 0,
  xyz_ratio VARCHAR(32) NULL,
  weight_check TINYINT NOT NULL DEFAULT 0,
  check_report JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/active/archived',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_blueprint_type CHECK (content_type IN ('theory', 'practical')),
  CONSTRAINT chk_competition_blueprint_version CHECK (version_no > 0),
  CONSTRAINT chk_competition_blueprint_status CHECK (status IN ('draft', 'active', 'archived')),
  CONSTRAINT chk_competition_blueprint_counts CHECK (point_count >= 0 AND topic_count >= 0),
  UNIQUE KEY uk_competition_blueprint_version (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, version_no, deleted
  ),
  KEY idx_competition_blueprint_scope (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论/实操细目表版本';

CREATE TABLE competition_question_blueprint_node (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  blueprint_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  node_level INT NOT NULL,
  code VARCHAR(128) NOT NULL,
  full_code VARCHAR(512) NOT NULL,
  name VARCHAR(1000) NOT NULL,
  weight DECIMAL(12,6) NULL,
  select_mode VARCHAR(32) NULL,
  skill_requirement TEXT NULL,
  related_knowledge TEXT NULL,
  importance VARCHAR(8) NULL,
  xyz_stat VARCHAR(64) NULL,
  question_quota INT NULL,
  sort_order INT NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_blueprint_node_level CHECK (node_level BETWEEN 1 AND 4),
  CONSTRAINT chk_competition_blueprint_node_order CHECK (sort_order > 0),
  CONSTRAINT chk_competition_blueprint_node_quota CHECK (question_quota IS NULL OR question_quota > 0),
  UNIQUE KEY uk_competition_blueprint_node_code (tenant_id, blueprint_id, full_code, deleted),
  KEY idx_competition_blueprint_node_parent (tenant_id, blueprint_id, parent_id, sort_order, deleted),
  KEY idx_competition_blueprint_node_leaf (tenant_id, blueprint_id, node_level, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛细目表层级与考评点节点';

CREATE TABLE competition_question_blueprint_binding (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  blueprint_id BIGINT NOT NULL,
  blueprint_node_id BIGINT NOT NULL,
  match_mode VARCHAR(32) NOT NULL COMMENT 'full_code/path_exact/point_name_exact/manual',
  source_batch_id BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_question_binding_match CHECK (
    match_mode IN ('full_code', 'path_exact', 'point_name_exact', 'manual')
  ),
  UNIQUE KEY uk_competition_question_blueprint_binding (
    tenant_id, question_id, blueprint_id, blueprint_node_id, deleted
  ),
  UNIQUE KEY uk_competition_question_blueprint_question (
    tenant_id, question_id, blueprint_id, deleted
  ),
  KEY idx_competition_question_blueprint_binding_node (
    tenant_id, blueprint_id, blueprint_node_id, deleted
  ),
  KEY idx_competition_question_blueprint_binding_question (
    tenant_id, question_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛试题与细目表考评点绑定';

CREATE TABLE competition_make_exam_plan_topic_rule (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  blueprint_node_id BIGINT NOT NULL,
  question_count INT NOT NULL,
  score DECIMAL(12,2) NOT NULL,
  selected TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_plan_topic_rule_count CHECK (question_count >= 0),
  CONSTRAINT chk_competition_plan_topic_rule_score CHECK (score > 0),
  CONSTRAINT chk_competition_plan_topic_rule_selected CHECK (selected IN (0, 1)),
  UNIQUE KEY uk_competition_plan_topic_rule_order (tenant_id, plan_id, sort_order, deleted),
  UNIQUE KEY uk_competition_plan_topic_rule_node (tenant_id, plan_id, blueprint_node_id, deleted),
  KEY idx_competition_plan_topic_rule_plan (tenant_id, plan_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛组卷计划主题配额';

ALTER TABLE competition_question_import_batch
  ADD COLUMN blueprint_id BIGINT NULL AFTER content_type,
  ADD COLUMN selected_sheet VARCHAR(255) NULL AFTER original_filename,
  ADD KEY idx_competition_question_batch_blueprint (tenant_id, blueprint_id, deleted);

ALTER TABLE competition_make_exam_plan
  ADD COLUMN blueprint_id BIGINT NULL AFTER event_work_type_level_id,
  ADD KEY idx_competition_make_plan_blueprint (tenant_id, blueprint_id, deleted);

ALTER TABLE competition_make_exam_paper_question
  ADD COLUMN blueprint_id BIGINT NULL AFTER source_question_id,
  ADD COLUMN blueprint_node_id BIGINT NULL AFTER blueprint_id,
  ADD COLUMN blueprint_full_code VARCHAR(512) NULL AFTER blueprint_node_id,
  ADD COLUMN blueprint_path VARCHAR(2000) NULL AFTER blueprint_full_code,
  ADD COLUMN source_total_score DECIMAL(12,2) NULL AFTER score,
  ADD COLUMN score_scale DECIMAL(18,8) NULL AFTER source_total_score,
  ADD KEY idx_competition_snapshot_question_blueprint (tenant_id, blueprint_id, blueprint_node_id, deleted);

ALTER TABLE competition_make_exam_paper_snapshot
  ADD COLUMN blueprint_id BIGINT NULL AFTER plan_id,
  ADD KEY idx_competition_snapshot_blueprint (tenant_id, blueprint_id, deleted);

-- 新增竞赛试题管理子模块及权限。运行租户和平台管理员按项目约定使用 10159/2000。
-- The live installation uses tenant 10159, while a clean Flyway database
-- starts with the demo tenant (1).  Resolve the competition menu's actual
-- scope first so the migration remains executable in both environments.
SET @competition_scope_tenant_id := COALESCE(
  (
    SELECT tenant_id FROM sys_menu
    WHERE code = 'competition_question_bank' AND deleted = 0
    ORDER BY id LIMIT 1
  ),
  (
    SELECT tenant_id FROM sys_menu
    WHERE code = 'competition_foundation' AND deleted = 0
    ORDER BY id LIMIT 1
  ),
  IF(EXISTS(SELECT 1 FROM sys_tenant WHERE id = 10159 AND deleted = 0), 10159, 1)
);

SET @competition_question_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id AND code = 'competition_question_bank' AND deleted = 0
  ORDER BY id LIMIT 1
);

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609160201, @competition_scope_tenant_id, @competition_question_menu_id, 'page', 'competition_theory_blueprint', '理论细目表',
 '/competition/question-bank/theory-blueprint', 'competition/CompetitionQuestionBlueprint', 'competition:question-blueprint:view',
 'ApartmentOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609160202, @competition_scope_tenant_id, @competition_question_menu_id, 'page', 'competition_theory_question', '理论试题',
 '/competition/question-bank/theory', 'competition/CompetitionQuestionBank', 'competition:question:view',
 'ProfileOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609160203, @competition_scope_tenant_id, @competition_question_menu_id, 'page', 'competition_practical_blueprint', '实操细目表',
 '/competition/question-bank/practical-blueprint', 'competition/CompetitionQuestionBlueprint', 'competition:question-blueprint:view',
 'ApartmentOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609160204, @competition_scope_tenant_id, @competition_question_menu_id, 'page', 'competition_practical_question', '实操试题',
 '/competition/question-bank/practical', 'competition/CompetitionQuestionBank', 'competition:question:view',
 'ToolOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- An upgraded tenant may already have the two question-page rows from
-- V20260904_003.  INSERT IGNORE then keeps those historical IDs, so resolve
-- every menu ID by code before seeding permissions and role-menu links.
SET @competition_theory_blueprint_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id AND code = 'competition_theory_blueprint' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @competition_theory_question_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id AND code = 'competition_theory_question' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @competition_practical_blueprint_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id AND code = 'competition_practical_blueprint' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @competition_practical_question_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id AND code = 'competition_practical_question' AND deleted = 0
  ORDER BY id LIMIT 1
);

-- The question-bank migration already created the two question pages.  The
-- INSERT above is intentionally idempotent, so normalize those existing rows
-- here as well; otherwise the new blueprint pages would not appear in the
-- requested theory/subject/practical/subject order on an upgraded tenant.
UPDATE sys_menu
SET parent_id = @competition_question_menu_id,
    path = '/competition/question-bank/theory',
    component = 'competition/CompetitionQuestionBank',
    permission_code = 'competition:question:view',
    sort_order = 2,
    updated_at = NOW(), updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_theory_question' AND deleted = 0;

UPDATE sys_menu
SET parent_id = @competition_question_menu_id,
    path = '/competition/question-bank/practical',
    component = 'competition/CompetitionQuestionBank',
    permission_code = 'competition:question:view',
    sort_order = 4,
    updated_at = NOW(), updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_practical_question' AND deleted = 0;

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
 (202609160301, @competition_scope_tenant_id, @competition_theory_blueprint_menu_id, 'competition:question-blueprint:view', '查看竞赛细目表', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
 (202609160302, @competition_scope_tenant_id, @competition_theory_blueprint_menu_id, 'competition:question-blueprint:import', '导入竞赛细目表', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
 (202609160303, @competition_scope_tenant_id, @competition_theory_blueprint_menu_id, 'competition:question-blueprint:activate', '启用竞赛细目表', 'activate', 'enabled', NOW(), NOW(), 1, 1, 0),
 (202609160304, @competition_scope_tenant_id, @competition_theory_blueprint_menu_id, 'competition:question-blueprint:archive', '归档竞赛细目表', 'archive', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
 (202609160401, @competition_scope_tenant_id, 2000, @competition_theory_blueprint_menu_id, NOW(), NOW(), 1, 1, 0),
 (202609160402, @competition_scope_tenant_id, 2000, @competition_practical_blueprint_menu_id, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609160501, @competition_scope_tenant_id, 2000, 202609160301, NOW(), NOW(), 1, 1, 0),
(202609160502, @competition_scope_tenant_id, 2000, 202609160302, NOW(), NOW(), 1, 1, 0),
(202609160503, @competition_scope_tenant_id, 2000, 202609160303, NOW(), NOW(), 1, 1, 0),
(202609160504, @competition_scope_tenant_id, 2000, 202609160304, NOW(), NOW(), 1, 1, 0);
