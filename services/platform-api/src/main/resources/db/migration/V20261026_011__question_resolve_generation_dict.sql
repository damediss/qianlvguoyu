-- 试题解析 Markdown 表格化一期（方案 docs/04_requirements/试题解析Markdown表格化_一期实施方案.md
-- §5.4/§5.9）：解析生成的总开关、回写开关、每学员每日限流阈值。
-- 默认 generation=0 / writeback=0：本次发版只统一读取与按需生成框架，不开放新生成；
-- shadow 验收（§10.1）前生产不得置 1。生产 resolve 列仍为 varchar(600)，
-- 开放写回前必须由用户先改 text（方案 §0.2 工程约束）。
-- 取号：2026-10-26 已查两库 MAX(version) = 测试库 20261024.002 / 生产库 20261026.010，
--       共用目录取 V20261026_011。
-- 幂等：INSERT IGNORE；tenant_id 固定 10159（千铝工投运行租户）。

INSERT IGNORE INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
(20261026011, 10159, 'question_resolve_generation_enabled', '题目解析：AI生成总开关(1开0关)',        '0',  'enabled', NOW(), NOW(), 1, 1, 0),
(20261026012, 10159, 'question_resolve_writeback_enabled',  '题目解析：AI结果回写resolve列开关(1开0关)', '0',  'enabled', NOW(), NOW(), 1, 1, 0),
(20261026013, 10159, 'question_resolve_daily_limit',        '题目解析：每学员每日生成上限',           '50', 'enabled', NOW(), NOW(), 1, 1, 0);
