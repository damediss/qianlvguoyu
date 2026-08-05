-- COMP-STAGE-01A: immutable event-level snapshots and nullable contestant bridge.
-- The exam work-type master remains read-only for the competition domain.

SET @event_work_type_level_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_contestant_profile'
    AND column_name = 'event_work_type_level_id'
);
SET @event_work_type_level_column_sql := IF(
  @event_work_type_level_column_exists = 0,
  'ALTER TABLE competition_contestant_profile ADD COLUMN event_work_type_level_id BIGINT NULL',
  'SELECT 1'
);
PREPARE competition_event_work_type_level_column_stmt FROM @event_work_type_level_column_sql;
EXECUTE competition_event_work_type_level_column_stmt;
DEALLOCATE PREPARE competition_event_work_type_level_column_stmt;

SET @event_work_type_level_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_contestant_profile'
    AND index_name = 'idx_competition_contestant_profile_level'
);
SET @event_work_type_level_index_sql := IF(
  @event_work_type_level_index_exists = 0,
  'ALTER TABLE competition_contestant_profile ADD KEY idx_competition_contestant_profile_level (tenant_id, event_work_type_id, event_work_type_level_id, work_type_match_status, deleted)',
  'SELECT 1'
);
PREPARE competition_event_work_type_level_index_stmt FROM @event_work_type_level_index_sql;
EXECUTE competition_event_work_type_level_index_stmt;
DEALLOCATE PREPARE competition_event_work_type_level_index_stmt;

CREATE TABLE competition_event_work_type_level (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  level_code VARCHAR(16) NOT NULL,
  level_name VARCHAR(64) NOT NULL,
  source_level_code VARCHAR(64) NULL,
  source_level_text VARCHAR(255) NULL,
  reconciliation_status VARCHAR(32) NOT NULL DEFAULT 'matched',
  confirmed_at DATETIME NULL,
  confirmed_by BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_competition_event_work_type_level (
    tenant_id, event_work_type_id, level_code, deleted
  ),
  KEY idx_competition_event_work_type_level_event (
    tenant_id, event_id, event_work_type_id, reconciliation_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
