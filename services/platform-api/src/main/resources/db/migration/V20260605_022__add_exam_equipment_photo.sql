-- 空库幂等化（踩坑 #34 修复）：原文件无条件 ADD 两列——存量库若 004 已补过会 Duplicate column，
-- 空库则依赖本处真正落列。列存在才跳过，语义对两条路径都一致。
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_exam_equipment'
     AND COLUMN_NAME = 'equipment_photo_file_id') = 0,
  'ALTER TABLE certification_exam_equipment ADD COLUMN equipment_photo_file_id BIGINT NULL COMMENT ''equipment photo file id'' AFTER equipment_status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_exam_equipment'
     AND COLUMN_NAME = 'equipment_photo_file_name') = 0,
  'ALTER TABLE certification_exam_equipment ADD COLUMN equipment_photo_file_name VARCHAR(300) NULL COMMENT ''equipment photo file name'' AFTER equipment_photo_file_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
