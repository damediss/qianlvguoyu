ALTER TABLE certification_task
  DROP INDEX idx_certification_task_level,
  DROP COLUMN work_type_name,
  DROP COLUMN level_code,
  DROP COLUMN level_name;
