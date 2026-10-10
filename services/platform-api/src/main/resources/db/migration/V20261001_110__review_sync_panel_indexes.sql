-- 运营数据面板「同步练习」分区聚合索引（《同步练习统计面板方案》v0.1 §9 SQL 核对项）
-- 幂等：information_schema.STATISTICS 守卫，已存在则跳过；存量库与空库均可安全执行。

-- ai_review_answer：created_at 范围过滤 + tenant 等值（近 7 日活跃 / 近 30 天趋势 / 知识点聚合）
SET @idx := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_review_answer'
    AND INDEX_NAME = 'idx_ai_review_answer_tenant_created'
);
SET @sql := IF(@idx > 0, 'SELECT 1',
  'ALTER TABLE ai_review_answer ADD INDEX idx_ai_review_answer_tenant_created (tenant_id, created_at)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ai_review_mock_paper：status='done' + tenant 等值（模考汇总）
SET @idx := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_review_mock_paper'
    AND INDEX_NAME = 'idx_ai_review_mock_paper_tenant_status'
);
SET @sql := IF(@idx > 0, 'SELECT 1',
  'ALTER TABLE ai_review_mock_paper ADD INDEX idx_ai_review_mock_paper_tenant_status (tenant_id, status)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
