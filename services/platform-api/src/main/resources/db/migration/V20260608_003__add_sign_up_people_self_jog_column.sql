-- 补偿迁移：V20260608_001 已执行后才追加 jog 字段，需单独补齐
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'jog') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN jog VARCHAR(255) NULL COMMENT ''岗位''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
