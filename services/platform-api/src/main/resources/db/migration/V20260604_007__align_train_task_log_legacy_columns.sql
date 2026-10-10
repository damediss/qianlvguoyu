-- 对齐 PHP 遗留 train_task_log（共享库可能仅有 train_class_id/ck_id/date/addtime/status/times）
-- Java 实体已 ignore tenant 列；本脚本为可选增强，幂等补列

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_task_log' AND COLUMN_NAME = 'deleted') = 0,
  'ALTER TABLE train_task_log ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_task_log' AND COLUMN_NAME = 'tenant_id') = 0,
  'ALTER TABLE train_task_log ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_task_log' AND COLUMN_NAME = 'create_time') = 0,
  'ALTER TABLE train_task_log ADD COLUMN create_time DATETIME NULL COMMENT ''创建时间''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_task_log' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE train_task_log ADD COLUMN update_time DATETIME NULL COMMENT ''更新时间''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
