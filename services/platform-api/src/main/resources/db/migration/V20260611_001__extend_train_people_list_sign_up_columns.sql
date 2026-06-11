-- 对齐 train_sign_up_people_self 遗留字段到 train_people_list（幂等追加）

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'sign_up_people_id') = 0,
  'ALTER TABLE train_people_list ADD COLUMN sign_up_people_id BIGINT NULL COMMENT ''来源报名人员ID'' AFTER platform_user_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'age') = 0,
  'ALTER TABLE train_people_list ADD COLUMN age INT NULL COMMENT ''年龄'' AFTER id_card',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'req_work_type_id') = 0,
  'ALTER TABLE train_people_list ADD COLUMN req_work_type_id BIGINT NULL COMMENT ''申报工种ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'req_work_type_name') = 0,
  'ALTER TABLE train_people_list ADD COLUMN req_work_type_name VARCHAR(255) NULL COMMENT ''申报工种''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'req_level') = 0,
  'ALTER TABLE train_people_list ADD COLUMN req_level INT NULL COMMENT ''申报工种等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'job') = 0,
  'ALTER TABLE train_people_list ADD COLUMN job VARCHAR(255) NULL COMMENT ''职业''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_id') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_type_id BIGINT NULL COMMENT ''工种ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_name') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_type_name VARCHAR(255) NULL COMMENT ''工种''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'level') = 0,
  'ALTER TABLE train_people_list ADD COLUMN level VARCHAR(20) NULL DEFAULT ''0'' COMMENT ''工种等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_id') = 0,
  'ALTER TABLE train_people_list ADD COLUMN company_id BIGINT NULL COMMENT ''企业ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_name') = 0,
  'ALTER TABLE train_people_list ADD COLUMN company_name VARCHAR(255) NULL COMMENT ''企业名称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_code') = 0,
  'ALTER TABLE train_people_list ADD COLUMN company_code VARCHAR(255) NULL COMMENT ''企业代码''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'zip_code') = 0,
  'ALTER TABLE train_people_list ADD COLUMN zip_code VARCHAR(255) NULL COMMENT ''邮编''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_address') = 0,
  'ALTER TABLE train_people_list ADD COLUMN company_address VARCHAR(255) NULL COMMENT ''企业地址''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'industrial_park') = 0,
  'ALTER TABLE train_people_list ADD COLUMN industrial_park VARCHAR(255) NULL COMMENT ''工业园''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'comments') = 0,
  'ALTER TABLE train_people_list ADD COLUMN comments VARCHAR(255) NULL COMMENT ''备注''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'company_status') = 0,
  'ALTER TABLE train_people_list ADD COLUMN company_status INT NOT NULL DEFAULT 0 COMMENT ''企业审核状态''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_front') = 0,
  'ALTER TABLE train_people_list ADD COLUMN id_card_front VARCHAR(500) NULL COMMENT ''身份证正面''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_back') = 0,
  'ALTER TABLE train_people_list ADD COLUMN id_card_back VARCHAR(500) NULL COMMENT ''身份证反面''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_header') = 0,
  'ALTER TABLE train_people_list ADD COLUMN id_card_header VARCHAR(500) NULL COMMENT ''一寸照''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'id_card_header2') = 0,
  'ALTER TABLE train_people_list ADD COLUMN id_card_header2 VARCHAR(500) NULL COMMENT ''两寸照''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_has') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_type_paper_has TINYINT NOT NULL DEFAULT 0 COMMENT ''是否有上一等级证书''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_type_paper VARCHAR(500) NULL COMMENT ''上一等级证书图片''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_no') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_type_paper_no VARCHAR(50) NULL COMMENT ''上一等级证书编号''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_level') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_type_paper_level INT NOT NULL DEFAULT 0 COMMENT ''上一等级证书等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_type_paper_name') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_type_paper_name VARCHAR(50) NULL COMMENT ''上一等级证书工种''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_date_start') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_date_start VARCHAR(20) NULL COMMENT ''本公司入职日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_date_end') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_date_end VARCHAR(20) NULL COMMENT ''本公司在职结束日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_time') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_time VARCHAR(20) NULL COMMENT ''从业日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'title_type') = 0,
  'ALTER TABLE train_people_list ADD COLUMN title_type VARCHAR(255) NULL COMMENT ''职称''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'title_level') = 0,
  'ALTER TABLE train_people_list ADD COLUMN title_level VARCHAR(255) NULL COMMENT ''职称等级''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'title_paper') = 0,
  'ALTER TABLE train_people_list ADD COLUMN title_paper VARCHAR(255) NULL COMMENT ''职称证''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'has_pay') = 0,
  'ALTER TABLE train_people_list ADD COLUMN has_pay TINYINT NULL DEFAULT 0 COMMENT ''是否缴费''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school') = 0,
  'ALTER TABLE train_people_list ADD COLUMN school VARCHAR(255) NULL COMMENT ''学校''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'major') = 0,
  'ALTER TABLE train_people_list ADD COLUMN major VARCHAR(100) NULL COMMENT ''专业''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'in_school_date') = 0,
  'ALTER TABLE train_people_list ADD COLUMN in_school_date VARCHAR(20) NULL COMMENT ''入学日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'graduate_date') = 0,
  'ALTER TABLE train_people_list ADD COLUMN graduate_date VARCHAR(20) NULL COMMENT ''毕业日期''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school_paper_has') = 0,
  'ALTER TABLE train_people_list ADD COLUMN school_paper_has INT NOT NULL DEFAULT 0 COMMENT ''是否有毕业证''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school_paper') = 0,
  'ALTER TABLE train_people_list ADD COLUMN school_paper VARCHAR(500) NULL COMMENT ''毕业证图片''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'nation') = 0,
  'ALTER TABLE train_people_list ADD COLUMN nation VARCHAR(50) NULL COMMENT ''民族''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'address') = 0,
  'ALTER TABLE train_people_list ADD COLUMN address VARCHAR(500) NULL COMMENT ''地址''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'education') = 0,
  'ALTER TABLE train_people_list ADD COLUMN education VARCHAR(255) NULL COMMENT ''学历''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'condition') = 0,
  'ALTER TABLE train_people_list ADD COLUMN `condition` VARCHAR(500) NULL COMMENT ''申报条件''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'school_no') = 0,
  'ALTER TABLE train_people_list ADD COLUMN school_no VARCHAR(200) NULL COMMENT ''毕业证编号''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'paper_no') = 0,
  'ALTER TABLE train_people_list ADD COLUMN paper_no VARCHAR(200) NULL COMMENT ''证书编号''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'work_exp') = 0,
  'ALTER TABLE train_people_list ADD COLUMN work_exp TEXT NULL COMMENT ''工作经历''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'contact_first_page') = 0,
  'ALTER TABLE train_people_list ADD COLUMN contact_first_page VARCHAR(500) NULL COMMENT ''劳动合同第一页''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'contact_expire_page') = 0,
  'ALTER TABLE train_people_list ADD COLUMN contact_expire_page VARCHAR(500) NULL COMMENT ''劳动合同期限页''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'contact_sign_page') = 0,
  'ALTER TABLE train_people_list ADD COLUMN contact_sign_page VARCHAR(500) NULL COMMENT ''劳动合同盖章页''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'social_security') = 0,
  'ALTER TABLE train_people_list ADD COLUMN social_security VARCHAR(500) NULL COMMENT ''社保''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'house_type') = 0,
  'ALTER TABLE train_people_list ADD COLUMN house_type VARCHAR(200) NULL COMMENT ''户口性质''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'political') = 0,
  'ALTER TABLE train_people_list ADD COLUMN political VARCHAR(255) NULL COMMENT ''政治面貌''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_people_list' AND COLUMN_NAME = 'task') = 0,
  'ALTER TABLE train_people_list ADD COLUMN task INT NOT NULL DEFAULT 1 COMMENT ''材料导出任务标记''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
