-- Backfill existing expert assignments from exam_task_ximu into exam_task_ximu_expert
-- This handles assignments made before the exam_task_ximu_expert table existed.

INSERT IGNORE INTO exam_task_ximu_expert (task_id, expert_id, task_role, tag, addtime, tenant_id, deleted)
SELECT t.id, t.expert_start, 0, 'ximu', NOW(), t.tenant_id, 0
FROM exam_task_ximu t
WHERE t.expert_start > 0 AND t.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM exam_task_ximu_expert e
    WHERE e.task_id = t.id AND e.expert_id = t.expert_start
      AND e.task_role = 0 AND e.tag = 'ximu' AND e.deleted = 0
  );

INSERT IGNORE INTO exam_task_ximu_expert (task_id, expert_id, task_role, tag, addtime, tenant_id, deleted)
SELECT t.id, t.expert_end, 1, 'ximu', NOW(), t.tenant_id, 0
FROM exam_task_ximu t
WHERE t.expert_end > 0 AND t.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM exam_task_ximu_expert e
    WHERE e.task_id = t.id AND e.expert_id = t.expert_end
      AND e.task_role = 1 AND e.tag = 'ximu' AND e.deleted = 0
  );
