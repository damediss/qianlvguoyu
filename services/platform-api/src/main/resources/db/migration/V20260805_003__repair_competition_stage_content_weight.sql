-- Repair competition_stage_content.weight skipped by the historical
-- 20260804.006 version conflict.
SET @add_stage_content_weight = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE competition_stage_content ADD COLUMN weight DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER status',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_stage_content'
    AND COLUMN_NAME = 'weight'
);
PREPARE add_stage_content_weight_stmt FROM @add_stage_content_weight;
EXECUTE add_stage_content_weight_stmt;
DEALLOCATE PREPARE add_stage_content_weight_stmt;
