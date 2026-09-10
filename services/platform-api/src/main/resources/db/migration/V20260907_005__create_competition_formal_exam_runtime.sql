-- 正式比赛运行域：与赛前试卷配置、理论排考场次相互独立。
-- 赛前选手试卷仍以 competition_stage_paper_assignment 为准，本迁移只冻结运行实例。

CREATE TABLE competition_formal_exam_session (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NOT NULL,
  exam_type VARCHAR(16) NOT NULL DEFAULT 'theory',
  status VARCHAR(16) NOT NULL DEFAULT 'preparing',
  duration_seconds INT NOT NULL,
  planned_start_time DATETIME NULL,
  actual_start_time DATETIME NULL,
  actual_end_time DATETIME NULL,
  candidate_total INT NOT NULL DEFAULT 0,
  submitted_total INT NOT NULL DEFAULT 0,
  occupancy_token BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_exam_session_type CHECK (
    exam_type IN ('theory', 'practical', 'simulation')
  ),
  CONSTRAINT chk_comp_formal_exam_session_status CHECK (
    status IN ('preparing', 'ready', 'running', 'finished', 'cancelled')
  ),
  UNIQUE KEY uk_comp_formal_exam_session_active (
    tenant_id, stage_id, exam_type, occupancy_token, deleted
  ),
  KEY idx_comp_formal_exam_session_event (
    tenant_id, event_id, stage_id, exam_type, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛运行实例';

CREATE TABLE competition_formal_exam_paper_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  source_paper_id BIGINT NOT NULL,
  source_origin_paper_id BIGINT NULL,
  paper_name VARCHAR(255) NULL,
  paper_code VARCHAR(32) NOT NULL,
  total_score DECIMAL(10, 2) NULL,
  question_count INT NOT NULL DEFAULT 0,
  duration_minutes INT NULL,
  snapshot_version INT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_comp_formal_exam_paper_source (
    tenant_id, exam_session_id, source_paper_id, deleted
  ),
  KEY idx_comp_formal_exam_paper_session (
    tenant_id, exam_session_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛试卷快照';

CREATE TABLE competition_formal_exam_question_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  source_question_id BIGINT NULL,
  source_paper_question_id BIGINT NULL,
  question_order INT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  stem LONGTEXT NULL,
  score DECIMAL(10, 2) NULL,
  options_json LONGTEXT NULL,
  correct_answers_json LONGTEXT NULL,
  fill_answer TEXT NULL,
  scoring_rule_code VARCHAR(64) NULL,
  scoring_rule_version INT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_comp_formal_exam_question_order (
    tenant_id, paper_snapshot_id, question_order, deleted
  ),
  KEY idx_comp_formal_exam_question_paper (
    tenant_id, paper_snapshot_id, exam_session_id, deleted
  ),
  KEY idx_comp_formal_exam_question_source (
    tenant_id, source_question_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛题目快照';

CREATE TABLE competition_formal_candidate_exam (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NOT NULL,
  contestant_member_id BIGINT NULL,
  registration_id BIGINT NULL,
  player_id BIGINT NULL,
  candidate_no VARCHAR(64) NULL,
  candidate_name VARCHAR(128) NULL,
  paper_snapshot_id BIGINT NOT NULL,
  source_paper_id BIGINT NOT NULL,
  paper_code VARCHAR(32) NULL,
  paper_name VARCHAR(255) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'waiting_enter',
  enter_time DATETIME NULL,
  start_time DATETIME NULL,
  submit_time DATETIME NULL,
  submit_type VARCHAR(32) NULL,
  answered_count INT NOT NULL DEFAULT 0,
  total_question_count INT NOT NULL DEFAULT 0,
  last_active_time DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_candidate_exam_status CHECK (
    status IN (
      'waiting_enter', 'ready', 'running',
      'submitted', 'auto_submitted', 'forced_submitted'
    )
  ),
  CONSTRAINT chk_comp_formal_candidate_exam_submit CHECK (
    submit_type IS NULL OR submit_type IN (
      'submitted', 'auto_submitted', 'forced_submitted'
    )
  ),
  UNIQUE KEY uk_comp_formal_candidate_exam (
    tenant_id, exam_session_id, contestant_profile_id, deleted
  ),
  KEY idx_comp_formal_candidate_exam_paper (
    tenant_id, exam_session_id, paper_snapshot_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛选手考试运行实例';

SET @competition_formal_exam_menu_id := COALESCE(
  (
    SELECT id FROM sys_menu
    WHERE code = 'competition_theory_exam' AND deleted = 0
    ORDER BY id LIMIT 1
  ),
  (
    SELECT id FROM sys_menu
    WHERE code = 'competition_event' AND deleted = 0
    ORDER BY id LIMIT 1
  )
);

SET @next_competition_formal_exam_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_permission
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@next_competition_formal_exam_permission_id := @next_competition_formal_exam_permission_id + 1,
   COALESCE((SELECT tenant_id FROM sys_menu WHERE id = @competition_formal_exam_menu_id), 1),
   @competition_formal_exam_menu_id, 'competition:exam-session:view',
   '查看正式比赛运行实例', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_formal_exam_permission_id := @next_competition_formal_exam_permission_id + 1,
   COALESCE((SELECT tenant_id FROM sys_menu WHERE id = @competition_formal_exam_menu_id), 1),
   @competition_formal_exam_menu_id, 'competition:exam-session:prepare',
   '初始化正式比赛运行实例', 'prepare', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @next_competition_formal_exam_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_formal_exam_role_permission_id :=
    @next_competition_formal_exam_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.code IN (
  'competition:exam-session:view',
  'competition:exam-session:prepare'
)
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_formal_exam_role_permission_id :=
    @next_competition_formal_exam_role_permission_id + 1,
  target_permission.tenant_id, base_role.role_id, target_permission.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission base_role
JOIN sys_permission base_permission
  ON base_permission.id = base_role.permission_id
  AND base_permission.tenant_id = base_role.tenant_id
  AND base_permission.deleted = 0
JOIN sys_permission target_permission
  ON target_permission.tenant_id = base_permission.tenant_id
  AND target_permission.deleted = 0
  AND target_permission.code = CASE
    WHEN base_permission.code IN (
      'competition:theory-paper:view',
      'competition:stage-paper-assignment:view'
    ) THEN 'competition:exam-session:view'
    ELSE 'competition:exam-session:prepare'
  END
WHERE base_role.deleted = 0
  AND base_permission.code IN (
    'competition:theory-paper:view',
    'competition:theory-paper:manage',
    'competition:theory-paper:confirm',
    'competition:stage-paper-assignment:view',
    'competition:stage-paper-assignment:manage',
    'competition:stage-paper-assignment:confirm'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing_role_permission
    WHERE existing_role_permission.tenant_id = target_permission.tenant_id
      AND existing_role_permission.role_id = base_role.role_id
      AND existing_role_permission.permission_id = target_permission.id
      AND existing_role_permission.deleted = 0
  );
