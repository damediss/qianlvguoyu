-- 对齐 PHP train_base_data：tag / sno / title（共享 PHP 库时列已存在，需幂等）
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'tag') = 0,
  'ALTER TABLE train_base_data ADD COLUMN tag VARCHAR(64) NULL COMMENT ''分类(民族/文化程度等)'' AFTER id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'sno') = 0,
  'ALTER TABLE train_base_data ADD COLUMN sno VARCHAR(32) NULL COMMENT ''编码'' AFTER tag',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'title') = 0,
  'ALTER TABLE train_base_data ADD COLUMN title VARCHAR(255) NULL COMMENT ''显示名称'' AFTER sno',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 纯 Java 库（V20260521）仅有 data_key 时，补 Java 侧字段（与 PHP 列可并存）
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'data_key') = 0,
  'ALTER TABLE train_base_data ADD COLUMN data_key VARCHAR(100) NULL COMMENT ''配置键'' AFTER id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'data_value') = 0,
  'ALTER TABLE train_base_data ADD COLUMN data_value TEXT NULL COMMENT ''配置值'' AFTER data_key',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'data_type') = 0,
  'ALTER TABLE train_base_data ADD COLUMN data_type VARCHAR(50) NULL COMMENT ''数据类型'' AFTER data_value',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_base_data' AND COLUMN_NAME = 'description') = 0,
  'ALTER TABLE train_base_data ADD COLUMN description VARCHAR(500) NULL COMMENT ''描述'' AFTER data_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
