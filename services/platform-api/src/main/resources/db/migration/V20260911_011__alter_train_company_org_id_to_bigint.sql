-- 培训域企业/组织 ID：INT → BIGINT（与 sys_org.id 雪花 ID 对齐）
-- 现象：创建 train_info_list 时 MysqlDataTruncation: Out of range value for column 'company_id'
-- gjjddb2026 上相关列已多为 BIGINT；本脚本幂等 MODIFY，双库可安全执行。

ALTER TABLE train_info_list
  MODIFY COLUMN company_id BIGINT NULL COMMENT '企业ID(sys_org.id)';

ALTER TABLE train_info_class
  MODIFY COLUMN company_id BIGINT NOT NULL COMMENT '企业ID(sys_org.id)';

ALTER TABLE train_request
  MODIFY COLUMN company_id BIGINT NULL COMMENT '企业ID';

ALTER TABLE train_solution
  MODIFY COLUMN company_id BIGINT NULL COMMENT '企业ID';

ALTER TABLE train_user_info
  MODIFY COLUMN company_id BIGINT NULL COMMENT '企业ID';

ALTER TABLE train_budget
  MODIFY COLUMN org_id BIGINT NULL COMMENT '企业ID';

ALTER TABLE train_exam_list
  MODIFY COLUMN org_id BIGINT NOT NULL COMMENT '组织ID';

ALTER TABLE train_need
  MODIFY COLUMN org_id BIGINT NULL COMMENT '组织ID';

ALTER TABLE train_talent_needs
  MODIFY COLUMN org_id BIGINT NULL COMMENT '组织ID';
