-- Keep dispatch progress away from the allocation cursor row.
-- Answer requests update last_seq; the dispatcher updates dispatch_seq. They
-- must not contend on the same InnoDB record during a high-concurrency exam.

CREATE TABLE IF NOT EXISTS competition_formal_exam_realtime_dispatch_cursor (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  dispatch_seq BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_exam_realtime_dispatch_cursor_seq CHECK (dispatch_seq >= 0),
  UNIQUE KEY uk_comp_formal_exam_realtime_dispatch_cursor_session (tenant_id, exam_session_id, deleted),
  KEY idx_comp_formal_exam_realtime_dispatch_cursor_seq (
    tenant_id, exam_session_id, dispatch_seq, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛实时 Outbox 派发游标';

-- Existing installations already have one allocation cursor per session. Seed
-- dispatch state from its current value so applying this migration does not
-- replay events that were completed before the split.
INSERT IGNORE INTO competition_formal_exam_realtime_dispatch_cursor (
  id, tenant_id, exam_session_id, dispatch_seq,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT
  id, tenant_id, exam_session_id, COALESCE(dispatch_seq, 0),
  created_by, updated_by, created_at, updated_at, deleted
FROM competition_formal_exam_realtime_cursor
WHERE deleted = 0;

UPDATE competition_formal_exam_realtime_dispatch_cursor d
INNER JOIN competition_formal_exam_realtime_cursor c
  ON c.tenant_id = d.tenant_id
 AND c.exam_session_id = d.exam_session_id
 AND c.deleted = 0
SET d.dispatch_seq = GREATEST(d.dispatch_seq, c.dispatch_seq),
    d.updated_at = GREATEST(d.updated_at, c.updated_at)
WHERE d.deleted = 0;
