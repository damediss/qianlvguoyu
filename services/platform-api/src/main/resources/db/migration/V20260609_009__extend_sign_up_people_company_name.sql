-- 对齐 PHP 自主报名人员 company_name 字段
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'company_name') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN company_name VARCHAR(255) NULL COMMENT ''企业名称'' AFTER company_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
