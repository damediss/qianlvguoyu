-- 正式比赛客观题评分：评分状态与考试运行状态分离。
-- ExamSession.status 仍只表达 preparing/ready/running/finished/cancelled。

ALTER TABLE competition_formal_exam_session
  ADD COLUMN scoring_status VARCHAR(16) NOT NULL DEFAULT 'not_started',
  ADD COLUMN scoring_started_at DATETIME NULL,
  ADD COLUMN scored_time DATETIME NULL,
  ADD COLUMN scoring_error VARCHAR(500) NULL;

ALTER TABLE competition_formal_exam_session
  ADD CONSTRAINT chk_comp_formal_exam_session_scoring_status CHECK (
    scoring_status IN ('not_started', 'scoring', 'completed', 'failed')
  );

ALTER TABLE competition_formal_exam_session
  ADD KEY idx_comp_formal_exam_session_scoring (
    tenant_id, status, scoring_status, deleted
  );

CREATE TABLE competition_formal_candidate_result (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  candidate_exam_id BIGINT NOT NULL,
  result_status VARCHAR(16) NOT NULL,
  full_score DECIMAL(10, 2) NULL,
  total_score DECIMAL(10, 2) NULL,
  question_count INT NULL,
  answered_count INT NULL,
  correct_count INT NULL,
  wrong_count INT NULL,
  unanswered_count INT NULL,
  score_version INT NOT NULL,
  scored_time DATETIME NULL,
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_candidate_result_status CHECK (
    result_status IN ('scored', 'absent')
  ),
  CONSTRAINT chk_comp_formal_candidate_result_version CHECK (score_version > 0),
  UNIQUE KEY uk_comp_formal_candidate_result_exam (
    tenant_id, candidate_exam_id, active_record_key
  ),
  KEY idx_comp_formal_candidate_result_session (
    tenant_id, exam_session_id, result_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛选手成绩结果';

CREATE TABLE competition_formal_candidate_question_score (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  candidate_result_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  candidate_exam_id BIGINT NOT NULL,
  question_snapshot_id BIGINT NOT NULL,
  question_order INT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  answer_version INT NOT NULL,
  is_answered TINYINT NOT NULL,
  is_correct TINYINT NOT NULL,
  max_score DECIMAL(10, 2) NOT NULL,
  awarded_score DECIMAL(10, 2) NOT NULL,
  scoring_rule_version INT NOT NULL,
  scored_time DATETIME NOT NULL,
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_candidate_question_score_answered CHECK (is_answered IN (0, 1)),
  CONSTRAINT chk_comp_formal_candidate_question_score_correct CHECK (is_correct IN (0, 1)),
  CONSTRAINT chk_comp_formal_candidate_question_score_version CHECK (answer_version >= 0),
  CONSTRAINT chk_comp_formal_candidate_question_score_rule CHECK (scoring_rule_version > 0),
  UNIQUE KEY uk_comp_formal_candidate_question_score (
    tenant_id, candidate_exam_id, question_snapshot_id, active_record_key
  ),
  KEY idx_comp_formal_candidate_question_score_result (
    tenant_id, candidate_result_id, question_order, deleted
  ),
  KEY idx_comp_formal_candidate_question_score_session (
    tenant_id, exam_session_id, candidate_exam_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛逐题评分明细';

SET @competition_formal_exam_menu_id := (
  SELECT COALESCE(
    (
      SELECT id FROM sys_menu
      WHERE code = 'competition_formal_exam' AND deleted = 0
      ORDER BY id LIMIT 1
    ),
    (
      SELECT id FROM sys_menu
      WHERE code = 'competition_event' AND deleted = 0
      ORDER BY id LIMIT 1
    )
  )
);

SET @next_competition_formal_exam_score_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_permission
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@next_competition_formal_exam_score_permission_id := @next_competition_formal_exam_score_permission_id + 1,
   COALESCE((SELECT tenant_id FROM sys_menu WHERE id = @competition_formal_exam_menu_id), 1),
   @competition_formal_exam_menu_id, 'competition:exam-session:score',
   '执行正式比赛评分', 'score', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @next_competition_formal_exam_score_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_formal_exam_score_role_permission_id :=
    @next_competition_formal_exam_score_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.code = 'competition:exam-session:score'
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
  @next_competition_formal_exam_score_role_permission_id :=
    @next_competition_formal_exam_score_role_permission_id + 1,
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
  AND target_permission.code = 'competition:exam-session:score'
WHERE base_role.deleted = 0
  AND base_permission.code IN (
    'competition:exam-session:start',
    'competition:exam-session:force-submit',
    'competition:exam-session:prepare'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing_role_permission
    WHERE existing_role_permission.tenant_id = target_permission.tenant_id
      AND existing_role_permission.role_id = base_role.role_id
      AND existing_role_permission.permission_id = target_permission.id
      AND existing_role_permission.deleted = 0
  );
