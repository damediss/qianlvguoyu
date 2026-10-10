-- COMP-SCORE-03B: theory requirement and immutable competition paper snapshots.
-- Version 20260801.002 follows the verified maximum successful Flyway version
-- 20260801.001 queried from the actual database on 2026-07-31.

CREATE TABLE competition_theory_requirement (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  score_rule_id BIGINT NOT NULL,
  template_code VARCHAR(32) NOT NULL,
  duration_minutes INT NOT NULL,
  single_count INT NOT NULL,
  single_score DECIMAL(12,6) NOT NULL,
  multi_count INT NOT NULL,
  multi_score DECIMAL(12,6) NOT NULL,
  judge_count INT NOT NULL,
  judge_score DECIMAL(12,6) NOT NULL,
  total_score DECIMAL(12,6) NOT NULL,
  pass_score DECIMAL(12,6) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_requirement_template CHECK (
    template_code IN ('THEORY_90', 'THEORY_120', 'CUSTOM')
  ),
  CONSTRAINT chk_comp_theory_requirement_duration CHECK (duration_minutes > 0),
  CONSTRAINT chk_comp_theory_requirement_counts CHECK (
    single_count >= 0 AND multi_count >= 0 AND judge_count >= 0
    AND single_count + multi_count + judge_count > 0
  ),
  CONSTRAINT chk_comp_theory_requirement_scores CHECK (
    single_score >= 0 AND multi_score >= 0 AND judge_score >= 0
  ),
  CONSTRAINT chk_comp_theory_requirement_nonzero_score CHECK (
    (single_count = 0 OR single_score > 0)
    AND (multi_count = 0 OR multi_score > 0)
    AND (judge_count = 0 OR judge_score > 0)
  ),
  CONSTRAINT chk_comp_theory_requirement_total CHECK (
    total_score > 0 AND total_score <= 100
    AND total_score =
      single_count * single_score
      + multi_count * multi_score
      + judge_count * judge_score
  ),
  CONSTRAINT chk_comp_theory_requirement_pass CHECK (
    pass_score >= 0 AND pass_score <= total_score
  ),
  UNIQUE KEY uk_comp_theory_requirement_rule (
    tenant_id, score_rule_id, deleted
  ),
  KEY idx_comp_theory_requirement_scope (
    tenant_id, event_id, stage_id, event_work_type_id, score_rule_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛评分规则版本的理论考试要求';

CREATE TABLE competition_theory_paper_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  score_rule_id BIGINT NOT NULL,
  requirement_id BIGINT NOT NULL,
  paper_code VARCHAR(16) NOT NULL COMMENT '当前页面使用A/B，字段预留C/D等扩展',
  version_no INT NOT NULL,
  source_course_id BIGINT NOT NULL,
  source_plan_id BIGINT NOT NULL,
  source_paper_id BIGINT NOT NULL,
  source_title VARCHAR(255) NOT NULL,
  source_hash CHAR(64) NOT NULL,
  duration_minutes INT NOT NULL,
  question_count INT NOT NULL,
  total_score DECIMAL(12,6) NOT NULL,
  pass_score DECIMAL(12,6) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'published',
  active_published_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'published' THEN 1 ELSE NULL END
  ) STORED,
  imported_at DATETIME NOT NULL,
  imported_by BIGINT NOT NULL,
  published_at DATETIME NOT NULL,
  published_by BIGINT NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_snapshot_code CHECK (
    CHAR_LENGTH(paper_code) BETWEEN 1 AND 16
  ),
  CONSTRAINT chk_comp_theory_snapshot_version CHECK (version_no > 0),
  CONSTRAINT chk_comp_theory_snapshot_duration CHECK (duration_minutes > 0),
  CONSTRAINT chk_comp_theory_snapshot_questions CHECK (question_count > 0),
  CONSTRAINT chk_comp_theory_snapshot_scores CHECK (
    total_score > 0 AND total_score <= 100
    AND pass_score >= 0 AND pass_score <= total_score
  ),
  CONSTRAINT chk_comp_theory_snapshot_status CHECK (
    status IN ('published', 'deprecated')
  ),
  UNIQUE KEY uk_comp_theory_snapshot_version (
    tenant_id, event_id, stage_id, event_work_type_id,
    score_rule_id, paper_code, version_no, deleted
  ),
  UNIQUE KEY uk_comp_theory_snapshot_current (
    tenant_id, event_id, stage_id, event_work_type_id,
    score_rule_id, paper_code, active_published_key
  ),
  KEY idx_comp_theory_snapshot_source (
    tenant_id, source_course_id, source_plan_id, source_paper_id, deleted
  ),
  KEY idx_comp_theory_snapshot_requirement (
    tenant_id, requirement_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论试卷不可变发布快照';

CREATE TABLE competition_theory_paper_question (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  paper_snapshot_id BIGINT NOT NULL,
  source_question_id BIGINT NULL,
  sort_order INT NOT NULL,
  question_type VARCHAR(16) NOT NULL,
  content LONGTEXT NOT NULL,
  options_json JSON NOT NULL,
  correct_answers_json JSON NOT NULL,
  score DECIMAL(12,6) NOT NULL,
  resolve_content LONGTEXT NULL,
  difficulty INT NULL,
  scoring_rule_code VARCHAR(64) NOT NULL DEFAULT 'exact_match',
  scoring_rule_version INT NOT NULL DEFAULT 1,
  source_org_id BIGINT NULL,
  source_org_name VARCHAR(255) NULL,
  source_author_id BIGINT NULL,
  source_updated_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_theory_question_sort CHECK (sort_order > 0),
  CONSTRAINT chk_comp_theory_question_type CHECK (
    question_type IN ('single', 'multi', 'judge')
  ),
  CONSTRAINT chk_comp_theory_question_content CHECK (
    CHAR_LENGTH(TRIM(content)) > 0
  ),
  CONSTRAINT chk_comp_theory_question_score CHECK (score > 0),
  CONSTRAINT chk_comp_theory_question_rule_version CHECK (scoring_rule_version > 0),
  CONSTRAINT chk_comp_theory_question_options_json CHECK (
    JSON_TYPE(options_json) = 'ARRAY'
  ),
  CONSTRAINT chk_comp_theory_question_answers_json CHECK (
    JSON_TYPE(correct_answers_json) = 'ARRAY'
  ),
  UNIQUE KEY uk_comp_theory_question_order (
    tenant_id, paper_snapshot_id, sort_order, deleted
  ),
  KEY idx_comp_theory_question_source (
    tenant_id, source_question_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛理论试卷题目、选项与标准答案快照';

SET @competition_event_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = 1
    AND code = 'competition_event'
    AND deleted = 0
  LIMIT 1
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (9978, 1, @competition_event_menu_id, 'competition:theory-paper:view',
   '查看竞赛理论试卷', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (9979, 1, @competition_event_menu_id, 'competition:theory-paper:manage',
   '管理竞赛理论试卷', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @next_competition_theory_role_permission_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_theory_role_permission_id :=
    @next_competition_theory_role_permission_id + 1,
  p.tenant_id,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'competition:theory-paper:view',
    'competition:theory-paper:manage'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
