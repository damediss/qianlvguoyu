-- A-1: 对齐 PHP train_sign_up_people_self 遗留字段（幂等追加）

SET @db := DATABASE();

-- helper: add column if missing
-- usage: set @col_sql then PREPARE/EXECUTE

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'age') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN age INT NULL COMMENT ''年龄'' AFTER id_card_number',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'nation') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN nation VARCHAR(50) NULL COMMENT ''民族''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'political') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN political VARCHAR(50) NULL COMMENT ''政治面貌''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'house_type') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN house_type VARCHAR(50) NULL COMMENT ''户口性质''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'address') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN address VARCHAR(500) NULL COMMENT ''地址''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'id_card_front') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN id_card_front VARCHAR(500) NULL COMMENT ''身份证正面''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'id_card_back') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN id_card_back VARCHAR(500) NULL COMMENT ''身份证反面''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'id_card_header') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN id_card_header VARCHAR(500) NULL COMMENT ''一寸证件照''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'id_card_header2') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN id_card_header2 VARCHAR(500) NULL COMMENT ''原证件照''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'school') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN school VARCHAR(255) NULL COMMENT ''学校名称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'major') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN major VARCHAR(255) NULL COMMENT ''专业''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'school_paper_has') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN school_paper_has TINYINT NOT NULL DEFAULT 0 COMMENT ''是否有毕业证''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'school_no') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN school_no VARCHAR(100) NULL COMMENT ''毕业证编号''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'in_school_date') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN in_school_date VARCHAR(20) NULL COMMENT ''入学日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'graduate_date') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN graduate_date VARCHAR(20) NULL COMMENT ''毕业日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'school_paper') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN school_paper VARCHAR(500) NULL COMMENT ''毕业证图片''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_time') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_time VARCHAR(20) NULL COMMENT ''从业时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'title_type') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN title_type VARCHAR(50) NULL COMMENT ''职称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'title_paper') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN title_paper VARCHAR(500) NULL COMMENT ''职称证书''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'company_id') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN company_id BIGINT NULL COMMENT ''公司ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_date_start') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_date_start VARCHAR(20) NULL COMMENT ''入职日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_date_end') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_date_end VARCHAR(20) NULL COMMENT ''结束日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_type_paper_has') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_type_paper_has TINYINT NOT NULL DEFAULT 0 COMMENT ''是否有上一等级证书''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_type_paper') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_type_paper VARCHAR(500) NULL COMMENT ''上一等级证书''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_type_id') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_type_id BIGINT NULL COMMENT ''现工种ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_type_name') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_type_name VARCHAR(255) NULL COMMENT ''现工种名称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'level') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN level INT NULL COMMENT ''现工种等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'paper_no') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN paper_no VARCHAR(100) NULL COMMENT ''现工种证书编号''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'req_work_type_id') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN req_work_type_id BIGINT NULL COMMENT ''预约工种ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'req_work_type_name') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN req_work_type_name VARCHAR(255) NULL COMMENT ''预约工种名称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'req_level') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN req_level INT NULL COMMENT ''预约工种等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'condition') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN `condition` VARCHAR(255) NULL COMMENT ''申报条件''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_exp') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_exp TEXT NULL COMMENT ''工作经历JSON''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'jog') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN jog VARCHAR(255) NULL COMMENT ''岗位''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'contact_first_page') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN contact_first_page VARCHAR(500) NULL COMMENT ''合同第一页''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'contact_expire_page') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN contact_expire_page VARCHAR(500) NULL COMMENT ''合同到期页''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'contact_sign_page') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN contact_sign_page VARCHAR(500) NULL COMMENT ''合同盖章页''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_type_paper_name') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_type_paper_name VARCHAR(255) NULL COMMENT ''上一等级证书工种''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_type_paper_level') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_type_paper_level INT NULL COMMENT ''上一等级证书等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'work_type_paper_no') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN work_type_paper_no VARCHAR(100) NULL COMMENT ''上一等级证书编号''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'company_status') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN company_status INT NOT NULL DEFAULT 0 COMMENT ''企业审核状态''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'has_pay') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN has_pay TINYINT NOT NULL DEFAULT 0 COMMENT ''是否已支付''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_sign_up_people_self' AND COLUMN_NAME = 'user_id') = 0,
  'ALTER TABLE train_sign_up_people_self ADD COLUMN user_id BIGINT NULL COMMENT ''遗留用户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
