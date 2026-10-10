-- Keep the legacy work-type-level confirmation columns available for callers
-- that do not yet provide a level, while new UI calls use the level scope below.

SET @work_type_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type ADD COLUMN stage_plan_status VARCHAR(32) NOT NULL DEFAULT ''draft'' AFTER sort_order',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type'
    AND column_name = 'stage_plan_status'
);
PREPARE work_type_stage_plan_stmt FROM @work_type_stage_plan_sql;
EXECUTE work_type_stage_plan_stmt;
DEALLOCATE PREPARE work_type_stage_plan_stmt;

SET @work_type_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type ADD COLUMN stage_plan_confirmed_by BIGINT NULL AFTER stage_plan_status',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type'
    AND column_name = 'stage_plan_confirmed_by'
);
PREPARE work_type_stage_plan_stmt FROM @work_type_stage_plan_sql;
EXECUTE work_type_stage_plan_stmt;
DEALLOCATE PREPARE work_type_stage_plan_stmt;

SET @work_type_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type ADD COLUMN stage_plan_confirmed_at DATETIME NULL AFTER stage_plan_confirmed_by',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type'
    AND column_name = 'stage_plan_confirmed_at'
);
PREPARE work_type_stage_plan_stmt FROM @work_type_stage_plan_sql;
EXECUTE work_type_stage_plan_stmt;
DEALLOCATE PREPARE work_type_stage_plan_stmt;

SET @work_type_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type ADD CONSTRAINT chk_competition_event_work_type_stage_plan_status CHECK (stage_plan_status IN (''draft'', ''confirmed''))',
    'SELECT 1'
  )
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type'
    AND constraint_name = 'chk_competition_event_work_type_stage_plan_status'
);
PREPARE work_type_stage_plan_stmt FROM @work_type_stage_plan_sql;
EXECUTE work_type_stage_plan_stmt;
DEALLOCATE PREPARE work_type_stage_plan_stmt;

-- A stage plan belongs to one event work type and one level.  Keep the
-- reconciliation confirmation columns above separate from this lifecycle.

SET @level_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_level ADD COLUMN stage_plan_status VARCHAR(32) NOT NULL DEFAULT ''draft'' AFTER reconciliation_status',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_level'
    AND column_name = 'stage_plan_status'
);
PREPARE level_stage_plan_stmt FROM @level_stage_plan_sql;
EXECUTE level_stage_plan_stmt;
DEALLOCATE PREPARE level_stage_plan_stmt;

SET @level_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_level ADD COLUMN stage_plan_confirmed_by BIGINT NULL AFTER stage_plan_status',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_level'
    AND column_name = 'stage_plan_confirmed_by'
);
PREPARE level_stage_plan_stmt FROM @level_stage_plan_sql;
EXECUTE level_stage_plan_stmt;
DEALLOCATE PREPARE level_stage_plan_stmt;

SET @level_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_level ADD COLUMN stage_plan_confirmed_at DATETIME NULL AFTER stage_plan_confirmed_by',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_level'
    AND column_name = 'stage_plan_confirmed_at'
);
PREPARE level_stage_plan_stmt FROM @level_stage_plan_sql;
EXECUTE level_stage_plan_stmt;
DEALLOCATE PREPARE level_stage_plan_stmt;

SET @level_stage_plan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_level ADD CONSTRAINT chk_competition_event_work_type_level_stage_plan_status CHECK (stage_plan_status IN (''draft'', ''confirmed''))',
    'SELECT 1'
  )
  FROM information_schema.table_constraints
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_level'
    AND constraint_name = 'chk_competition_event_work_type_level_stage_plan_status'
);
PREPARE level_stage_plan_stmt FROM @level_stage_plan_sql;
EXECUTE level_stage_plan_stmt;
DEALLOCATE PREPARE level_stage_plan_stmt;
