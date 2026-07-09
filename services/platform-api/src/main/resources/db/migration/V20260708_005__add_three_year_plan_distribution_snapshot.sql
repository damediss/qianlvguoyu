ALTER TABLE train_three_year_plan
  ADD COLUMN distribution_snapshot TEXT NULL COMMENT '认定分布快照 JSON' AFTER analysis_status;
