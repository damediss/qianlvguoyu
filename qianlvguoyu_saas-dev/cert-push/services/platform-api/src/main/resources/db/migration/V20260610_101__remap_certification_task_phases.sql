-- 将认定任务流程从 5 阶段收敛为 4 阶段：项目创建 -> 认定筹备 -> 认定归档 -> 认定完成

UPDATE certification_task_phase
SET deleted = 1,
    updated_at = NOW()
WHERE deleted = 0
  AND phase_code IN ('before_exam', 'theory_exam', 'practical_exam');

UPDATE certification_task_phase
SET phase_code = 'project_creation',
    phase_name = '项目创建阶段',
    phase_order = 1,
    updated_at = NOW()
WHERE deleted = 0
  AND phase_code = 'preparation';

UPDATE certification_task_phase
SET phase_name = '认定归档阶段',
    phase_order = 3,
    updated_at = NOW()
WHERE deleted = 0
  AND phase_code = 'archive';

INSERT INTO certification_task_phase (
  id, tenant_id, task_id, phase_code, phase_name, phase_order, status,
  started_at, completed_at, completed_by, remark, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  task.id * 10 + 2,
  task.tenant_id,
  task.id,
  'preparation',
  '认定筹备阶段',
  2,
  'pending',
  NULL,
  NULL,
  NULL,
  NULL,
  NOW(),
  NOW(),
  task.created_by,
  task.updated_by,
  0
FROM certification_task task
WHERE task.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM certification_task_phase phase
    WHERE phase.task_id = task.id
      AND phase.phase_code = 'preparation'
      AND phase.deleted = 0
  );

INSERT INTO certification_task_phase (
  id, tenant_id, task_id, phase_code, phase_name, phase_order, status,
  started_at, completed_at, completed_by, remark, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  task.id * 10 + 4,
  task.tenant_id,
  task.id,
  'certification_completed',
  '认定完成',
  4,
  'pending',
  NULL,
  NULL,
  NULL,
  NULL,
  NOW(),
  NOW(),
  task.created_by,
  task.updated_by,
  0
FROM certification_task task
WHERE task.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM certification_task_phase phase
    WHERE phase.task_id = task.id
      AND phase.phase_code = 'certification_completed'
      AND phase.deleted = 0
  );

UPDATE certification_task
SET phase_total_count = 4,
    updated_at = NOW()
WHERE deleted = 0;

UPDATE certification_task
SET current_phase = 'certification_completed',
    phase_completed_count = 4,
    updated_at = NOW()
WHERE deleted = 0
  AND status = 'archive_completed';

UPDATE certification_task
SET current_phase = 'archive',
    phase_completed_count = 2,
    updated_at = NOW()
WHERE deleted = 0
  AND status = 'preparation_completed';

UPDATE certification_task
SET current_phase = 'preparation',
    phase_completed_count = 1,
    updated_at = NOW()
WHERE deleted = 0
  AND status = 'project_prepared';

UPDATE certification_task
SET current_phase = 'project_creation',
    phase_completed_count = 0,
    updated_at = NOW()
WHERE deleted = 0
  AND status IN ('running', 'pending_review', 'rejected', 'approved');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'completed',
    phase.completed_at = COALESCE(phase.completed_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'archive_completed'
  AND phase.phase_code IN ('project_creation', 'preparation', 'archive', 'certification_completed');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'completed',
    phase.completed_at = COALESCE(phase.completed_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'preparation_completed'
  AND phase.phase_code IN ('project_creation', 'preparation');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'running',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'preparation_completed'
  AND phase.phase_code = 'archive';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'completed',
    phase.completed_at = COALESCE(phase.completed_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'project_prepared'
  AND phase.phase_code = 'project_creation';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'running',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'project_prepared'
  AND phase.phase_code = 'preparation';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'running',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status IN ('running', 'pending_review', 'rejected', 'approved')
  AND phase.phase_code = 'project_creation';

UPDATE certification_task
SET current_phase = 'archive',
    phase_completed_count = 2,
    updated_at = NOW()
WHERE deleted = 0
  AND current_phase IN ('before_exam', 'theory_exam', 'practical_exam')
  AND status NOT IN ('archive_completed', 'preparation_completed', 'project_prepared');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'pending',
    phase.started_at = NULL,
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status IN ('running', 'pending_review', 'rejected', 'approved')
  AND phase.phase_code IN ('preparation', 'archive', 'certification_completed');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'pending',
    phase.started_at = NULL,
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'project_prepared'
  AND phase.phase_code IN ('archive', 'certification_completed');
