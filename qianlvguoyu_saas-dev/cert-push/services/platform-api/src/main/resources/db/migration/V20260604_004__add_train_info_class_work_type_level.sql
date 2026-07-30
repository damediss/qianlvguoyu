-- 对齐 PHP classsave：建班时从 train_info_list 写入工种等级
-- MySQL 8 无 ADD COLUMN IF NOT EXISTS，按 information_schema 幂等补列
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_info_class' AND COLUMN_NAME = 'work_type_level') = 0,
  'ALTER TABLE train_info_class ADD COLUMN work_type_level INT NULL COMMENT ''工种等级 1-5'' AFTER work_type_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
