-- 对齐共享库 demand_train 列名：created_at/updated_at → create_time/update_time
-- Java 实体 DemandTrainEntity 使用 createTime/updateTime（映射 create_time/update_time）

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'created_at') > 0
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'create_time') = 0,
  'ALTER TABLE demand_train CHANGE COLUMN created_at create_time DATETIME NULL COMMENT ''创建时间''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'updated_at') > 0
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE demand_train CHANGE COLUMN updated_at update_time DATETIME NULL COMMENT ''更新时间''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 共享库可能缺少 addtime
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'demand_train' AND COLUMN_NAME = 'addtime') = 0,
  'ALTER TABLE demand_train ADD COLUMN addtime INT NULL COMMENT ''添加时间戳'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
