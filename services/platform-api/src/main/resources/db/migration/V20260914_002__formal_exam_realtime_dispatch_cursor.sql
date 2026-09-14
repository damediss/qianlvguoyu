-- Separate realtime dispatch progress from the event-allocation high watermark.
-- last_seq is allocated by request transactions; dispatch_seq is advanced only
-- after the corresponding Outbox row is durably published or dropped.

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'competition_formal_exam_realtime_cursor'
        AND COLUMN_NAME = 'dispatch_seq'
    ),
    'SELECT 1',
    'ALTER TABLE competition_formal_exam_realtime_cursor ADD COLUMN dispatch_seq BIGINT NOT NULL DEFAULT 0 AFTER last_seq'
  )
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'competition_formal_exam_realtime_cursor'
        AND INDEX_NAME = 'idx_comp_formal_exam_realtime_cursor_dispatch'
    ),
    'SELECT 1',
    'ALTER TABLE competition_formal_exam_realtime_cursor ADD KEY idx_comp_formal_exam_realtime_cursor_dispatch (tenant_id, exam_session_id, dispatch_seq, deleted)'
  )
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
