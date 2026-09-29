-- 扩展字典项值字段长度，支持最长 500 字符
-- 空库幂等化（踩坑 #34 修复）：空库中 sys_dict_item 由链上 V20260505_001 建出且带
-- UNIQUE KEY uk_sys_dict_item_value(..., item_value, ...)，TEXT 列不能进无长度唯一键，
-- 直接 MODIFY 必报 "used in key specification without a key length"。存量库（PHP 老表）
-- 没有该唯一键所以当年能过。生产终态：item_value TEXT 且无该唯一键。
-- 处理：唯一键存在则先删，再 MODIFY；已是 TEXT 则跳过。存量库本版本已应用不会重跑。
SET @db := DATABASE();

SET @need_drop_key := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_dict_item'
    AND INDEX_NAME = 'uk_sys_dict_item_value');
SET @sql := IF(@need_drop_key > 0,
  'ALTER TABLE sys_dict_item DROP INDEX uk_sys_dict_item_value',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @is_text := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_dict_item'
    AND COLUMN_NAME = 'item_value' AND DATA_TYPE = 'text');
SET @sql := IF(@is_text = 0,
  'ALTER TABLE sys_dict_item MODIFY COLUMN item_value text NOT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
