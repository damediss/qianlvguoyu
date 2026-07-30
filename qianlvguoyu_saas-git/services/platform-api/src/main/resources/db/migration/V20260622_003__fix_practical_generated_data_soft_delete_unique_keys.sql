-- The cancellation flow soft-deletes these generated records. Keep only active records unique
-- so a candidate can be configured, cancelled, and configured again repeatedly.
ALTER TABLE certification_assessor_task
  DROP INDEX uk_cert_assessor_scoring_task,
  ADD COLUMN active_candidate_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN candidate_id ELSE NULL END) VIRTUAL
    COMMENT '未删除考生id，用于唯一约束',
  ADD UNIQUE KEY uk_cert_assessor_scoring_task_active (
    tenant_id, task_id, active_candidate_id, paper_question_id, assessor_id
  );

ALTER TABLE certification_practical_question_result
  DROP INDEX uk_cert_practical_question_result,
  ADD COLUMN active_candidate_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN candidate_id ELSE NULL END) VIRTUAL
    COMMENT '未删除考生id，用于唯一约束',
  ADD UNIQUE KEY uk_cert_practical_question_result_active (
    tenant_id, task_id, active_candidate_id, paper_question_id
  );

ALTER TABLE certification_practical_candidate_result
  DROP INDEX uk_cert_practical_candidate_result,
  ADD COLUMN active_candidate_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN candidate_id ELSE NULL END) VIRTUAL
    COMMENT '未删除考生id，用于唯一约束',
  ADD UNIQUE KEY uk_cert_practical_candidate_result_active (
    tenant_id, task_id, active_candidate_id
  );
