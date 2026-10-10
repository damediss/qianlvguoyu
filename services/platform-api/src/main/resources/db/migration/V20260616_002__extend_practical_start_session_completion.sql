ALTER TABLE certification_practical_start_session
  ADD COLUMN completed_at DATETIME NULL COMMENT 'Practical exam completed time' AFTER started_by,
  ADD COLUMN completed_by BIGINT NULL COMMENT 'Practical exam completed by user id' AFTER completed_at,
  ADD COLUMN summary_generated_at DATETIME NULL COMMENT 'Practical score summary generated time' AFTER completed_by,
  ADD COLUMN summary_file_name VARCHAR(255) NULL COMMENT 'Practical score summary file name' AFTER summary_generated_at;

ALTER TABLE certification_practical_start_session
  ADD KEY idx_cert_practical_start_group_active (tenant_id, task_id, group_id, status, deleted);
