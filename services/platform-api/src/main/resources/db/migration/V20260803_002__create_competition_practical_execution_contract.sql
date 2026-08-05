-- COMP-STAGE-01 / 01E: practical rule snapshots, sessions, judge groups and score sheets.

CREATE TABLE competition_practical_rule (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  rule_version INT NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  total_score DECIMAL(12,2) NOT NULL DEFAULT 100.00,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  published_at DATETIME NULL,
  published_by BIGINT NULL,
  active_current_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'published' THEN 1 ELSE NULL END
  ) STORED,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_rule_version CHECK (rule_version > 0),
  CONSTRAINT chk_comp_practical_rule_total CHECK (total_score > 0 AND total_score <= 100),
  CONSTRAINT chk_comp_practical_rule_status CHECK (status IN ('draft', 'published', 'withdrawn')),
  UNIQUE KEY uk_comp_practical_rule_version (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, rule_version, deleted
  ),
  UNIQUE KEY uk_comp_practical_rule_current (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, active_current_key
  ),
  KEY idx_comp_practical_rule_scope (
    tenant_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_practical_rule_item (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  rule_id BIGINT NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL,
  max_score DECIMAL(12,2) NOT NULL,
  deduction_allowed TINYINT NOT NULL DEFAULT 1,
  criteria_payload JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_rule_item_order CHECK (sort_order > 0),
  CONSTRAINT chk_comp_practical_rule_item_score CHECK (max_score > 0),
  CONSTRAINT chk_comp_practical_rule_item_deduction CHECK (deduction_allowed IN (0, 1)),
  CONSTRAINT chk_comp_practical_rule_item_status CHECK (status IN ('enabled', 'disabled')),
  UNIQUE KEY uk_comp_practical_rule_item_code (tenant_id, rule_id, item_code, deleted),
  UNIQUE KEY uk_comp_practical_rule_item_order (tenant_id, rule_id, sort_order, deleted),
  KEY idx_comp_practical_rule_item_rule (tenant_id, rule_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_practical_session (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  session_code VARCHAR(64) NOT NULL,
  session_name VARCHAR(128) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  capacity INT NULL,
  resource_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_session_time CHECK (end_at > start_at),
  CONSTRAINT chk_comp_practical_session_capacity CHECK (capacity IS NULL OR capacity > 0),
  CONSTRAINT chk_comp_practical_session_status CHECK (
    status IN ('draft', 'published', 'in_progress', 'closed', 'cancelled')
  ),
  UNIQUE KEY uk_comp_practical_session_code (tenant_id, plan_id, session_code, deleted),
  KEY idx_comp_practical_session_scope (
    tenant_id, stage_id, event_work_type_id, event_work_type_level_id, start_at, status, deleted
  ),
  KEY idx_comp_practical_session_resource (tenant_id, resource_id, start_at, end_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_practical_judge_group (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  group_no INT NOT NULL,
  group_name VARCHAR(128) NOT NULL,
  official_count INT NOT NULL DEFAULT 3,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_judge_group_no CHECK (group_no > 0),
  CONSTRAINT chk_comp_practical_judge_group_official_count CHECK (official_count = 3),
  CONSTRAINT chk_comp_practical_judge_group_status CHECK (
    status IN ('draft', 'published', 'closed')
  ),
  UNIQUE KEY uk_comp_practical_judge_group_no (tenant_id, session_id, group_no, deleted),
  KEY idx_comp_practical_judge_group_plan (tenant_id, plan_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_practical_judge_member (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  judge_group_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  referee_member_id BIGINT NOT NULL,
  member_role VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_judge_member_role CHECK (member_role IN ('official', 'standby')),
  CONSTRAINT chk_comp_practical_judge_member_status CHECK (status IN ('active', 'replaced', 'withdrawn')),
  UNIQUE KEY uk_comp_practical_judge_member (tenant_id, judge_group_id, referee_member_id, deleted),
  KEY idx_comp_practical_judge_member_session (tenant_id, session_id, member_role, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_practical_assignment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  judge_group_id BIGINT NOT NULL,
  roster_item_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  referee_member_id BIGINT NOT NULL,
  workstation_code VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'assigned',
  assigned_at DATETIME NOT NULL,
  ended_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_assignment_status CHECK (
    status IN ('assigned', 'in_progress', 'completed', 'withdrawn')
  ),
  UNIQUE KEY uk_comp_practical_assignment_referee (
    tenant_id, session_id, contestant_member_id, referee_member_id, deleted
  ),
  KEY idx_comp_practical_assignment_contestant (
    tenant_id, plan_id, contestant_member_id, session_id, status, deleted
  ),
  KEY idx_comp_practical_assignment_group (tenant_id, judge_group_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_practical_score_sheet (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  assignment_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  rule_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  contestant_member_id BIGINT NOT NULL,
  referee_member_id BIGINT NOT NULL,
  revision_no INT NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  total_score DECIMAL(12,2) NULL,
  unlock_reason VARCHAR(1000) NULL,
  opened_at DATETIME NOT NULL,
  submitted_at DATETIME NULL,
  locked_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_score_sheet_revision CHECK (revision_no > 0),
  CONSTRAINT chk_comp_practical_score_sheet_status CHECK (
    status IN ('draft', 'submitted_locked', 'unlocked_for_rescore', 'voided')
  ),
  CONSTRAINT chk_comp_practical_score_sheet_score CHECK (
    total_score IS NULL OR total_score BETWEEN 0 AND 100
  ),
  CONSTRAINT chk_comp_practical_score_sheet_locked CHECK (
    status <> 'submitted_locked'
    OR (total_score IS NOT NULL AND submitted_at IS NOT NULL AND locked_at IS NOT NULL)
  ),
  UNIQUE KEY uk_comp_practical_score_sheet_revision (
    tenant_id, assignment_id, revision_no, deleted
  ),
  UNIQUE KEY uk_comp_practical_score_sheet_current (
    tenant_id, assignment_id, status, deleted
  ),
  KEY idx_comp_practical_score_sheet_scope (
    tenant_id, stage_id, event_work_type_id, event_work_type_level_id,
    contestant_member_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_practical_score_entry (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  score_sheet_id BIGINT NOT NULL,
  rule_item_id BIGINT NOT NULL,
  score DECIMAL(12,2) NOT NULL DEFAULT 0,
  deduction DECIMAL(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(1000) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_practical_score_entry_score CHECK (score >= 0),
  CONSTRAINT chk_comp_practical_score_entry_deduction CHECK (deduction >= 0),
  UNIQUE KEY uk_comp_practical_score_entry_item (tenant_id, score_sheet_id, rule_item_id, deleted),
  KEY idx_comp_practical_score_entry_sheet (tenant_id, score_sheet_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
