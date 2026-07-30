-- V20260610_007: /api/exam/exam-details 列表接口性能优化
--
-- 背景: 该接口走 mapper.selectPage(req, wrapper) 查询 exam_exam_detail 表(32 万行),
-- 典型 ORDER BY 子句是:
--   ORDER BY sort_number DESC, id DESC
-- 当前所有索引都不包含 sort_number / id 字段,MySQL 每次都要给 ~15 万行做 filesort
-- (走 idx_ed_tenant 单列索引过滤后),导致接口延迟 2 秒+。
--
-- 优化: 新增 (tenant_id, sort_number DESC, id DESC) 复合索引,
-- 让 WHERE tenant_id=? ORDER BY sort_number DESC, id DESC LIMIT 20
-- 走索引扫描且无 filesort。
--
-- 注意:
-- - MySQL 8.0+ 支持 DESC 索引(项目用 8.4,可用)
-- - 旧索引保留不删(其他接口仍在用)
-- - 包含幂等检查: INFORMATION_SCHEMA.STATISTICS 查重,避免重复跑挂掉
-- - 加索引是大表操作,MySQL 8 默认 ONLINE DDL, ALGORITHM=INPLACE LOCK=NONE, 不阻塞 DML
--
-- 验证 (DDL 后跑):
--   EXPLAIN SELECT id FROM exam_exam_detail
--   WHERE deleted = 0 AND tenant_id = 1
--   ORDER BY sort_number DESC, id DESC LIMIT 20;
--   -- 期望: type=ref, key=idx_ed_tenant_sort_desc, Extra="Using where" (无 filesort)

SET @ix := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'exam_exam_detail'
    AND INDEX_NAME = 'idx_ed_tenant_sort_desc'
);
SET @sql := IF(@ix = 0,
  'CREATE INDEX idx_ed_tenant_sort_desc ON exam_exam_detail (tenant_id, sort_number DESC, id DESC)',
  'SELECT "idx_ed_tenant_sort_desc already exists, skip" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;