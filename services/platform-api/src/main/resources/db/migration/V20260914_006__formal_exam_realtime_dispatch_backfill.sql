-- Repair dispatch progress for Outbox rows that predate the split dispatch
-- cursor. The old allocation cursor only knows the allocation high-watermark;
-- initialize dispatch progress from the first unfinished event instead.

INSERT IGNORE INTO competition_formal_exam_realtime_dispatch_cursor (
  id, tenant_id, exam_session_id, dispatch_seq,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT
  c.id, c.tenant_id, c.exam_session_id, 0,
  c.created_by, c.updated_by, c.created_at, c.updated_at, c.deleted
FROM competition_formal_exam_realtime_cursor c
WHERE c.deleted = 0;

UPDATE competition_formal_exam_realtime_dispatch_cursor d
INNER JOIN (
  SELECT
    tenant_id,
    exam_session_id,
    COALESCE(
      MIN(CASE WHEN status NOT IN ('published', 'dropped') THEN seq END) - 1,
      MAX(seq),
      0
    ) AS completed_prefix_seq
  FROM competition_formal_exam_outbox
  WHERE deleted = 0
  GROUP BY tenant_id, exam_session_id
) progress
  ON progress.tenant_id = d.tenant_id
 AND progress.exam_session_id = d.exam_session_id
SET d.dispatch_seq = GREATEST(d.dispatch_seq, progress.completed_prefix_seq),
    d.updated_at = CURRENT_TIMESTAMP
WHERE d.deleted = 0;
