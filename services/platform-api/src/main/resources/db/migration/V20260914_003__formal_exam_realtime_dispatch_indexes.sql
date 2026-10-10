-- Formal exam realtime dispatcher indexes.
-- The dispatcher claims session heads with a short SELECT ... FOR UPDATE SKIP LOCKED
-- transaction and recovers only a bounded lease batch. Keep both access paths
-- indexed; do not make the request thread scan the entire outbox.

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'competition_formal_exam_outbox'
        AND INDEX_NAME = 'idx_comp_formal_exam_outbox_head'
    ),
    'SELECT 1',
    'ALTER TABLE competition_formal_exam_outbox ADD KEY idx_comp_formal_exam_outbox_head (status, deleted, exam_session_id, seq, id, tenant_id)'
  )
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'competition_formal_exam_outbox'
        AND INDEX_NAME = 'idx_comp_formal_exam_outbox_lease'
    ),
    'SELECT 1',
    'ALTER TABLE competition_formal_exam_outbox ADD KEY idx_comp_formal_exam_outbox_lease (status, deleted, updated_at, id)'
  )
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
