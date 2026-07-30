ALTER TABLE certification_practical_scoring_config
  ADD COLUMN candidate_id BIGINT NULL COMMENT '按学员单独配置的考生id' AFTER scope_id;

ALTER TABLE certification_practical_scoring_config
  DROP INDEX uk_cert_practical_config,
  ADD UNIQUE KEY uk_cert_practical_config_candidate (
    tenant_id, task_id, candidate_id, deleted
  ),
  ADD KEY idx_cert_practical_config_scope (
    tenant_id, task_id, scope_id, status, deleted
  );
