-- 认定模块配置考评表导入的评分要素可能超过 VARCHAR(200)，改用 TEXT 保存。
-- 版本依据：gjjddb2026(测试) MAX(version)=20260920.104；gjjddbpro(生产) MAX(version)=20260920.101。

ALTER TABLE certification_assessor_task_score
  MODIFY COLUMN step_name TEXT NOT NULL COMMENT '评分要素';
