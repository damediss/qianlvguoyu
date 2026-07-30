-- 将 certification_task.status 对齐到四阶段流程：
--   project_creation  -> 项目创建阶段
--   preparation       -> 认定筹备阶段
--   archive           -> 认定归档阶段
--   certification_completed -> 认定完成
-- 现存任务依据原 status 与 current_phase 联合判定后映射到四阶段中的某一个。

UPDATE certification_task
SET status = 'certification_completed',
    current_phase = 'certification_completed',
    phase_completed_count = 4,
    phase_total_count = 4,
    updated_at = NOW()
WHERE deleted = 0
  AND (status = 'archive_completed' OR current_phase = 'certification_completed');

UPDATE certification_task
SET status = 'archive',
    current_phase = 'archive',
    phase_completed_count = 2,
    phase_total_count = 4,
    updated_at = NOW()
WHERE deleted = 0
  AND status NOT IN ('certification_completed')
  AND (status = 'preparation_completed' OR current_phase = 'archive');

UPDATE certification_task
SET status = 'preparation',
    current_phase = 'preparation',
    phase_completed_count = 1,
    phase_total_count = 4,
    updated_at = NOW()
WHERE deleted = 0
  AND status NOT IN ('certification_completed', 'archive')
  AND (status = 'project_prepared' OR current_phase = 'preparation');

UPDATE certification_task
SET status = 'project_creation',
    current_phase = 'project_creation',
    phase_completed_count = 0,
    phase_total_count = 4,
    updated_at = NOW()
WHERE deleted = 0
  AND status NOT IN ('certification_completed', 'archive', 'preparation');

-- 同步 certification_task_phase 中的状态：本阶段前置全部 completed、本阶段 running、后置 pending；
-- 认定完成阶段将全部阶段标记为 completed。
UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'completed',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.completed_at = COALESCE(phase.completed_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'certification_completed'
  AND phase.phase_code IN ('project_creation', 'preparation', 'archive', 'certification_completed');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'completed',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.completed_at = COALESCE(phase.completed_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'archive'
  AND phase.phase_code IN ('project_creation', 'preparation');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'running',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'archive'
  AND phase.phase_code = 'archive';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'pending',
    phase.started_at = NULL,
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'archive'
  AND phase.phase_code = 'certification_completed';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'completed',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.completed_at = COALESCE(phase.completed_at, NOW()),
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'preparation'
  AND phase.phase_code = 'project_creation';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'running',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'preparation'
  AND phase.phase_code = 'preparation';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'pending',
    phase.started_at = NULL,
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'preparation'
  AND phase.phase_code IN ('archive', 'certification_completed');

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'running',
    phase.started_at = COALESCE(phase.started_at, NOW()),
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'project_creation'
  AND phase.phase_code = 'project_creation';

UPDATE certification_task_phase phase
JOIN certification_task task ON task.id = phase.task_id AND task.deleted = 0
SET phase.status = 'pending',
    phase.started_at = NULL,
    phase.completed_at = NULL,
    phase.updated_at = NOW()
WHERE phase.deleted = 0
  AND task.status = 'project_creation'
  AND phase.phase_code IN ('preparation', 'archive', 'certification_completed');
