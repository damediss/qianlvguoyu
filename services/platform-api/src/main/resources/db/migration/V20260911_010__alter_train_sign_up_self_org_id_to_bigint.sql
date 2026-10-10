-- 开班准备资料收集：org_id INT → BIGINT（与 train_info_class.company_id / sys_org.id 雪花 ID 对齐）
-- 现象：ensure-collection 写入 company_id=2098... 时 MysqlDataTruncation: Out of range value for column 'org_id'
ALTER TABLE train_sign_up_self
  MODIFY COLUMN org_id BIGINT NOT NULL DEFAULT 0 COMMENT '组织ID(sys_org.id/班级company_id)';
