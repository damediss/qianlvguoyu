-- 三年规划：新增驳回历史 + 提交历史字段，支持驳回后重新提交并保留记录

ALTER TABLE train_three_year_plan
  ADD COLUMN reject_history_json TEXT NULL COMMENT '驳回历史 JSON：[{reason, rejectedBy, rejectedAt}]'
  AFTER reject_reason;

ALTER TABLE train_three_year_plan
  ADD COLUMN submit_history_json TEXT NULL COMMENT '提交历史 JSON：[{submittedBy, submittedAt, personnelFileName, schemeFileName}]'
  AFTER reject_history_json;
