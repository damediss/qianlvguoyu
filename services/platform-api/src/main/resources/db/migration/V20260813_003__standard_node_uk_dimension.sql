-- 细目表节点唯一键修正：理论叶与技能叶共享同一 L2 下的 001… 序号段，
-- 原 uk(task_id, level_code, direction, node_key) 导致技能叶插入与理论叶撞键
-- （Duplicate entry '…-B-D-B-001'，2026-08-13 E2E 实测）。唯一键加入 dimension 区分。
-- 版本号依据：DB flyway_schema_history MAX(version)=20260813.002（2026-08-13 查询）
ALTER TABLE exam_standard_node
  DROP INDEX uk_standard_node_key,
  ADD UNIQUE KEY uk_standard_node_key (task_id, level_code, direction(32), dimension, node_key);
