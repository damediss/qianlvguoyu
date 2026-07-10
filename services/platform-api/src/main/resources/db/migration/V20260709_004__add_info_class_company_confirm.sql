ALTER TABLE train_info_class
  ADD COLUMN company_confirm_status TINYINT NOT NULL DEFAULT 0 COMMENT '企业确认 0待确认 1已确认 2已驳回' AFTER kcbiao_file,
  ADD COLUMN company_confirm_time DATETIME NULL COMMENT '企业确认时间' AFTER company_confirm_status,
  ADD COLUMN company_confirm_user_id BIGINT NULL COMMENT '企业确认人' AFTER company_confirm_time,
  ADD COLUMN company_confirm_username VARCHAR(64) NULL COMMENT '企业确认人姓名' AFTER company_confirm_user_id,
  ADD COLUMN company_reject_reason VARCHAR(500) NULL COMMENT '企业驳回理由' AFTER company_confirm_username;
