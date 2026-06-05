ALTER TABLE certification_task_exam_site
  ADD COLUMN exam_type VARCHAR(40) NOT NULL DEFAULT 'theory' COMMENT '考试类型' AFTER end_time;

UPDATE certification_task_exam_site
SET exam_type = CASE WHEN site_type = 'practical' THEN 'practical' ELSE 'theory' END
WHERE deleted = 0;
