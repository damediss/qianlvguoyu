-- 理论知识试题生成：命题明细表加列（溯源 + 知识点打通）
-- 方案：docs/04_requirements/理论知识试题生成管理方案.md v1.2 §6.3（T1 迁移 2）
-- 表名为 RENAME 后真实表名（V20260623_001）；难度复用既有 nanyicd 列，不新增 difficulty
-- 版本号依据（2026-09-04 双库查询）：gjjddbpro(生产) MAX=20260831.001；
-- gjjddb2026(测试) 的 20260904.001/.002 已被 train_certificate 工作流占用，故取 .010~.013（踩坑 #7）。
-- 幂等说明：gjjddb2026 上同名列已由 train_certificate 迁移先行添加（类型与本文件一致），
-- 故全部采用 information_schema 条件化 ADD COLUMN；生产库（gjjddbpro）列不存在时会实际执行添加。

-- 1) task_exam_detail：knowledge_id / ai_batch_id / source
SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_exam_detail' AND COLUMN_NAME = 'knowledge_id');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_exam_detail ADD COLUMN knowledge_id BIGINT NULL COMMENT ''考评点 exam_knowledge_structure.id''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_exam_detail' AND COLUMN_NAME = 'ai_batch_id');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_exam_detail ADD COLUMN ai_batch_id BIGINT NULL COMMENT ''AI生成批次 exam_theory_gen_batch.id''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_exam_detail' AND COLUMN_NAME = 'source');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_exam_detail ADD COLUMN source TINYINT NOT NULL DEFAULT 0 COMMENT ''0手工 1导入 2AI生成''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) task_examsub_detail：exam_type / knowledge_id / score_points / scoring_standard / ai_batch_id / source
SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub_detail' AND COLUMN_NAME = 'exam_type');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examsub_detail ADD COLUMN exam_type VARCHAR(50) NULL COMMENT ''简答题/计算题/绘图题/论述题/案例分析题''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub_detail' AND COLUMN_NAME = 'knowledge_id');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examsub_detail ADD COLUMN knowledge_id BIGINT NULL COMMENT ''考评点 exam_knowledge_structure.id''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub_detail' AND COLUMN_NAME = 'score_points');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examsub_detail ADD COLUMN score_points TEXT NULL COMMENT ''评分要点''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub_detail' AND COLUMN_NAME = 'scoring_standard');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examsub_detail ADD COLUMN scoring_standard TEXT NULL COMMENT ''评分标准''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub_detail' AND COLUMN_NAME = 'ai_batch_id');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examsub_detail ADD COLUMN ai_batch_id BIGINT NULL COMMENT ''AI生成批次 exam_theory_gen_batch.id''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examsub_detail' AND COLUMN_NAME = 'source');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examsub_detail ADD COLUMN source TINYINT NOT NULL DEFAULT 0 COMMENT ''0手工 1导入 2AI生成''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
