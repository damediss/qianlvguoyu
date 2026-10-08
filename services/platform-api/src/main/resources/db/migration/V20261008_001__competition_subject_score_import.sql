-- Content-weighted stages import practical and simulation scores from Excel.
-- Rows are matched to contestants by ID card, then confirmed into competition_score_source.

CREATE TABLE competition_subject_score_import_batch (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  scope_level_key BIGINT GENERATED ALWAYS AS (
    COALESCE(event_work_type_level_id, -1)
  ) STORED,
  source_type VARCHAR(32) NOT NULL COMMENT 'practical or simulation',
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
  CONSTRAINT chk_comp_subject_import_type CHECK (
    source_type IN ('practical', 'simulation')
  ),
  CONSTRAINT chk_comp_subject_import_status CHECK (
    batch_status IN ('parsing', 'ready', 'invalid', 'confirming', 'completed', 'failed')
  ),
  CONSTRAINT chk_comp_subject_import_counts CHECK (
    total_rows >= 0 AND valid_rows >= 0 AND error_rows >= 0 AND confirmed_rows >= 0
  ),
  UNIQUE KEY uk_comp_subject_import_file (
    tenant_id, event_id, stage_id, event_work_type_id,
    scope_level_key, source_type, file_hash, deleted
  ),
  KEY idx_comp_subject_import_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, source_type, batch_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操/仿真成绩 Excel 导入批次（按身份证匹配）';

CREATE TABLE competition_subject_score_import_row (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  id_card_no VARCHAR(32) NULL,
  contestant_member_id BIGINT NULL,
  raw_score DECIMAL(12,6) NULL,
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
  CONSTRAINT chk_comp_subject_import_row_no CHECK (row_no > 0),
  CONSTRAINT chk_comp_subject_import_row_score CHECK (
    raw_score IS NULL OR raw_score BETWEEN 0 AND 100
  ),
  CONSTRAINT chk_comp_subject_import_row_status CHECK (
    validation_status IN ('pending', 'valid', 'invalid', 'confirmed')
  ),
  UNIQUE KEY uk_comp_subject_import_row (
    tenant_id, batch_id, row_no, deleted
  ),
  KEY idx_comp_subject_import_row_status (
    tenant_id, batch_id, validation_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛实操/仿真成绩 Excel 导入行';
