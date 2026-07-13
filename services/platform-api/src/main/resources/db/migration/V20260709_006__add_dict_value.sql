-- 为字典表增加 value 字段(通过 code 查询对应的 value)
SET @db := DATABASE();
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_dict' AND COLUMN_NAME = 'value') = 0,
  'ALTER TABLE sys_dict ADD COLUMN value VARCHAR(1000) NOT NULL DEFAULT '''' COMMENT ''字典值'' AFTER name',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
