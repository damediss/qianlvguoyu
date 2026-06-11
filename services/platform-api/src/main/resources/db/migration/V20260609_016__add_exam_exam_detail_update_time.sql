-- 为 exam_exam_detail 增加 update_time 列（幂等）
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'exam_exam_detail' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE exam_exam_detail ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE exam_exam_detail
  SET update_time = create_time
  WHERE update_time IS NULL AND create_time IS NOT NULL;
