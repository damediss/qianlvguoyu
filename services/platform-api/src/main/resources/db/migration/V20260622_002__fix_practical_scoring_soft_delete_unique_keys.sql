-- Only active practical configurations and unified-start sessions must be unique.
-- A nullable generated key lets multiple historical soft-deleted records coexist.
ALTER TABLE certification_practical_scoring_config
  DROP INDEX uk_cert_practical_config_candidate,
  ADD COLUMN active_candidate_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN candidate_id ELSE NULL END) VIRTUAL
    COMMENT '未删除考生id，用于唯一约束',
  ADD UNIQUE KEY uk_cert_practical_config_candidate_active (
    tenant_id, task_id, active_candidate_id
  );

ALTER TABLE certification_practical_start_session
  DROP INDEX uk_cert_practical_start_session,
  ADD COLUMN active_candidate_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN candidate_id ELSE NULL END) VIRTUAL
    COMMENT '未删除考生id，用于唯一约束',
  ADD UNIQUE KEY uk_cert_practical_start_session_active (
    tenant_id, task_id, active_candidate_id
  );
