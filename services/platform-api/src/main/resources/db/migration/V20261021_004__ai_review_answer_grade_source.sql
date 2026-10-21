-- 主观题无意义作答拦截（方案 docs/04_requirements/主观题无意义作答拦截与学员引导方案.md §3.2/§3.4）：
-- ai_review_answer 增加判分来源列（gradeSource 此前未落库，回放恒 null，拦截判 0 的可重答性
-- 刷新即丢失）；字典种子：闸口总开关（默认开）+ R3 汉字数阈值。
-- 取号：2026-10-21 已查两库 MAX(version) = 测试库 20261021.003 / 生产库 20261021.002，
--       共用目录取 V20261021_004（撞号会导致 jar 全环境无法启动）。
-- 幂等：列存在则跳过（information_schema 守卫 + PREPARE，踩坑 #34 手法）；字典 INSERT IGNORE。

SET @col_gs := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_review_answer' AND COLUMN_NAME = 'grade_source');
SET @ddl := IF(@col_gs = 0,
  'ALTER TABLE ai_review_answer ADD COLUMN grade_source VARCHAR(16) NULL COMMENT ''判分来源 ai/self/rule/skip（rule=无意义作答拦截，skip=主动放弃看解析）'' AFTER is_correct',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tenant_id 固定 10159（千铝工投运行租户，根 AGENTS.md 种子口径）：
-- App 链 TenantContextFilter 对 /appreviewai/** 与 /appreviewsync/** 强制 10159，
-- 字典查询经多租户插件按 tenant 精确过滤，种 tenant 1 在运行链路上查不到。
INSERT IGNORE INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
(20261021041, 10159, 'ai_review_grade_guard_enabled',  'AI判分：无意义作答闸口开关(1开0关)', '1', 'enabled', NOW(), NOW(), 1, 1, 0),
(20261021042, 10159, 'ai_review_grade_guard_min_hanzi','AI判分：无意义作答R3汉字数阈值',     '4', 'enabled', NOW(), NOW(), 1, 1, 0);
