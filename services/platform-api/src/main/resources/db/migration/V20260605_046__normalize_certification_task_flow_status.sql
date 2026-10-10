ALTER TABLE certification_task
  MODIFY status VARCHAR(40) NOT NULL DEFAULT 'running'
  COMMENT '任务状态：running进行中，pending_review待审核，rejected已驳回，project_prepared认定准备已完成，preparation_completed认定筹备已完成，archive_completed认定归档已完成';

UPDATE certification_task
SET status = 'project_prepared',
    last_business_updated_at = COALESCE(last_business_updated_at, NOW()),
    updated_at = NOW()
WHERE status = 'completed'
  AND deleted = 0;

UPDATE certification_task task
JOIN certification_archive_record archive_record
  ON archive_record.task_id = task.id
 AND archive_record.deleted = 0
 AND archive_record.archive_status = 'ARCHIVED'
SET task.status = 'archive_completed',
    task.current_phase = 'archive',
    task.last_business_updated_at = COALESCE(archive_record.archived_at, task.last_business_updated_at, NOW()),
    task.updated_at = NOW()
WHERE task.deleted = 0;
