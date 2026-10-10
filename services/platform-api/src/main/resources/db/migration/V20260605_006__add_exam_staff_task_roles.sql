ALTER TABLE certification_task_exam_staff
  ADD COLUMN task_role VARCHAR(40) NOT NULL DEFAULT 'exam_staff' COMMENT '任务角色' AFTER status,
  ADD COLUMN role_name VARCHAR(80) NULL COMMENT '角色名称' AFTER task_role;

UPDATE certification_task_exam_staff
SET task_role = CASE WHEN is_leader = 1 THEN 'internal_supervisor' ELSE 'exam_staff' END,
    role_name = CASE WHEN is_leader = 1 THEN '内督' ELSE '考务员' END
WHERE deleted = 0;
