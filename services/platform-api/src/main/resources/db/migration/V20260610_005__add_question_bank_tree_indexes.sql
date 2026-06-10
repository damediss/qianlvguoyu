-- V20260610_005: 题库分类导航聚合树(/api/exam/question-bank-tree)性能优化
--
-- 背景: 该接口一次拉 4 张表 (work_type / course / knowledge_structure / exam_detail),
-- 其中 exam_exam_detail 表 32 万行,旧实现把所有 (id, knowledge_id) 拉到 Java 内存聚合,
-- 走的是 idx_ed_tenant 单列索引扫描 ~15 万行,再 GROUP BY,接口延迟 500ms+。
--
-- 优化:
-- 1) exam_exam_detail 增加 (tenant_id, knowledge_id, deleted) 覆盖索引,
--    让 "SELECT knowledge_id, COUNT(*) WHERE tenant_id=? AND deleted=0 AND
--    knowledge_id IS NOT NULL GROUP BY knowledge_id" 走索引覆盖,无需回表。
-- 2) exam_work_type 增加 (tenant_id, sort_number, id) 索引,
--    消除 ORDER BY sort_number, id 的 filesort。
-- 3) exam_knowledge_structure 增加 (tenant_id, status, course_id) 索引,
--    现有 idx_ks_course (tenant_id, course_id) 缺少 status 前缀,
--    三列等值过滤能用上覆盖扫描。
--
-- 注意:
-- - MySQL 8 默认 ONLINE DDL, ALGORITHM=INPLACE LOCK=NONE, 不阻塞 DML
-- - 旧索引保留不删 (其他接口仍在用)
-- - 包含幂等检查: INFORMATION_SCHEMA.STATISTICS 查重,避免重复跑挂掉

-- ============ 1. exam_exam_detail 覆盖索引 ============
SET @ix := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'exam_exam_detail'
    AND INDEX_NAME = 'idx_ed_tenant_knowledge_deleted'
);
SET @sql := IF(@ix = 0,
  'CREATE INDEX idx_ed_tenant_knowledge_deleted ON exam_exam_detail (tenant_id, knowledge_id, deleted)',
  'SELECT "idx_ed_tenant_knowledge_deleted already exists, skip" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 2. exam_work_type 排序索引 ============
SET @ix := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'exam_work_type'
    AND INDEX_NAME = 'idx_worktype_tenant_sort'
);
SET @sql := IF(@ix = 0,
  'CREATE INDEX idx_worktype_tenant_sort ON exam_work_type (tenant_id, sort_number, id)',
  'SELECT "idx_worktype_tenant_sort already exists, skip" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 3. exam_knowledge_structure 三列前缀索引 ============
SET @ix := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'exam_knowledge_structure'
    AND INDEX_NAME = 'idx_ks_tenant_status_course'
);
SET @sql := IF(@ix = 0,
  'CREATE INDEX idx_ks_tenant_status_course ON exam_knowledge_structure (tenant_id, status, course_id)',
  'SELECT "idx_ks_tenant_status_course already exists, skip" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;