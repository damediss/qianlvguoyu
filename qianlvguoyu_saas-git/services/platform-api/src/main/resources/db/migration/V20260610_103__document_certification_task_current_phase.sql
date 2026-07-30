-- 补充 certification_task.current_phase / status 字段注释，并将默认值对齐到四阶段流程首阶段。
-- 四阶段编码：
--   project_creation        -> 项目创建阶段
--   preparation             -> 认定筹备阶段
--   archive                 -> 认定归档阶段
--   certification_completed -> 认定完成

ALTER TABLE certification_task
  MODIFY COLUMN current_phase VARCHAR(40) NOT NULL DEFAULT 'project_creation'
  COMMENT '当前阶段：project_creation=项目创建阶段，preparation=认定筹备阶段，archive=认定归档阶段，certification_completed=认定完成';

ALTER TABLE certification_task
  MODIFY COLUMN status VARCHAR(40) NOT NULL DEFAULT 'project_creation'
  COMMENT '任务状态（与四阶段对齐）：project_creation=项目创建阶段，preparation=认定筹备阶段，archive=认定归档阶段，certification_completed=认定完成';

ALTER TABLE certification_task
  MODIFY COLUMN phase_total_count INT NOT NULL DEFAULT 4 COMMENT '阶段总数';
