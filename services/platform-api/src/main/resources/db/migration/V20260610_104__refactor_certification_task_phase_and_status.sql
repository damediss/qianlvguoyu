-- 重构 certification_task 的任务阶段（current_phase）与任务状态（status），让两者解耦：
--   current_phase（任务阶段，4 个取值）：
--     task_creation           -> 任务创建阶段（新任务默认值）
--     preparation             -> 认定筹备阶段（任务创建阶段完成所有材料复审后）
--     archive                 -> 认定归档阶段（认定筹备动作完成后）
--     certification_completed -> 认定完成（认定归档动作完成后）
--   status（任务状态，4 个取值）：
--     in_progress     -> 进行中（阶段内常规工作状态，默认）
--     pending_review  -> 待审核（阶段内材料处于初审/复核中）
--     rejected        -> 已驳回（阶段内材料被驳回）
--     completed       -> 已完成（阶段动作全部完成；认定完成阶段固定该状态）
--
-- 注意：之前 status 与 current_phase 同步存放阶段编码，本次迁移会把 status 收敛到上述 4 个状态值，
-- 并将历史阶段编码 project_creation 重命名为 task_creation。

-- 1. certification_task_phase：把 project_creation 重命名为 task_creation
--    若同一任务已存在 task_creation（应用侧已写入），则软删冗余的 project_creation，避免唯一键冲突
UPDATE certification_task_phase old_phase
INNER JOIN (
  SELECT DISTINCT task_id, tenant_id
  FROM certification_task_phase
  WHERE deleted = 0
    AND phase_code = 'task_creation'
) existing ON existing.task_id = old_phase.task_id
  AND existing.tenant_id = old_phase.tenant_id
SET old_phase.deleted = 1,
    old_phase.updated_at = NOW()
WHERE old_phase.deleted = 0
  AND old_phase.phase_code = 'project_creation';

UPDATE certification_task_phase
SET phase_code = 'task_creation',
    phase_name = '任务创建阶段',
    updated_at = NOW()
WHERE deleted = 0
  AND phase_code = 'project_creation';

-- 2. certification_task.current_phase：把 project_creation 重命名为 task_creation
UPDATE certification_task
SET current_phase = 'task_creation',
    updated_at = NOW()
WHERE deleted = 0
  AND current_phase = 'project_creation';

-- 3. certification_task.status：依据当前阶段把 status 映射到新的 4 状态枚举
--    认定完成阶段 -> completed
UPDATE certification_task
SET status = 'completed',
    updated_at = NOW()
WHERE deleted = 0
  AND current_phase = 'certification_completed';

--    其它阶段（任务创建/认定筹备/认定归档）默认置为 in_progress
UPDATE certification_task
SET status = 'in_progress',
    updated_at = NOW()
WHERE deleted = 0
  AND current_phase IN ('task_creation', 'preparation', 'archive')
  AND status <> 'completed';

-- 4. 重新设置列默认值与注释
ALTER TABLE certification_task
  MODIFY COLUMN current_phase VARCHAR(40) NOT NULL DEFAULT 'task_creation'
  COMMENT '任务阶段：task_creation=任务创建阶段，preparation=认定筹备阶段，archive=认定归档阶段，certification_completed=认定完成';

ALTER TABLE certification_task
  MODIFY COLUMN status VARCHAR(40) NOT NULL DEFAULT 'in_progress'
  COMMENT '任务状态：in_progress=进行中，pending_review=待审核，rejected=已驳回，completed=已完成';
