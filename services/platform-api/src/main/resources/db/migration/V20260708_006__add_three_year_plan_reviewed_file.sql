ALTER TABLE train_three_year_plan
  ADD COLUMN reviewed_plan_file_id BIGINT NULL COMMENT '提交企业审核时生成的已审核规划表文件ID' AFTER distribution_snapshot;
