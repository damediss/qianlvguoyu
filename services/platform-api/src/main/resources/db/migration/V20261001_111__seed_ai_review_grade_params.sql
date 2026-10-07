-- 《简答题作答与AI判分方案》§5-3 / §5-10 / Q2：AI 判分参数（幂等）
-- tenant_id 固定 10159（千铝工投运行租户，根 AGENTS.md 种子口径）：
-- App 链 TenantContextFilter 对 /appreviewai/** 与 /appreviewsync/** 强制 10159，
-- 字典查询经多租户插件按 tenant 精确过滤，种 tenant 1 在运行链路上查不到（方案 §7 v1.4 实证）
INSERT IGNORE INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
(20261001111, 10159, 'ai_review_grade_pass_rate',    'AI判分：判对得分率阈值(%)',    '60',  'enabled', NOW(), NOW(), 1, 1, 0),
(20261001112, 10159, 'ai_review_mock_subjective_max','AI判分：模考卷主观题数量上限', '5',   'enabled', NOW(), NOW(), 1, 1, 0),
(20261001113, 10159, 'ai_review_grade_rate_minute',  'AI判分：每用户每分钟限流',     '10',  'enabled', NOW(), NOW(), 1, 1, 0),
(20261001114, 10159, 'ai_review_grade_rate_day',     'AI判分：每用户每日限流',       '200', 'enabled', NOW(), NOW(), 1, 1, 0);
