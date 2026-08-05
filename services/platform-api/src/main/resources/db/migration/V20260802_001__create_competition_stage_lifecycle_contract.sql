-- COMP-STAGE-01B: fixed stage lifecycle and audit facts.
-- This migration is intentionally additive; no applied migration is edited.

ALTER TABLE competition_stage
  ADD COLUMN frozen_at datetime NULL,
  ADD COLUMN ready_at datetime NULL,
  ADD COLUMN closed_at datetime NULL,
  ADD COLUMN result_published_at datetime NULL,
  ADD KEY idx_competition_stage_lifecycle (tenant_id, event_id, status, start_at, end_at, deleted),
  ADD UNIQUE KEY uk_competition_stage_type (tenant_id, event_id, stage_type, deleted);

CREATE TABLE competition_stage_content (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  content_type varchar(32) NOT NULL,
  status varchar(32) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  provider_code varchar(64) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_competition_stage_content_type (tenant_id, stage_id, content_type, deleted),
  KEY idx_competition_stage_content_scope (tenant_id, event_id, stage_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_extension (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  previous_start_at DATETIME NULL,
  previous_end_at DATETIME NULL,
  new_start_at DATETIME NULL,
  new_end_at DATETIME NOT NULL,
  reason varchar(1000) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_competition_stage_extension_scope (tenant_id, event_id, stage_id, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_lifecycle_history (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  from_status varchar(32) NULL,
  to_status varchar(32) NOT NULL,
  action varchar(64) NOT NULL,
  reason varchar(1000) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_competition_stage_history_scope (tenant_id, event_id, stage_id, created_at, deleted),
  KEY idx_competition_stage_history_status (tenant_id, stage_id, to_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
