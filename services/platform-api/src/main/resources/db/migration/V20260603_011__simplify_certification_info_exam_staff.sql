-- 考务信息仅保留姓名与联系电话，其余业务字段下线
-- 空库幂等化（踩坑 #34 修复）：存量库该表是 PHP 老表（带下列索引与字段）；
-- 空库中该表由上游 V20260603_010 建出（已不含这些列/索引），无条件 DROP 必断链。
-- 存量库本版本已应用不会重跑（validate-on-migrate=false）。
SET @db := DATABASE();

-- 1) 索引存在才 DROP
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff'
     AND INDEX_NAME = 'uk_cert_info_exam_staff_cert') > 0,
  'ALTER TABLE certification_info_exam_staff DROP INDEX uk_cert_info_exam_staff_cert',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 列存在才 DROP（逐列条件化；顶层脚本不支持 WHILE，只能展开写）
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'user_id') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN user_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'certificate_no') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN certificate_no',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'work_type_scope') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN work_type_scope',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'work_type_json') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN work_type_json',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'level_scope') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN level_scope',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'level_json') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN level_json',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'valid_until') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN valid_until',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'org_id') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN org_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'org_name') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN org_name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'materials_description') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN materials_description',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'certification_info_exam_staff' AND COLUMN_NAME = 'remark') > 0,
  'ALTER TABLE certification_info_exam_staff DROP COLUMN remark',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
