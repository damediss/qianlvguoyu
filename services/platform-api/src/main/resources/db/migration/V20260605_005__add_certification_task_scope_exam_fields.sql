ALTER TABLE certification_task_scope
  ADD COLUMN certification_type VARCHAR(80) NULL COMMENT '认定类型' AFTER task_id,
  ADD COLUMN planned_people_count INT NULL COMMENT '计划人数' AFTER level_name;

UPDATE certification_task_scope s
INNER JOIN certification_task t ON t.id = s.task_id AND t.deleted = 0
SET
  s.certification_type = t.certification_type,
  s.planned_people_count = t.planned_people_count
WHERE s.deleted = 0
  AND (s.certification_type IS NULL OR s.planned_people_count IS NULL);
