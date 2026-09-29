-- Distinguish manually composed practical scoring schemes from historical simple make plans.
SET @has_plan_purpose := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_make_exam_plan'
    AND COLUMN_NAME = 'plan_purpose'
);
SET @ddl := IF(@has_plan_purpose = 0,
  'ALTER TABLE competition_make_exam_plan ADD COLUMN plan_purpose VARCHAR(32) NOT NULL DEFAULT ''EXAM_PLAN'' COMMENT ''计划用途：常规组卷计划或实操评分方案'' AFTER content_type',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_purpose_check := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_make_exam_plan'
    AND CONSTRAINT_NAME = 'chk_competition_make_plan_purpose'
    AND CONSTRAINT_TYPE = 'CHECK'
);
SET @ddl := IF(@has_purpose_check = 0,
  'ALTER TABLE competition_make_exam_plan ADD CONSTRAINT chk_competition_make_plan_purpose CHECK (plan_purpose IN (''EXAM_PLAN'', ''PRACTICAL_SCORING_SCHEME''))',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_version_index := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_make_exam_plan'
    AND INDEX_NAME = 'uk_competition_make_plan_version'
);
SET @ddl := IF(@has_old_version_index > 0,
  'ALTER TABLE competition_make_exam_plan DROP INDEX uk_competition_make_plan_version',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_purpose_version_index := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_make_exam_plan'
    AND INDEX_NAME = 'uk_competition_make_plan_purpose_version'
);
SET @ddl := IF(@has_purpose_version_index = 0,
  'ALTER TABLE competition_make_exam_plan ADD UNIQUE KEY uk_competition_make_plan_purpose_version (tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, plan_purpose, plan_version, deleted)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_purpose_scope_index := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_make_exam_plan'
    AND INDEX_NAME = 'idx_competition_make_plan_purpose_scope'
);
SET @ddl := IF(@has_purpose_scope_index = 0,
  'ALTER TABLE competition_make_exam_plan ADD KEY idx_competition_make_plan_purpose_scope (tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, plan_purpose, status, deleted)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_menu
SET name = '实操评分方案',
    path = '/competition/make-exam-plans/simple/practical',
    component = 'competition/CompetitionPracticalScoringScheme',
    icon = 'ToolOutlined',
    updated_at = NOW(),
    updated_by = 1
WHERE code = 'competition_simple_practical_make_exam_plan'
  AND tenant_id = 10159
  AND deleted = 0;
