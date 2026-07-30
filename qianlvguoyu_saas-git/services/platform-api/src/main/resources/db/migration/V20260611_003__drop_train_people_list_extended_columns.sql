-- 还原 train_people_list 遗留结构（Flyway 兼容，无 DELIMITER/存储过程）
-- 幂等删除 V20260611_001 及 SaaS 追加的扩展列

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'sign_up_people_id') > 0,
  'ALTER TABLE train_people_list DROP COLUMN sign_up_people_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'age') > 0,
  'ALTER TABLE train_people_list DROP COLUMN age',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'req_work_type_id') > 0,
  'ALTER TABLE train_people_list DROP COLUMN req_work_type_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'req_work_type_name') > 0,
  'ALTER TABLE train_people_list DROP COLUMN req_work_type_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'req_level') > 0,
  'ALTER TABLE train_people_list DROP COLUMN req_level',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'job') > 0,
  'ALTER TABLE train_people_list DROP COLUMN job',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_id') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_type_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_name') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_type_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'level') > 0,
  'ALTER TABLE train_people_list DROP COLUMN `level`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_id') > 0,
  'ALTER TABLE train_people_list DROP COLUMN company_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_name') > 0,
  'ALTER TABLE train_people_list DROP COLUMN company_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_code') > 0,
  'ALTER TABLE train_people_list DROP COLUMN company_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'zip_code') > 0,
  'ALTER TABLE train_people_list DROP COLUMN zip_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_address') > 0,
  'ALTER TABLE train_people_list DROP COLUMN company_address',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'industrial_park') > 0,
  'ALTER TABLE train_people_list DROP COLUMN industrial_park',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'comments') > 0,
  'ALTER TABLE train_people_list DROP COLUMN comments',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_status') > 0,
  'ALTER TABLE train_people_list DROP COLUMN company_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_front') > 0,
  'ALTER TABLE train_people_list DROP COLUMN id_card_front',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_back') > 0,
  'ALTER TABLE train_people_list DROP COLUMN id_card_back',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_header') > 0,
  'ALTER TABLE train_people_list DROP COLUMN id_card_header',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_header2') > 0,
  'ALTER TABLE train_people_list DROP COLUMN id_card_header2',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_has') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_type_paper_has',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_type_paper',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_no') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_type_paper_no',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_level') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_type_paper_level',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_name') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_type_paper_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_date_start') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_date_start',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_date_end') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_date_end',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_time') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'title_type') > 0,
  'ALTER TABLE train_people_list DROP COLUMN title_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'title_level') > 0,
  'ALTER TABLE train_people_list DROP COLUMN title_level',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'title_paper') > 0,
  'ALTER TABLE train_people_list DROP COLUMN title_paper',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'has_pay') > 0,
  'ALTER TABLE train_people_list DROP COLUMN has_pay',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school') > 0,
  'ALTER TABLE train_people_list DROP COLUMN school',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'major') > 0,
  'ALTER TABLE train_people_list DROP COLUMN major',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'in_school_date') > 0,
  'ALTER TABLE train_people_list DROP COLUMN in_school_date',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'graduate_date') > 0,
  'ALTER TABLE train_people_list DROP COLUMN graduate_date',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school_paper_has') > 0,
  'ALTER TABLE train_people_list DROP COLUMN school_paper_has',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school_paper') > 0,
  'ALTER TABLE train_people_list DROP COLUMN school_paper',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'nation') > 0,
  'ALTER TABLE train_people_list DROP COLUMN nation',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'address') > 0,
  'ALTER TABLE train_people_list DROP COLUMN address',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'education') > 0,
  'ALTER TABLE train_people_list DROP COLUMN education',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'condition') > 0,
  'ALTER TABLE train_people_list DROP COLUMN `condition`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school_no') > 0,
  'ALTER TABLE train_people_list DROP COLUMN school_no',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'paper_no') > 0,
  'ALTER TABLE train_people_list DROP COLUMN paper_no',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_exp') > 0,
  'ALTER TABLE train_people_list DROP COLUMN work_exp',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'contact_first_page') > 0,
  'ALTER TABLE train_people_list DROP COLUMN contact_first_page',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'contact_expire_page') > 0,
  'ALTER TABLE train_people_list DROP COLUMN contact_expire_page',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'contact_sign_page') > 0,
  'ALTER TABLE train_people_list DROP COLUMN contact_sign_page',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'social_security') > 0,
  'ALTER TABLE train_people_list DROP COLUMN social_security',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'house_type') > 0,
  'ALTER TABLE train_people_list DROP COLUMN house_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'political') > 0,
  'ALTER TABLE train_people_list DROP COLUMN political',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'task') > 0,
  'ALTER TABLE train_people_list DROP COLUMN task',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'status') > 0,
  'ALTER TABLE train_people_list DROP COLUMN status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
