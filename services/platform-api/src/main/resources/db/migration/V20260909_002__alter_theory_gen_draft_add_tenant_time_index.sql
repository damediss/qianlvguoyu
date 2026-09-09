-- 生成工作台重构：草稿"全部批次"模式按 created_at 倒序跨批次翻页
-- 现有索引均以 batch_id 为前缀（idx_etgd_batch/idx_etgd_review/idx_etgd_publish），跨批次排序会 filesort。
-- 条件化 ADD INDEX（information_schema.statistics 判重），两库幂等。
-- 版本号依据（2026-09-09）：仓库工作区最大 V20260909_001；gjjddb2026(测试) MAX=20260908.003（踩坑 #7）。

SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_theory_gen_draft'
    AND INDEX_NAME = 'idx_etgd_tenant_time');
SET @ddl = (SELECT IF(@has_idx = 0,
  'ALTER TABLE exam_theory_gen_draft ADD KEY idx_etgd_tenant_time (tenant_id, created_at)',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
