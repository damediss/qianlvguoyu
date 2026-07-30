-- train_sign_up_self.files：JSON 数组，存储方案附件相对路径（对齐 PHP）

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_self' AND COLUMN_NAME = 'files') = 0,
  'ALTER TABLE train_sign_up_self ADD COLUMN files TEXT NULL COMMENT ''方案附件(JSON数组)''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
