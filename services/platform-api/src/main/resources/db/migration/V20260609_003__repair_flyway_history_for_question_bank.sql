-- ============================================================
-- Flyway Schema History 修复脚本
-- 背景: V20260609_001 和 V20260609_002 的 SQL 已在当前库手动执行,
--       需要补录 flyway_schema_history 记录防止 Flyway 重复执行;
--       同时清理不在代码库中的孤儿记录。
-- 说明: 项目配置 validate-on-migrate=false, checksum=0 不会触发校验。
-- 幂等保证: 使用 NOT EXISTS 子查询按 version 去重,
--   - 当前库: .001/.002 已由 Flyway 记录 → NOT EXISTS 跳过
--   - 全新库: .001/.002 已由 Flyway 正常执行并记录 → NOT EXISTS 跳过
--   - 仅当 version 不存在时才补录
-- ============================================================

-- 1. 清理代码库中不存在的孤儿迁移记录
--    (手动执行 V001/V002 时 Flyway 自动生成的 .003/.004 记录, 对应 .sql 未提交)
DELETE FROM flyway_schema_history
WHERE version IN ('20260609.003', '20260609.004');

-- 2. 补录 V20260609_001 的执行记录 (仅当 version 不存在时)
INSERT INTO flyway_schema_history (
  installed_rank, version, description, type, script, checksum,
  installed_by, installed_on, execution_time, success
)
SELECT
  (SELECT IFNULL(MAX(fsh.installed_rank), 0) + 1
   FROM flyway_schema_history fsh),
  '20260609.001',
  'create exam knowledge structure and exam detail',
  'SQL',
  'V20260609_001__create_exam_knowledge_structure_and_exam_detail.sql',
  0, USER(), NOW(), 0, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM flyway_schema_history WHERE version = '20260609.001'
);

-- 3. 补录 V20260609_002 的执行记录 (仅当 version 不存在时)
INSERT INTO flyway_schema_history (
  installed_rank, version, description, type, script, checksum,
  installed_by, installed_on, execution_time, success
)
SELECT
  (SELECT IFNULL(MAX(fsh.installed_rank), 0) + 1
   FROM flyway_schema_history fsh),
  '20260609.002',
  'seed question bank page and permissions',
  'SQL',
  'V20260609_002__seed_question_bank_page_and_permissions.sql',
  0, USER(), NOW(), 0, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM flyway_schema_history WHERE version = '20260609.002'
);
