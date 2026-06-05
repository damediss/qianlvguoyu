ALTER TABLE certification_assessor_task
  ADD COLUMN assessment_file_id BIGINT NULL COMMENT '考评表文件id' AFTER title,
  ADD COLUMN assessment_file_name VARCHAR(300) NULL COMMENT '考评表文件名' AFTER assessment_file_id;

