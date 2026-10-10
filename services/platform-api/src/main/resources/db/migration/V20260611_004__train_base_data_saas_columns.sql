-- 共享 PHP 库 train_base_data 可能缺少 SaaS 列，幂等补齐
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'tenant_id') = 0,
  'ALTER TABLE train_base_data ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER description',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'create_time') = 0,
  'ALTER TABLE train_base_data ADD COLUMN create_time DATETIME NULL COMMENT ''创建时间'' AFTER tenant_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE train_base_data ADD COLUMN update_time DATETIME NULL COMMENT ''更新时间'' AFTER create_time',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'deleted') = 0,
  'ALTER TABLE train_base_data ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除'' AFTER update_time',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
