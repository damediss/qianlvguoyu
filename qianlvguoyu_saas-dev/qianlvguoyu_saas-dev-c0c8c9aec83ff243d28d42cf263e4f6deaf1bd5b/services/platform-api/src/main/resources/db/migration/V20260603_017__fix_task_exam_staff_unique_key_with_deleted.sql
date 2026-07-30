-- 与考评员分配表一致：唯一键包含 deleted，避免逻辑删除后无法再次删除/绑定
ALTER TABLE certification_task_exam_staff
  DROP INDEX uk_cert_task_exam_staff;

DELETE t1
FROM certification_task_exam_staff t1
INNER JOIN certification_task_exam_staff t2
  ON t1.tenant_id = t2.tenant_id
  AND t1.task_id = t2.task_id
  AND t1.staff_id = t2.staff_id
  AND t1.deleted = t2.deleted
  AND t1.id < t2.id;

ALTER TABLE certification_task_exam_staff
  ADD UNIQUE KEY uk_cert_task_exam_staff (tenant_id, task_id, staff_id, deleted);

-- 修复无有效任务绑定但仍显示「认定任务中」的考务人员
UPDATE certification_info_exam_staff s
SET s.work_status = 'idle',
    s.referenced_count = 0,
    s.updated_at = NOW()
WHERE s.deleted = 0
  AND s.audit_status = 'approved'
  AND s.work_status = 'assigned'
  AND NOT EXISTS (
    SELECT 1
    FROM certification_task_exam_staff t
    WHERE t.staff_id = s.id
      AND t.deleted = 0
  );

-- 同步引用次数与有效绑定数一致
UPDATE certification_info_exam_staff s
SET s.referenced_count = (
      SELECT COUNT(*)
      FROM certification_task_exam_staff t
      WHERE t.staff_id = s.id
        AND t.deleted = 0
    ),
    s.updated_at = NOW()
WHERE s.deleted = 0
  AND s.audit_status = 'approved';
