-- 共享库 demand_train / demand_employee 的 id 可能来自旧 PHP 表，无 AUTO_INCREMENT，导致 Java 插入失败

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db
     AND TABLE_NAME = 'demand_train'
     AND COLUMN_NAME = 'id'
     AND EXTRA NOT LIKE '%auto_increment%') > 0,
  'ALTER TABLE demand_train MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db
     AND TABLE_NAME = 'demand_employee'
     AND COLUMN_NAME = 'id'
     AND EXTRA NOT LIKE '%auto_increment%') > 0,
  'ALTER TABLE demand_employee MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
