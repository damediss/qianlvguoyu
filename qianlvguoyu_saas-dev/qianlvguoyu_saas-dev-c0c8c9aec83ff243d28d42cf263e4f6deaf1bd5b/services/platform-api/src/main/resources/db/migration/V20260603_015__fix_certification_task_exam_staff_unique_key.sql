-- 逻辑删除时 deleted 纳入唯一键会导致二次删除/重复绑定冲突
ALTER TABLE certification_task_exam_staff
  DROP INDEX uk_cert_task_exam_staff;

DELETE t1
FROM certification_task_exam_staff t1
INNER JOIN certification_task_exam_staff t2
  ON t1.tenant_id = t2.tenant_id
  AND t1.task_id = t2.task_id
  AND t1.staff_id = t2.staff_id
  AND t1.id < t2.id;

ALTER TABLE certification_task_exam_staff
  ADD UNIQUE KEY uk_cert_task_exam_staff (tenant_id, task_id, staff_id);
