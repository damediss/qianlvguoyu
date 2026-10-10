-- 竞赛试题序号只对未删除题目保持唯一，软删除记录不再占用序号。
-- Production may already have the new functional index from a manual rollout.
SET @has_named_index := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_question'
    AND INDEX_NAME = 'uk_competition_question_scope_order'
);
SET @has_functional_index_part := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition_question'
    AND INDEX_NAME = 'uk_competition_question_scope_order'
    AND COLUMN_NAME IS NULL
);
SET @ddl := IF(@has_functional_index_part > 0,
  'SELECT 1',
  IF(@has_named_index > 0,
    'ALTER TABLE competition_question DROP INDEX uk_competition_question_scope_order, ADD UNIQUE KEY uk_competition_question_scope_order (tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, question_type, (CASE WHEN deleted = 0 THEN sort_order ELSE NULL END))',
    'ALTER TABLE competition_question ADD UNIQUE KEY uk_competition_question_scope_order (tenant_id, event_id, event_work_type_id, event_work_type_level_id, content_type, question_type, (CASE WHEN deleted = 0 THEN sort_order ELSE NULL END))'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
