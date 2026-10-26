-- Production counterpart of migration-test/V20261022_001__competition_plantvr_simulation_integration.sql.
-- The test-side file is already applied and must retain its original Flyway checksum.
-- PlantVR simulation score integration for competition stage-content scoring.
-- External identity values are matched in memory; only masked identity data is persisted.

CREATE TABLE competition_simulation_provider_binding (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  scope_level_key BIGINT GENERATED ALWAYS AS (
    COALESCE(event_work_type_level_id, -1)
  ) STORED,
  provider_code VARCHAR(64) NOT NULL DEFAULT 'plantvr',
  external_host_tenant_id VARCHAR(64) NOT NULL,
  external_session_id VARCHAR(128) NOT NULL,
  module_filter VARCHAR(128) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'active',
  last_sync_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_sim_provider_status CHECK (status IN ('active', 'disabled')),
  UNIQUE KEY uk_comp_sim_provider_scope (
    tenant_id, event_id, stage_id, event_work_type_id, scope_level_key,
    provider_code, deleted
  ),
  KEY idx_comp_sim_provider_external (
    tenant_id, provider_code, external_host_tenant_id, external_session_id, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛仿真科目与外部成绩场次绑定';

CREATE TABLE competition_simulation_sync_batch (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  binding_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  batch_status VARCHAR(32) NOT NULL DEFAULT 'syncing',
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
  CONSTRAINT chk_comp_sim_sync_status CHECK (
    batch_status IN ('syncing', 'ready', 'ready_with_errors', 'confirming', 'completed', 'failed')
  ),
  CONSTRAINT chk_comp_sim_sync_counts CHECK (
    total_rows >= 0 AND valid_rows >= 0 AND error_rows >= 0 AND confirmed_rows >= 0
  ),
  KEY idx_comp_sim_sync_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, created_at, deleted
  ),
  KEY idx_comp_sim_sync_binding (tenant_id, binding_id, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛仿真外部成绩同步批次';

CREATE TABLE competition_simulation_sync_row (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  external_participant_id VARCHAR(128) NULL,
  external_session_id VARCHAR(128) NOT NULL,
  player_name VARCHAR(128) NULL,
  id_card_masked VARCHAR(32) NULL,
  contestant_member_id BIGINT NULL,
  raw_score DECIMAL(12,6) NULL,
  module_title VARCHAR(255) NULL,
  app_display_name VARCHAR(255) NULL,
  duration_seconds INT NULL,
  achieved_at DATETIME NULL,
  validation_status VARCHAR(24) NOT NULL DEFAULT 'pending',
  error_message VARCHAR(1000) NULL,
  score_source_id BIGINT NULL,
  detail_json LONGTEXT NULL,
  payload_hash CHAR(64) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_sim_sync_row_no CHECK (row_no > 0),
  CONSTRAINT chk_comp_sim_sync_row_score CHECK (
    raw_score IS NULL OR raw_score BETWEEN 0 AND 100
  ),
  CONSTRAINT chk_comp_sim_sync_row_status CHECK (
    validation_status IN ('pending', 'valid', 'invalid', 'conflict', 'confirmed')
  ),
  UNIQUE KEY uk_comp_sim_sync_row_no (tenant_id, batch_id, row_no, deleted),
  UNIQUE KEY uk_comp_sim_sync_external (
    tenant_id, batch_id, external_session_id, deleted
  ),
  KEY idx_comp_sim_sync_row_status (
    tenant_id, batch_id, validation_status, deleted
  ),
  KEY idx_comp_sim_sync_row_member (
    tenant_id, contestant_member_id, achieved_at, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛仿真外部成绩同步行与评分明细快照';
