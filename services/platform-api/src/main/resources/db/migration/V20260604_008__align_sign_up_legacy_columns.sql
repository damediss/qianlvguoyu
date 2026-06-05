-- 对齐 PHP 遗留报名相关表（共享库可能无 tenant_id/create_time/update_time）

SET @db := DATABASE();

-- train_sign_up_self
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_self' AND COLUMN_NAME = 'deleted') = 0,
  'ALTER TABLE train_sign_up_self ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_self' AND COLUMN_NAME = 'tenant_id') = 0,
  'ALTER TABLE train_sign_up_self ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_self' AND COLUMN_NAME = 'create_time') = 0,
  'ALTER TABLE train_sign_up_self ADD COLUMN create_time DATETIME NULL COMMENT ''创建时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_self' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE train_sign_up_self ADD COLUMN update_time DATETIME NULL COMMENT ''更新时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- train_sign_up_people_self
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'deleted') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'tenant_id') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'create_time') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN create_time DATETIME NULL COMMENT ''创建时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN update_time DATETIME NULL COMMENT ''更新时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
