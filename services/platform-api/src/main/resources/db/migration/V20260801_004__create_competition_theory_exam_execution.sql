-- COMP-SCORE-03D: isolated exam sessions, attempts, idempotent answer saves and scoring.
-- The actual maximum successful Flyway version was 20260801.001 on 2026-07-31;
-- COMP-SCORE-03B/03C already reserve 20260801.002 and 20260801.003 in this source tree.

ALTER TABLE competition_theory_exam_session
  ADD COLUMN execution_status VARCHAR(32) NOT NULL DEFAULT 'scheduled' AFTER status,
  ADD COLUMN opened_at DATETIME NULL AFTER execution_status,
  ADD COLUMN closed_at DATETIME NULL AFTER opened_at,
  ADD CONSTRAINT chk_comp_theory_session_execution_status CHECK (
    execution_status IN ('scheduled', 'open', 'closed')
  ),
  ADD KEY idx_comp_theory_session_execution (
    tenant_id, execution_status, start_at, end_at, status, deleted
  );

CREATE TABLE competition_theory_attempt (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  assignment_id BIGINT NOT NULL,
  schedule_version_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'waiting',
  session_state VARCHAR(32) NOT NULL DEFAULT 'offline',
  last_session_event_at DATETIME NULL,
  started_at DATETIME NULL,
  submitted_at DATETIME NULL,
  submit_type VARCHAR(32) NULL,
  submit_request_id VARCHAR(64) NULL,
  total_score DECIMAL(12,6) NULL,
  score_source_id BIGINT NULL,
  absence_flag TINYINT NOT NULL DEFAULT 0,
  provisional_absent_at DATETIME NULL,
  absence_finalized_at DATETIME NULL,
  restore_count INT NOT NULL DEFAULT 0,
  last_restored_at DATETIME NULL,
  last_saved_at DATETIME NULL,
  forced_by BIGINT NULL,
  force_reason VARCHAR(1000) NULL,
  lock_version INT NOT NULL DEFAULT 0,
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_attempt_status CHECK (
    status IN ('waiting', 'ready', 'in_progress', 'submitting', 'provisional_absent',
      'submitted_manual', 'submitted_auto', 'submitted_forced', 'absent')
  ),
  CONSTRAINT chk_comp_theory_attempt_submit_type CHECK (
    submit_type IS NULL OR submit_type IN ('manual', 'auto', 'forced', 'absent')
  ),
  CONSTRAINT chk_comp_theory_attempt_session_state CHECK (
    session_state IN ('offline', 'active', 'forced_offline', 'closed')
  ),
  CONSTRAINT chk_comp_theory_attempt_score CHECK (
    total_score IS NULL OR (total_score >= 0 AND total_score <= 100)
  ),
  CONSTRAINT chk_comp_theory_attempt_absence CHECK (absence_flag IN (0, 1)),
  CONSTRAINT chk_comp_theory_attempt_restore CHECK (restore_count >= 0),
  CONSTRAINT chk_comp_theory_attempt_lock CHECK (lock_version >= 0),
  UNIQUE KEY uk_comp_theory_attempt_assignment (
    tenant_id, assignment_id, active_record_key
  ),
  UNIQUE KEY uk_comp_theory_attempt_submit_request (
    tenant_id, submit_request_id
  ),
  KEY idx_comp_theory_attempt_player (
    tenant_id, player_id, status, deleted
  ),
  KEY idx_comp_theory_attempt_session_status (
    tenant_id, session_id, status, deleted
  ),
  KEY idx_comp_theory_attempt_timeout (
    tenant_id, status, submitted_at, deleted
  ),
  KEY idx_comp_theory_attempt_score_source (
    tenant_id, score_source_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论考试选手作答会话';

CREATE TABLE competition_theory_answer (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  attempt_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  question_snapshot_id BIGINT NOT NULL,
  answer_json TEXT NULL,
  marked_for_review TINYINT NOT NULL DEFAULT 0,
  save_version INT NOT NULL DEFAULT 1,
  last_request_id VARCHAR(64) NOT NULL,
  last_saved_at DATETIME NOT NULL,
  question_score DECIMAL(12,6) NOT NULL,
  awarded_score DECIMAL(12,6) NULL,
  correct_flag TINYINT NULL,
  scoring_rule_code VARCHAR(64) NOT NULL,
  scoring_rule_version INT NOT NULL,
  scored_at DATETIME NULL,
  active_record_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_answer_marked CHECK (marked_for_review IN (0, 1)),
  CONSTRAINT chk_comp_theory_answer_version CHECK (save_version > 0),
  CONSTRAINT chk_comp_theory_answer_scores CHECK (
    question_score >= 0 AND (awarded_score IS NULL OR
      (awarded_score >= 0 AND awarded_score <= question_score))
  ),
  CONSTRAINT chk_comp_theory_answer_correct CHECK (correct_flag IS NULL OR correct_flag IN (0, 1)),
  UNIQUE KEY uk_comp_theory_answer_question (
    tenant_id, attempt_id, question_snapshot_id, active_record_key
  ),
  KEY idx_comp_theory_answer_attempt (
    tenant_id, attempt_id, save_version, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论考试当前答案与逐题判分结果';

CREATE TABLE competition_theory_answer_save_request (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  attempt_id BIGINT NOT NULL,
  question_snapshot_id BIGINT NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  accepted_version INT NOT NULL,
  answer_hash VARCHAR(64) NOT NULL,
  saved_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_answer_request_version CHECK (accepted_version > 0),
  UNIQUE KEY uk_comp_theory_answer_request (
    tenant_id, attempt_id, request_id, deleted
  ),
  KEY idx_comp_theory_answer_request_question (
    tenant_id, attempt_id, question_snapshot_id, saved_at, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论考试答案保存请求幂等台账';

CREATE TABLE competition_theory_exam_session_token_audit (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  assignment_id BIGINT NOT NULL,
  attempt_id BIGINT NOT NULL,
  token_fingerprint VARCHAR(64) NULL,
  event_type VARCHAR(32) NOT NULL,
  session_status VARCHAR(32) NOT NULL,
  reason VARCHAR(1000) NULL,
  operated_by BIGINT NULL,
  occurred_at DATETIME NOT NULL,
  request_id VARCHAR(64) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_token_event CHECK (
    event_type IN ('login', 'logout', 'force_offline', 'start', 'save',
      'submit', 'auto_submit', 'force_submit', 'provisional_absent',
      'restore_absence', 'absent')
  ),
  CONSTRAINT chk_comp_theory_token_status CHECK (
    session_status IN ('active', 'closed', 'forced_offline', 'submitted', 'absent')
  ),
  KEY idx_comp_theory_token_player (
    tenant_id, player_id, occurred_at, deleted
  ),
  KEY idx_comp_theory_token_attempt (
    tenant_id, attempt_id, occurred_at, deleted
  ),
  KEY idx_comp_theory_token_fingerprint (
    tenant_id, token_fingerprint, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='独立正式考试会话与强制操作审计';

SET @competition_event_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_event' AND deleted = 0
  LIMIT 1
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9984, 1, @competition_event_menu_id, 'competition:theory-monitor:view',
   '查看理论考试监控', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9985, 1, @competition_event_menu_id, 'competition:theory-monitor:force-offline',
   '强制下线理论考试会话', 'force-offline', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9986, 1, @competition_event_menu_id, 'competition:theory-monitor:force-submit',
   '强制交卷理论考试', 'force-submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9987, 1, @competition_event_menu_id, 'competition:theory-monitor:restore-absence',
   '恢复暂定缺考', 'restore-absence', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9988, 1, @competition_event_menu_id, 'competition:theory-result:view',
   '查看理论考试成绩摘要', 'view-result', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = 1;

SET @next_comp_theory_execution_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_comp_theory_execution_role_permission_id :=
    @next_comp_theory_execution_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1 AND p.deleted = 0
  AND p.code IN (
    'competition:theory-monitor:view',
    'competition:theory-monitor:force-offline',
    'competition:theory-monitor:force-submit',
    'competition:theory-monitor:restore-absence',
    'competition:theory-result:view'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id AND rp.role_id = 2000
      AND rp.permission_id = p.id AND rp.deleted = 0
  );
