-- 共享库 demand_train 可能已有 created_by；Flyway 新建库需补齐
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE demand_train ADD COLUMN created_by BIGINT NULL COMMENT ''创建人'' AFTER update_time',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
