-- 操作技能试题生成：实操命题明细表加列（溯源标记）
-- 方案：docs/04_requirements/操作技能试题生成管理方案.md v1.1 §6.3（T1 迁移 2，2 列）
-- 一期不写知识点树（方案 C2/S1）：不加 knowledge_id（二期打通时以新迁移补列）。
-- 版本号依据（2026-09-10 双库查询）：gjjddb2026(测试) MAX=20260909.004；gjjddbpro(生产) MAX=20260909.003，
-- 本组迁移取 20260910.010~013（踩坑 #7）。
-- 条件化 ADD COLUMN（information_schema 判重），两库幂等；PREPARE 依赖 Flyway 独立直连（踩坑 #25）。

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examshicao_detail' AND COLUMN_NAME = 'ai_batch_id');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examshicao_detail ADD COLUMN ai_batch_id BIGINT NULL COMMENT ''AI生成批次 exam_skill_gen_batch.id''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_examshicao_detail' AND COLUMN_NAME = 'source');
SET @ddl = (SELECT IF(@has_col = 0,
  'ALTER TABLE task_examshicao_detail ADD COLUMN source TINYINT NOT NULL DEFAULT 0 COMMENT ''0手工 1导入 2AI生成''',
  'SELECT 1'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
