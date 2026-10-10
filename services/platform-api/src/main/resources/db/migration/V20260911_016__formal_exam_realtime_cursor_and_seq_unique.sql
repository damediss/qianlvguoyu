-- Formal exam realtime cursor + outbox seq uniqueness.
-- Flyway take-number (2026-09-11):
--   gjjddb2026 MAX(version)=20260911.011
--   gjjddbpro  MAX(version)=20260911.014
--   local uncommitted files already occupy 20260911.001/.002/.003
--   new version = max(both DBs)+interval -> 20260911.016
-- MUST run while application processes are stopped.
-- Does not delete historical outbox rows. Does not modify V20260909_007.

CREATE TABLE IF NOT EXISTS competition_formal_exam_realtime_cursor (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  last_seq BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_exam_realtime_cursor_seq CHECK (last_seq >= 0),
  UNIQUE KEY uk_comp_formal_exam_realtime_cursor_session (tenant_id, exam_session_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛实时 Outbox 序号游标';

-- Deterministic rewrite of live seq to 1..N per tenant_id + exam_session_id.
-- Also rewrite payload_json.seq so the durable envelope matches the column.
UPDATE competition_formal_exam_outbox o
INNER JOIN (
  SELECT id,
         ROW_NUMBER() OVER (
           PARTITION BY tenant_id, exam_session_id
           ORDER BY created_at, id
         ) AS new_seq
  FROM competition_formal_exam_outbox
  WHERE deleted = 0
) ranked ON ranked.id = o.id
SET o.seq = ranked.new_seq,
    o.payload_json = IF(
      JSON_VALID(o.payload_json),
      JSON_SET(o.payload_json, '$.seq', ranked.new_seq),
      o.payload_json
    ),
    o.updated_at = NOW()
WHERE o.deleted = 0;

INSERT INTO competition_formal_exam_realtime_cursor (
  id, tenant_id, exam_session_id, last_seq, deleted, created_by, updated_by, created_at, updated_at
)
SELECT
  exam_session_id,
  tenant_id,
  exam_session_id,
  MAX(seq),
  0,
  NULL,
  NULL,
  NOW(),
  NOW()
FROM competition_formal_exam_outbox
WHERE deleted = 0
GROUP BY tenant_id, exam_session_id
ON DUPLICATE KEY UPDATE
  last_seq = GREATEST(last_seq, VALUES(last_seq)),
  updated_at = NOW();

-- Duplicate check before UNIQUE. CHECK(dup_count = 0) fails the migration
-- instead of deleting historical outbox rows.
DROP TABLE IF EXISTS competition_formal_exam_outbox_seq_dup_guard;
CREATE TABLE competition_formal_exam_outbox_seq_dup_guard (
  dup_count INT NOT NULL,
  CONSTRAINT chk_comp_formal_exam_outbox_seq_dup CHECK (dup_count = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO competition_formal_exam_outbox_seq_dup_guard (dup_count)
SELECT COUNT(*)
FROM (
  SELECT 1
  FROM competition_formal_exam_outbox
  WHERE deleted = 0
  GROUP BY tenant_id, exam_session_id, seq
  HAVING COUNT(*) > 1
) leftover;

DROP TABLE IF EXISTS competition_formal_exam_outbox_seq_dup_guard;

SET @ddl = (
  SELECT IF(
    (
      SELECT DATA_TYPE
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'competition_formal_exam_outbox'
        AND COLUMN_NAME = 'seq'
    ) = 'bigint',
    'SELECT 1',
    'ALTER TABLE competition_formal_exam_outbox MODIFY seq BIGINT NOT NULL'
  )
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'competition_formal_exam_outbox'
        AND INDEX_NAME = 'uk_comp_formal_exam_outbox_session_seq'
    ),
    'SELECT 1',
    'ALTER TABLE competition_formal_exam_outbox ADD UNIQUE KEY uk_comp_formal_exam_outbox_session_seq (tenant_id, exam_session_id, seq, deleted)'
  )
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'competition_formal_exam_outbox'
        AND INDEX_NAME = 'idx_comp_formal_exam_outbox_session'
    ),
    'ALTER TABLE competition_formal_exam_outbox DROP INDEX idx_comp_formal_exam_outbox_session',
    'SELECT 1'
  )
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
