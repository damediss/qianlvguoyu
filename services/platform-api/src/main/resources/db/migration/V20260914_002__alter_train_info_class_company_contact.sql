-- 班级单位联系人：供默认模板「工作年限证明」com_user / com_phone 使用
-- 幂等：双库（gjjddb2026 / gjjddbpro）可重复执行

SET @db := DATABASE();

SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'train_info_class'
        AND COLUMN_NAME = 'company_contact_name'
    ),
    'SELECT 1',
    'ALTER TABLE train_info_class ADD COLUMN company_contact_name VARCHAR(64) NULL COMMENT ''单位联系人'' AFTER kcbiao_file'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'train_info_class'
        AND COLUMN_NAME = 'company_contact_phone'
    ),
    'SELECT 1',
    'ALTER TABLE train_info_class ADD COLUMN company_contact_phone VARCHAR(32) NULL COMMENT ''单位联系方式'' AFTER company_contact_name'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
