-- 认定筹备：CSV 考生导入、认定班级归属与来源快照扩展
ALTER TABLE certification_candidate
  ADD COLUMN task_scope_id BIGINT NULL COMMENT '认定班级（任务工种等级范围）id' AFTER task_id,
  ADD KEY idx_cert_candidate_scope (tenant_id, task_id, task_scope_id, deleted);

ALTER TABLE certification_candidate_import_batch
  ADD COLUMN task_scope_id BIGINT NULL COMMENT '认定班级（任务工种等级范围）id' AFTER task_id,
  ADD KEY idx_cert_candidate_batch_scope (tenant_id, task_id, task_scope_id, created_at, deleted);

ALTER TABLE certification_candidate_source_staging
  ADD COLUMN task_scope_id BIGINT NULL COMMENT '认定班级（任务工种等级范围）id' AFTER task_id,
  ADD COLUMN import_batch_id BIGINT NULL COMMENT 'CSV 导入批次id' AFTER candidate_id,
  ADD COLUMN graduation_certificate_no VARCHAR(160) NULL COMMENT '毕业证编号或入学时间' AFTER major,
  ADD COLUMN political_status VARCHAR(80) NULL COMMENT '政治面貌' AFTER nation,
  ADD COLUMN position_name VARCHAR(160) NULL COMMENT '所在岗位' AFTER company_name,
  ADD COLUMN entry_date DATE NULL COMMENT '入职时间' AFTER work_start_date,
  ADD COLUMN labor_contract_start_date DATE NULL COMMENT '劳动合同签订开始时间' AFTER entry_date,
  ADD COLUMN labor_contract_end_date DATE NULL COMMENT '劳动合同签订结束时间' AFTER labor_contract_start_date,
  ADD COLUMN responsible_name VARCHAR(80) NULL COMMENT '负责人' AFTER labor_contract_end_date,
  ADD COLUMN responsible_phone VARCHAR(32) NULL COMMENT '负责人电话' AFTER responsible_name,
  ADD KEY idx_cert_candidate_staging_batch (tenant_id, import_batch_id, deleted),
  ADD KEY idx_cert_candidate_staging_scope (tenant_id, task_id, task_scope_id, deleted);
