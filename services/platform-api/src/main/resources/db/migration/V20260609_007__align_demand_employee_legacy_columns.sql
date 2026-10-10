-- 对齐共享库 demand_employee 列名/字段，兼容 Java DemandEmployeeEntity

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'created_at') > 0
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'create_time') = 0,
  'ALTER TABLE demand_employee CHANGE COLUMN created_at create_time DATETIME NULL COMMENT ''创建时间''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'updated_at') > 0
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE demand_employee CHANGE COLUMN updated_at update_time DATETIME NULL COMMENT ''更新时间''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'addtime') = 0,
  'ALTER TABLE demand_employee ADD COLUMN addtime INT NULL COMMENT ''添加时间戳'' AFTER major',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'create_time') = 0,
  'ALTER TABLE demand_employee ADD COLUMN create_time DATETIME NULL COMMENT ''创建时间'' AFTER tenant_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE demand_employee ADD COLUMN update_time DATETIME NULL COMMENT ''更新时间'' AFTER create_time',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 若曾误加 create_time/update_time 且仍保留 created_at/updated_at，合并后删除旧列
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'created_at') > 0
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'create_time') > 0,
  'UPDATE demand_employee SET create_time = COALESCE(create_time, created_at), update_time = COALESCE(update_time, updated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'created_at') > 0
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_employee' AND COLUMN_NAME = 'create_time') > 0,
  'ALTER TABLE demand_employee DROP COLUMN created_at, DROP COLUMN updated_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
