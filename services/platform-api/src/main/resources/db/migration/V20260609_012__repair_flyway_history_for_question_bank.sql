-- ============================================================
-- Flyway Schema History 修复脚本（题库迁移）
-- 背景: 题库相关 SQL 可能在部分环境已手动执行,
--       需要补录 flyway_schema_history 记录防止 Flyway 重复执行。
-- 幂等: 按 version 去重，仅当 version 不存在时才补录。
-- ============================================================

-- 补录 V20260609_010 的执行记录 (仅当 version 不存在时)
INSERT INTO flyway_schema_history (
  installed_rank, version, description, type, script, checksum,
  installed_by, installed_on, execution_time, success
)
SELECT
  (SELECT IFNULL(MAX(fsh.installed_rank), 0) + 1
   FROM flyway_schema_history fsh),
  '20260609.010',
  'create exam knowledge structure and exam detail',
  'SQL',
  'V20260609_010__create_exam_knowledge_structure_and_exam_detail.sql',
  0, USER(), NOW(), 0, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM flyway_schema_history WHERE version = '20260609.010'
);

-- 补录 V20260609_011 的执行记录 (仅当 version 不存在时)
INSERT INTO flyway_schema_history (
  installed_rank, version, description, type, script, checksum,
  installed_by, installed_on, execution_time, success
)
SELECT
  (SELECT IFNULL(MAX(fsh.installed_rank), 0) + 1
   FROM flyway_schema_history fsh),
  '20260609.011',
  'seed question bank page and permissions',
  'SQL',
  'V20260609_011__seed_question_bank_page_and_permissions.sql',
  0, USER(), NOW(), 0, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM flyway_schema_history WHERE version = '20260609.011'
);
