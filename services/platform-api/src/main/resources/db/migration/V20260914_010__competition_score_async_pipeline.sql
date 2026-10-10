-- COMP-SCORE: reliable asynchronous formal-exam scoring and published ranking versions.
-- Version selected after checking both gjjddb2026 (max 20260912.001) and
-- gjjddbpro (max 20260914.006) on 2026-09-14.

ALTER TABLE competition_outbox_event
  ADD COLUMN event_channel VARCHAR(32) NULL AFTER tenant_id,
  ADD COLUMN routing_key VARCHAR(128) NULL AFTER event_type,
  ADD COLUMN event_key VARCHAR(191) NULL AFTER routing_key;

UPDATE competition_outbox_event
SET event_channel = 'notice',
    routing_key = 'competition.notice.published.v1',
    event_key = CONCAT('legacy:', id)
WHERE event_channel IS NULL;

ALTER TABLE competition_outbox_event
  MODIFY COLUMN event_channel VARCHAR(32) NOT NULL,
  MODIFY COLUMN routing_key VARCHAR(128) NOT NULL,
  MODIFY COLUMN event_key VARCHAR(191) NOT NULL,
  ADD CONSTRAINT chk_competition_outbox_channel CHECK (
    event_channel IN ('notice', 'scoring', 'score_pipeline')
  ),
  ADD UNIQUE KEY uk_competition_outbox_event_key (
    tenant_id, event_key, deleted
  ),
  ADD KEY idx_competition_outbox_channel_dispatch (
    event_channel, status, next_attempt_at, created_at, deleted
  );

ALTER TABLE competition_formal_exam_session
  DROP CHECK chk_comp_formal_exam_session_scoring_status,
  ADD CONSTRAINT chk_comp_formal_exam_session_scoring_status CHECK (
    scoring_status IN ('not_started', 'queued', 'scoring', 'completed', 'failed')
  );

ALTER TABLE competition_formal_exam_paper_snapshot
  ADD COLUMN score_rule_id BIGINT NULL AFTER source_origin_paper_id,
  ADD KEY idx_comp_formal_paper_score_rule (
    tenant_id, exam_session_id, score_rule_id, deleted
  );

UPDATE competition_formal_exam_paper_snapshot fp
JOIN competition_theory_paper_snapshot tp ON tp.id = fp.source_paper_id
SET fp.score_rule_id = tp.score_rule_id
WHERE fp.score_rule_id IS NULL
  AND fp.deleted = 0
  AND tp.deleted = 0;

ALTER TABLE competition_formal_candidate_result
  DROP INDEX uk_comp_formal_candidate_result_exam,
  ADD COLUMN revision_no INT NOT NULL DEFAULT 1 AFTER candidate_exam_id,
  ADD COLUMN current_flag TINYINT NOT NULL DEFAULT 1 AFTER score_version,
  ADD COLUMN correction_reason VARCHAR(500) NULL AFTER current_flag,
  ADD COLUMN active_current_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND current_flag = 1 THEN 1 ELSE NULL END
  ) STORED,
  ADD CONSTRAINT chk_comp_formal_candidate_result_revision CHECK (revision_no > 0),
  ADD CONSTRAINT chk_comp_formal_candidate_result_current CHECK (current_flag IN (0, 1)),
  ADD UNIQUE KEY uk_comp_formal_candidate_result_revision (
    tenant_id, candidate_exam_id, revision_no, deleted
  ),
  ADD UNIQUE KEY uk_comp_formal_candidate_result_current (
    tenant_id, candidate_exam_id, active_current_key
  );

ALTER TABLE competition_formal_candidate_question_score
  DROP INDEX uk_comp_formal_candidate_question_score,
  ADD UNIQUE KEY uk_comp_formal_question_score_result (
    tenant_id, candidate_result_id, question_snapshot_id, deleted
  );

CREATE TABLE competition_formal_scoring_task (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  candidate_exam_id BIGINT NOT NULL,
  result_revision_no INT NOT NULL,
  task_status VARCHAR(24) NOT NULL DEFAULT 'pending',
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME NOT NULL,
  lease_owner VARCHAR(128) NULL,
  lease_until DATETIME NULL,
  last_error VARCHAR(1000) NULL,
  requested_reason VARCHAR(500) NULL,
  requested_by BIGINT NULL,
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_scoring_task_revision CHECK (result_revision_no > 0),
  CONSTRAINT chk_comp_formal_scoring_task_attempt CHECK (attempt_count >= 0),
  CONSTRAINT chk_comp_formal_scoring_task_status CHECK (
    task_status IN ('pending', 'processing', 'retry', 'completed', 'failed')
  ),
  UNIQUE KEY uk_comp_formal_scoring_task_revision (
    tenant_id, candidate_exam_id, result_revision_no, deleted
  ),
  KEY idx_comp_formal_scoring_task_session (
    tenant_id, exam_session_id, task_status, deleted
  ),
  KEY idx_comp_formal_scoring_task_recovery (
    task_status, next_attempt_at, lease_until, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛选手级异步判分任务';

ALTER TABLE competition_total_score_snapshot
  ADD COLUMN scope_level_key BIGINT GENERATED ALWAYS AS (
    COALESCE(event_work_type_level_id, -1)
  ) STORED AFTER event_work_type_level_id,
  ADD COLUMN input_fingerprint CHAR(64) NULL AFTER practical_source_id,
  DROP INDEX uk_comp_total_score_version,
  DROP INDEX uk_comp_total_score_current,
  ADD UNIQUE KEY uk_comp_total_score_version (
    tenant_id, event_id, stage_id, event_work_type_id, scope_level_key,
    contestant_member_id, score_rule_id, calculation_version, deleted
  ),
  ADD UNIQUE KEY uk_comp_total_score_current (
    tenant_id, event_id, stage_id, event_work_type_id, scope_level_key,
    contestant_member_id, score_rule_id, active_current_key
  ),
  ADD UNIQUE KEY uk_comp_total_score_input (
    tenant_id, event_id, stage_id, event_work_type_id,
    scope_level_key, contestant_member_id, score_rule_id,
    input_fingerprint, deleted
  );

ALTER TABLE competition_score_source
  ADD COLUMN scope_level_key BIGINT GENERATED ALWAYS AS (
    COALESCE(event_work_type_level_id, -1)
  ) STORED AFTER event_work_type_level_id,
  DROP INDEX uk_comp_score_source_revision,
  DROP INDEX uk_comp_score_source_current,
  ADD UNIQUE KEY uk_comp_score_source_revision (
    tenant_id, event_id, stage_id, event_work_type_id, scope_level_key,
    contestant_member_id, source_type, revision_no, deleted
  ),
  ADD UNIQUE KEY uk_comp_score_source_current (
    tenant_id, event_id, stage_id, event_work_type_id, scope_level_key,
    contestant_member_id, source_type, active_current_key
  );

ALTER TABLE competition_ranking_snapshot
  DROP CHECK chk_comp_ranking_snapshot_status,
  ADD COLUMN scope_level_key BIGINT GENERATED ALWAYS AS (
    COALESCE(event_work_type_level_id, -1)
  ) STORED AFTER event_work_type_level_id,
  ADD COLUMN input_fingerprint CHAR(64) NULL AFTER snapshot_version,
  ADD COLUMN confirmed_at DATETIME NULL AFTER generation_reason,
  ADD COLUMN confirmed_by BIGINT NULL AFTER confirmed_at,
  ADD COLUMN published_at DATETIME NULL AFTER confirmed_by,
  ADD COLUMN published_by BIGINT NULL AFTER published_at,
  ADD COLUMN superseded_at DATETIME NULL AFTER published_by,
  ADD COLUMN superseded_by BIGINT NULL AFTER superseded_at,
  ADD COLUMN supersedes_snapshot_id BIGINT NULL AFTER superseded_by,
  ADD COLUMN correction_reason VARCHAR(500) NULL AFTER supersedes_snapshot_id,
  ADD COLUMN active_published_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'published' THEN 1 ELSE NULL END
  ) STORED,
  ADD CONSTRAINT chk_comp_ranking_snapshot_status CHECK (
    status IN ('draft', 'confirmed', 'published', 'superseded')
  ),
  DROP INDEX uk_comp_ranking_snapshot_version,
  ADD UNIQUE KEY uk_comp_ranking_snapshot_version (
    tenant_id, event_id, stage_id, event_work_type_id, scope_level_key,
    score_rule_id, snapshot_version, deleted
  ),
  ADD UNIQUE KEY uk_comp_ranking_snapshot_input (
    tenant_id, event_id, stage_id, event_work_type_id,
    scope_level_key, score_rule_id, input_fingerprint, deleted
  ),
  ADD UNIQUE KEY uk_comp_ranking_snapshot_published (
    tenant_id, event_id, stage_id, event_work_type_id,
    scope_level_key, score_rule_id, active_published_key
  );

CREATE TABLE competition_simulation_score_import_batch (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  scope_level_key BIGINT GENERATED ALWAYS AS (
    COALESCE(event_work_type_level_id, -1)
  ) STORED,
  score_rule_id BIGINT NOT NULL,
  source_file_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_hash CHAR(64) NOT NULL,
  batch_status VARCHAR(24) NOT NULL DEFAULT 'parsing',
  total_rows INT NOT NULL DEFAULT 0,
  valid_rows INT NOT NULL DEFAULT 0,
  error_rows INT NOT NULL DEFAULT 0,
  confirmed_rows INT NOT NULL DEFAULT 0,
  correction_reason VARCHAR(500) NULL,
  error_message VARCHAR(1000) NULL,
  confirmed_at DATETIME NULL,
  confirmed_by BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_sim_import_status CHECK (
    batch_status IN ('parsing', 'ready', 'invalid', 'confirming', 'completed', 'failed')
  ),
  CONSTRAINT chk_comp_sim_import_counts CHECK (
    total_rows >= 0 AND valid_rows >= 0 AND error_rows >= 0 AND confirmed_rows >= 0
  ),
  UNIQUE KEY uk_comp_sim_import_file (
    tenant_id, event_id, stage_id, event_work_type_id,
    scope_level_key, score_rule_id, file_hash, deleted
  ),
  KEY idx_comp_sim_import_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, batch_status, deleted
  ),
  KEY idx_comp_sim_import_source_file (tenant_id, source_file_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛仿真成绩Excel导入批次';

CREATE TABLE competition_simulation_score_import_row (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  candidate_no VARCHAR(64) NULL,
  contestant_member_id BIGINT NULL,
  simulation_score DECIMAL(12,6) NULL,
  external_reference VARCHAR(255) NULL,
  achieved_at DATETIME NULL,
  remark VARCHAR(500) NULL,
  validation_status VARCHAR(16) NOT NULL DEFAULT 'pending',
  error_message VARCHAR(1000) NULL,
  score_source_id BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_sim_import_row_no CHECK (row_no > 0),
  CONSTRAINT chk_comp_sim_import_row_score CHECK (
    simulation_score IS NULL OR simulation_score BETWEEN 0 AND 100
  ),
  CONSTRAINT chk_comp_sim_import_row_status CHECK (
    validation_status IN ('pending', 'valid', 'invalid', 'confirmed')
  ),
  UNIQUE KEY uk_comp_sim_import_row (
    tenant_id, batch_id, row_no, deleted
  ),
  KEY idx_comp_sim_import_row_status (
    tenant_id, batch_id, validation_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛仿真成绩Excel导入行';

SET @competition_score_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 10159 AND code = 'competition_score_ranking' AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @next_comp_score_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@next_comp_score_permission_id := @next_comp_score_permission_id + 1, 10159,
   @competition_score_menu_id, 'competition:score:import', '导入竞赛仿真成绩', 'import',
   'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_comp_score_permission_id := @next_comp_score_permission_id + 1, 10159,
   @competition_score_menu_id, 'competition:score:review', '复核竞赛成绩排名', 'review',
   'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_comp_score_permission_id := @next_comp_score_permission_id + 1, 10159,
   @competition_score_menu_id, 'competition:ranking:publish', '发布竞赛排名', 'publish',
   'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_comp_score_permission_id := @next_comp_score_permission_id + 1, 10159,
   @competition_score_menu_id, 'competition:score-task:retry', '重试竞赛判分任务', 'retry',
   'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = 1;

SET @next_comp_score_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_comp_score_role_permission_id := @next_comp_score_role_permission_id + 1,
  10159, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 10159
  AND p.code IN (
    'competition:score:import', 'competition:score:review',
    'competition:ranking:publish', 'competition:score-task:retry'
  )
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 10159 AND rp.role_id = 2000
      AND rp.permission_id = p.id AND rp.deleted = 0
  );
