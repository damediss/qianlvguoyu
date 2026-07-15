ALTER TABLE train_three_year_plan
  ADD COLUMN submit_personnel_file_id BIGINT NULL COMMENT '提交企业审核时上传的审核人员名单文件ID' AFTER reviewed_plan_file_id,
  ADD COLUMN submit_scheme_file_id BIGINT NULL COMMENT '提交企业审核时上传的方案文档文件ID' AFTER submit_personnel_file_id;
