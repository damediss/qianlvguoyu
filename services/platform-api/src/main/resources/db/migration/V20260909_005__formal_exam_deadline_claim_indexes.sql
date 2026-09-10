-- Formal exam P0 indexes, deadline_at, and expire-claim ownership.
-- deadline_at is stored so expired-session scans are sargable.
-- expire_claim_* lets one application instance own deadline finalization.

ALTER TABLE competition_formal_exam_session
  ADD COLUMN deadline_at DATETIME NULL AFTER duration_seconds,
  ADD COLUMN expire_claim_until DATETIME NULL AFTER occupancy_token,
  ADD COLUMN expire_claim_owner VARCHAR(64) NULL AFTER expire_claim_until;

UPDATE competition_formal_exam_session
SET deadline_at = TIMESTAMPADD(SECOND, duration_seconds, actual_start_time)
WHERE deleted = 0
  AND actual_start_time IS NOT NULL
  AND duration_seconds > 0
  AND deadline_at IS NULL;

ALTER TABLE competition_formal_exam_session
  ADD KEY idx_comp_formal_exam_session_deadline (
    tenant_id, status, occupancy_token, deadline_at, expire_claim_until, deleted
  );

ALTER TABLE competition_formal_candidate_exam
  ADD KEY idx_comp_formal_candidate_exam_session_status (
    tenant_id, exam_session_id, status, deleted
  ),
  ADD KEY idx_comp_formal_candidate_exam_session_player (
    tenant_id, exam_session_id, player_id, deleted
  ),
  ADD KEY idx_comp_formal_candidate_exam_list (
    tenant_id, exam_session_id, paper_code, candidate_no, id, deleted
  );
