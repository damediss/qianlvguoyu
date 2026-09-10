-- Each competition work type and level owns an independent stage canvas.
-- Existing NULL/NULL rows remain the legacy event-level canvas.

-- MySQL 8.4 does not support ALTER TABLE ... ADD COLUMN IF NOT EXISTS.
-- Use metadata-guarded dynamic DDL so a retry after MySQL's implicit DDL commit
-- is safe (the first ALTER may have succeeded before a later statement failed).
SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_stage ADD COLUMN event_work_type_id BIGINT NULL AFTER event_id',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_stage'
    AND column_name = 'event_work_type_id'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_stage ADD COLUMN event_work_type_level_code VARCHAR(16) NULL AFTER event_work_type_id',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_stage'
    AND column_name = 'event_work_type_level_code'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE competition_stage DROP INDEX uk_competition_stage_type',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_stage'
    AND index_name = 'uk_competition_stage_type'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_stage ADD UNIQUE KEY uk_competition_stage_scope_type (tenant_id, event_id, event_work_type_id, event_work_type_level_code, stage_type, deleted)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_stage'
    AND index_name = 'uk_competition_stage_scope_type'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_stage ADD KEY idx_competition_stage_scope (tenant_id, event_id, event_work_type_id, event_work_type_level_code, sort_order, deleted)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_stage'
    AND index_name = 'idx_competition_stage_scope'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_stage ADD COLUMN event_work_type_level_code VARCHAR(16) NULL AFTER event_work_type_id',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_stage'
    AND column_name = 'event_work_type_level_code'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_stage ADD COLUMN promotion_quota INT NULL AFTER promotion_percentage',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_stage'
    AND column_name = 'promotion_quota'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE competition_event_work_type_stage DROP INDEX uk_comp_event_work_type_stage',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_stage'
    AND index_name = 'uk_comp_event_work_type_stage'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_stage ADD UNIQUE KEY uk_comp_event_work_type_stage_scope (tenant_id, event_work_type_id, event_work_type_level_code, stage_id, deleted)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_stage'
    AND index_name = 'uk_comp_event_work_type_stage_scope'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

SET @scope_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_event_work_type_stage ADD KEY idx_comp_event_work_type_stage_level_scope (tenant_id, event_id, event_work_type_id, event_work_type_level_code, enabled, sort_order, deleted)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_event_work_type_stage'
    AND index_name = 'idx_comp_event_work_type_stage_level_scope'
);
PREPARE scope_stmt FROM @scope_sql;
EXECUTE scope_stmt;
DEALLOCATE PREPARE scope_stmt;

-- Preserve the already saved legacy canvas only for work types that had an enabled
-- event-work-type stage configuration. A newly added work type without such a
-- configuration therefore starts with an empty canvas instead of inheriting another
-- work type's stages. Support both historical event-level rows (NULL work type)
-- and an earlier work-type-level implementation (work type set, level NULL).
-- If levels were not configured in the legacy data, copy to all standard levels
-- because the old UI exposed all five levels on the shared canvas.
DROP TEMPORARY TABLE IF EXISTS competition_stage_scope_backfill;
CREATE TEMPORARY TABLE competition_stage_scope_backfill (
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_code VARCHAR(16) NOT NULL,
  legacy_stage_id BIGINT NOT NULL,
  new_stage_id BIGINT NOT NULL,
  PRIMARY KEY (
    tenant_id, event_id, event_work_type_id, event_work_type_level_code, legacy_stage_id
  )
);

INSERT INTO competition_stage_scope_backfill (
  tenant_id, event_id, event_work_type_id, event_work_type_level_code, legacy_stage_id, new_stage_id
)
SELECT s.tenant_id, s.event_id,
       COALESCE(s.event_work_type_id, wts.event_work_type_id), level_scope.level_code,
       s.id, COALESCE(scoped_stage.id, UUID_SHORT())
FROM competition_stage s
LEFT JOIN competition_event_work_type_stage wts
  ON wts.tenant_id = s.tenant_id
 AND wts.event_id = s.event_id
 AND wts.stage_id = s.id
 AND s.event_work_type_id IS NULL
 AND wts.enabled = 1
 AND wts.deleted = 0
 AND wts.event_work_type_level_code IS NULL
JOIN (
  SELECT configured_level.tenant_id, configured_level.event_id,
         configured_level.event_work_type_id, configured_level.level_code
  FROM competition_event_work_type_level configured_level
  WHERE configured_level.deleted = 0
  UNION ALL
  SELECT configured_work_type.tenant_id, configured_work_type.event_id,
         configured_work_type.event_work_type_id, levels.level_code
  FROM (
    SELECT DISTINCT tenant_id, event_id, event_work_type_id
    FROM competition_event_work_type_stage
    WHERE enabled = 1 AND deleted = 0
    UNION
    SELECT DISTINCT tenant_id, event_id, event_work_type_id
    FROM competition_stage
    WHERE event_work_type_id IS NOT NULL
      AND event_work_type_level_code IS NULL
      AND deleted = 0
  ) configured_work_type
  CROSS JOIN (
    SELECT '1' AS level_code
    UNION ALL SELECT '2'
    UNION ALL SELECT '3'
    UNION ALL SELECT '4'
    UNION ALL SELECT '5'
  ) levels
  WHERE NOT EXISTS (
    SELECT 1
    FROM competition_event_work_type_level configured_level
    WHERE configured_level.tenant_id = configured_work_type.tenant_id
      AND configured_level.event_id = configured_work_type.event_id
      AND configured_level.event_work_type_id = configured_work_type.event_work_type_id
      AND configured_level.deleted = 0
  )
) level_scope
  ON level_scope.tenant_id = s.tenant_id
 AND level_scope.event_id = s.event_id
 AND level_scope.event_work_type_id = COALESCE(s.event_work_type_id, wts.event_work_type_id)
LEFT JOIN competition_stage scoped_stage
  ON scoped_stage.tenant_id = s.tenant_id
 AND scoped_stage.event_id = s.event_id
 AND scoped_stage.event_work_type_id = COALESCE(s.event_work_type_id, wts.event_work_type_id)
 AND scoped_stage.event_work_type_level_code = level_scope.level_code
 AND scoped_stage.stage_type = s.stage_type
 AND scoped_stage.deleted = 0
WHERE s.event_work_type_level_code IS NULL
  AND s.deleted = 0
  AND (wts.event_work_type_id IS NOT NULL OR s.event_work_type_id IS NOT NULL);

INSERT INTO competition_stage (
  id, tenant_id, event_id, event_work_type_id, event_work_type_level_code,
  name, stage_type, sort_order, start_at, end_at, status, description,
  frozen_at, ready_at, closed_at, result_published_at,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT b.new_stage_id, s.tenant_id, s.event_id, b.event_work_type_id,
       b.event_work_type_level_code, s.name, s.stage_type, s.sort_order,
       s.start_at, s.end_at, s.status, s.description, s.frozen_at, s.ready_at,
       s.closed_at, s.result_published_at, s.created_by, s.updated_by,
       s.created_at, s.updated_at, s.deleted
FROM competition_stage_scope_backfill b
JOIN competition_stage s ON s.id = b.legacy_stage_id
LEFT JOIN competition_stage scoped_stage
  ON scoped_stage.tenant_id = b.tenant_id
 AND scoped_stage.event_id = b.event_id
 AND scoped_stage.event_work_type_id = b.event_work_type_id
 AND scoped_stage.event_work_type_level_code = b.event_work_type_level_code
 AND scoped_stage.stage_type = s.stage_type
 AND scoped_stage.deleted = 0
WHERE scoped_stage.id IS NULL;

INSERT INTO competition_stage_content (
  id, tenant_id, event_id, stage_id, content_type, status, weight, sort_order,
  provider_code, created_by, updated_by, created_at, updated_at, deleted
)
SELECT UUID_SHORT(), b.tenant_id, b.event_id, b.new_stage_id, c.content_type,
       c.status, c.weight, c.sort_order, c.provider_code, c.created_by,
       c.updated_by, c.created_at, c.updated_at, c.deleted
FROM competition_stage_scope_backfill b
JOIN competition_stage_content c
  ON c.tenant_id = b.tenant_id
 AND c.event_id = b.event_id
 AND c.stage_id = b.legacy_stage_id
 AND c.deleted = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM competition_stage_content existing_content
  WHERE existing_content.tenant_id = b.tenant_id
    AND existing_content.event_id = b.event_id
    AND existing_content.stage_id = b.new_stage_id
    AND existing_content.content_type = c.content_type
    AND existing_content.deleted = 0
);

INSERT INTO competition_event_work_type_stage (
  id, tenant_id, event_id, event_work_type_id, event_work_type_level_code,
  stage_id, enabled, sort_order, promotion_rule_type, promotion_percentage,
  promotion_quota, legacy_rule, created_by, updated_by, created_at, updated_at, deleted
)
SELECT UUID_SHORT(), wts.tenant_id, wts.event_id, wts.event_work_type_id,
       b.event_work_type_level_code, b.new_stage_id, wts.enabled, wts.sort_order,
       wts.promotion_rule_type, wts.promotion_percentage, wts.promotion_quota,
       wts.legacy_rule, wts.created_by, wts.updated_by, wts.created_at,
       wts.updated_at, wts.deleted
FROM competition_stage_scope_backfill b
JOIN competition_event_work_type_stage wts
  ON wts.tenant_id = b.tenant_id
 AND wts.event_id = b.event_id
 AND wts.event_work_type_id = b.event_work_type_id
 AND wts.stage_id = b.legacy_stage_id
 AND wts.event_work_type_level_code IS NULL
 AND wts.enabled = 1
 AND wts.deleted = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM competition_event_work_type_stage existing_wts
  WHERE existing_wts.tenant_id = b.tenant_id
    AND existing_wts.event_id = b.event_id
    AND existing_wts.event_work_type_id = b.event_work_type_id
    AND existing_wts.event_work_type_level_code = b.event_work_type_level_code
    AND existing_wts.stage_id = b.new_stage_id
    AND existing_wts.deleted = 0
);

DROP TEMPORARY TABLE competition_stage_scope_backfill;
