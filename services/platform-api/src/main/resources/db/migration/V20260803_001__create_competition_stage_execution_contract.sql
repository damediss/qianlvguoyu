-- COMP-STAGE-01C: generic execution plans, roster snapshots, resource refs and assignments.
-- Existing theory schedule tables remain readable; this contract is the shared plan layer.

CREATE TABLE competition_stage_execution_plan (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  plan_version INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  resource_version VARCHAR(128) NULL,
  preflight_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  roster_snapshot_id BIGINT NULL,
  active_plan_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'published' THEN 1 ELSE NULL END
  ) STORED,
  published_at DATETIME NULL,
  published_by BIGINT NULL,
  withdrawn_at DATETIME NULL,
  withdrawn_by BIGINT NULL,
  withdraw_reason VARCHAR(1000) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_plan_status CHECK (status IN ('draft', 'published', 'withdrawn')),
  CONSTRAINT chk_comp_stage_plan_preflight CHECK (preflight_status IN ('pending', 'blocked', 'passed')),
  CONSTRAINT chk_comp_stage_plan_version CHECK (plan_version > 0),
  UNIQUE KEY uk_comp_stage_plan_version (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, plan_version, deleted
  ),
  UNIQUE KEY uk_comp_stage_plan_published (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, active_plan_key
  ),
  KEY idx_comp_stage_plan_scope (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_plan_resource (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  content_type VARCHAR(32) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NULL,
  resource_code VARCHAR(128) NULL,
  resource_version VARCHAR(128) NULL,
  start_at DATETIME NULL,
  end_at DATETIME NULL,
  resource_payload JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_plan_resource_status CHECK (status IN ('enabled', 'disabled')),
  CONSTRAINT chk_comp_stage_plan_resource_time CHECK (end_at IS NULL OR start_at IS NULL OR end_at > start_at),
  UNIQUE KEY uk_comp_stage_plan_resource (
    tenant_id, plan_id, content_type, resource_type, resource_id, resource_code, deleted
  ),
  KEY idx_comp_stage_plan_resource_scope (
    tenant_id, event_id, stage_id, content_type, resource_type, start_at, end_at, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_resource_booking (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'reserved',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_booking_time CHECK (end_at > start_at),
  CONSTRAINT chk_comp_stage_booking_status CHECK (status IN ('reserved', 'released')),
  KEY idx_comp_stage_booking_conflict (
    tenant_id, resource_type, resource_id, start_at, end_at, status, deleted
  ),
  KEY idx_comp_stage_booking_plan (tenant_id, plan_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_roster_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  snapshot_version INT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  active_snapshot_key TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status = 'published' THEN 1 ELSE NULL END
  ) STORED,
  generated_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  published_by BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_roster_status CHECK (status IN ('draft', 'published', 'superseded')),
  CONSTRAINT chk_comp_stage_roster_source CHECK (source_type IN ('eligibility', 'promotion', 'manual')),
  CONSTRAINT chk_comp_stage_roster_version CHECK (snapshot_version > 0),
  UNIQUE KEY uk_comp_stage_roster_version (
    tenant_id, plan_id, snapshot_version, deleted
  ),
  UNIQUE KEY uk_comp_stage_roster_current (
    tenant_id, plan_id, active_snapshot_key
  ),
  KEY idx_comp_stage_roster_scope (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_roster_item (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  snapshot_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  contestant_member_id BIGINT NOT NULL,
  contestant_profile_id BIGINT NULL,
  registration_id BIGINT NULL,
  inclusion_status VARCHAR(32) NOT NULL DEFAULT 'included',
  eligibility_status VARCHAR(32) NOT NULL DEFAULT 'eligible',
  reason VARCHAR(1000) NULL,
  source_promotion_item_id BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_roster_item_status CHECK (inclusion_status IN ('included', 'removed')),
  CONSTRAINT chk_comp_stage_roster_item_eligibility CHECK (eligibility_status IN ('eligible', 'absent', 'blocked')),
  UNIQUE KEY uk_comp_stage_roster_item_member (tenant_id, snapshot_id, contestant_member_id, deleted),
  KEY idx_comp_stage_roster_item_scope (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, inclusion_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_roster_adjustment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  snapshot_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  before_status VARCHAR(32) NULL,
  after_status VARCHAR(32) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  operated_by BIGINT NOT NULL,
  operated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_comp_stage_roster_adjustment_scope (tenant_id, snapshot_id, contestant_member_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_assignment (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  roster_snapshot_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  contestant_member_id BIGINT NOT NULL,
  session_id BIGINT NULL,
  assignment_type VARCHAR(32) NOT NULL,
  resource_id BIGINT NULL,
  assignment_status VARCHAR(32) NOT NULL DEFAULT 'assigned',
  source_type VARCHAR(32) NOT NULL DEFAULT 'provider',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_assignment_status CHECK (assignment_status IN ('assigned', 'withdrawn')),
  CONSTRAINT chk_comp_stage_assignment_type CHECK (assignment_type IN ('theory', 'simulation', 'practical')),
  UNIQUE KEY uk_comp_stage_assignment_scope (
    tenant_id, plan_id, contestant_member_id, assignment_type, deleted
  ),
  KEY idx_comp_stage_assignment_session (tenant_id, session_id, assignment_status, deleted),
  KEY idx_comp_stage_assignment_scope (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_import_batch (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NOT NULL,
  source_file_id BIGINT NULL,
  operation VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'received',
  total_count INT NOT NULL DEFAULT 0,
  valid_count INT NOT NULL DEFAULT 0,
  invalid_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(2000) NULL,
  confirmed_by BIGINT NULL,
  confirmed_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_import_operation CHECK (operation IN ('roster', 'assignment')),
  CONSTRAINT chk_comp_stage_import_status CHECK (status IN ('received', 'validating', 'validation_failed', 'pending_confirm', 'confirmed', 'processing', 'completed', 'failed')),
  UNIQUE KEY uk_comp_stage_import_batch_no (tenant_id, plan_id, batch_no, deleted),
  KEY idx_comp_stage_import_status (tenant_id, plan_id, status, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_import_item (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  contestant_member_id BIGINT NULL,
  content_type VARCHAR(32) NULL,
  session_id BIGINT NULL,
  validation_status VARCHAR(32) NOT NULL DEFAULT 'valid',
  error_message VARCHAR(1000) NULL,
  raw_payload JSON NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_import_item_status CHECK (validation_status IN ('valid', 'invalid')),
  UNIQUE KEY uk_comp_stage_import_item_row (tenant_id, batch_id, row_no, deleted),
  KEY idx_comp_stage_import_item_batch (tenant_id, batch_id, validation_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
