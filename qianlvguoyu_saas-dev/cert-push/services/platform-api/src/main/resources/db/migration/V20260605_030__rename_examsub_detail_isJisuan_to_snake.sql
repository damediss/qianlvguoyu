-- ===========================================================================
-- 修正: exam_task_examsub_detail.isJisuan 列名(migration 写成了 camelCase,
--       MyBatis-Plus 默认按 snake_case 拼 SQL,所以 service 一插就报
--       "Unknown column 'is_jisuan'")。
-- 把列重命名为 snake_case 风格的 is_jisuan,跟同一张表其他列(nanyicd / isblod /
-- zhishidian / tenant_id 等)以及 MyBatis-Plus 默认命名约定保持一致。
-- ===========================================================================

ALTER TABLE exam_task_examsub_detail
  CHANGE COLUMN isJisuan is_jisuan INT NOT NULL DEFAULT 0 COMMENT '是否计算题 0否 1是';
