-- 空库幂等化（踩坑 #34 修复）：存量库 train_solution_badge 是 PHP 老表无 update_time，
-- 但迁移链上游 V20260521_001 的建表已自带 update_time，空库从零跑会 Duplicate column。
-- 列已存在则跳过；存量库本版本已应用不会重跑（validate-on-migrate=false）。
SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'train_solution_badge' AND COLUMN_NAME = 'update_time') = 0,
  'ALTER TABLE train_solution_badge ADD COLUMN update_time DATETIME NULL COMMENT ''更新时间'' AFTER create_time',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
