-- 为 phalcon 兼容添加的列（saas 不使用这些列，仅保证 phalcon 写入不报错）

-- task_exam_audit_records: 添加 uptime 和 exam_idea
ALTER TABLE task_exam_audit_records
  ADD COLUMN `exam_idea` text DEFAULT NULL COMMENT '解析/备注(phalcon用)',
  ADD COLUMN `uptime` int NOT NULL DEFAULT 0 COMMENT '更新时间戳(phalcon用)';

-- task_examshicao_audit_records: 添加 uptime
ALTER TABLE task_examshicao_audit_records
  ADD COLUMN `uptime` int NOT NULL DEFAULT 0 COMMENT '更新时间戳(phalcon用)';

-- task_examsub_audit_records: 添加 uptime
ALTER TABLE task_examsub_audit_records
  ADD COLUMN `uptime` int NOT NULL DEFAULT 0 COMMENT '更新时间戳(phalcon用)';

-- task_ximu_audit_records: 添加 uptime
ALTER TABLE task_ximu_audit_records
  ADD COLUMN `uptime` int NOT NULL DEFAULT 0 COMMENT '更新时间戳(phalcon用)';

-- task_exam_detail.level: int → varchar(20) 与 phalcon 统一
ALTER TABLE task_exam_detail
  MODIFY COLUMN `level` varchar(20) DEFAULT NULL COMMENT '级别';
