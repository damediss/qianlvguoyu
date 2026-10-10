-- Practical collection is isolated by exam session. Legacy rows remain in scope 0
-- through COALESCE(exam_session_id, 0), so existing task-only flows stay compatible.

ALTER TABLE certification_practical_collection_status
  ADD COLUMN active_exam_session_scope_id BIGINT
    GENERATED ALWAYS AS (
      CASE WHEN deleted = 0 THEN COALESCE(exam_session_id, 0) ELSE NULL END
    ) VIRTUAL AFTER active_candidate_id,
  DROP INDEX uk_cert_practical_collect_status_candidate,
  ADD UNIQUE KEY uk_cert_practical_collect_status_candidate (
    tenant_id, task_id, active_exam_session_scope_id, active_candidate_id
  );

ALTER TABLE certification_practical_collection_queue
  ADD COLUMN active_exam_session_scope_id BIGINT
    GENERATED ALWAYS AS (
      CASE WHEN deleted = 0 THEN COALESCE(exam_session_id, 0) ELSE NULL END
    ) VIRTUAL AFTER active_queue_no,
  DROP INDEX uk_cert_practical_collect_queue_candidate,
  DROP INDEX uk_cert_practical_collect_queue_no,
  ADD UNIQUE KEY uk_cert_practical_collect_queue_candidate (
    tenant_id, task_id, active_exam_session_scope_id, active_queue_candidate_id
  ),
  ADD UNIQUE KEY uk_cert_practical_collect_queue_no (
    tenant_id, task_id, active_exam_session_scope_id, active_queue_no
  );
