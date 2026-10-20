-- 历年真题按工种+等级共享（方案 v2.1 §4.2）：exam_zhenti 增加共享组字段
-- 取号：2026-10-20 实测两库 MAX(version) 均为 20261018.004，顺延 20261020.001~004。
-- 幂等：列/索引存在则跳过（information_schema 守卫 + PREPARE，踩坑 #34 手法）。
-- scope_key 由应用统一生成（ZhentiScopeKey），默认值 '' 表示待回填（002 处理）。

SET @col_wt := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_zhenti' AND COLUMN_NAME = 'work_type_id');
SET @ddl := IF(@col_wt = 0,
  'ALTER TABLE exam_zhenti ADD COLUMN work_type_id BIGINT NULL COMMENT ''共享组：工种（exam_work_type 叶子节点）'' AFTER exam_id',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_lv := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_zhenti' AND COLUMN_NAME = 'level_code');
SET @ddl := IF(@col_lv = 0,
  'ALTER TABLE exam_zhenti ADD COLUMN level_code TINYINT NULL COMMENT ''共享组：等级 0五级 1四级 2三级 3二级 4一级'' AFTER work_type_id',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_key := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_zhenti' AND COLUMN_NAME = 'scope_key');
SET @ddl := IF(@col_key = 0,
  'ALTER TABLE exam_zhenti ADD COLUMN scope_key VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''去重键：work:{workTypeId}:{level} 或 exam:{examId}'' AFTER level_code',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_scope := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_zhenti' AND INDEX_NAME = 'idx_zhenti_scope');
SET @ddl := IF(@idx_scope = 0,
  'ALTER TABLE exam_zhenti ADD KEY idx_zhenti_scope (tenant_id, work_type_id, level_code, status, deleted)',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
