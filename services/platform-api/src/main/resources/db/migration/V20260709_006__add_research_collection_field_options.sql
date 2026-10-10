-- 调研企业人员采集字段下拉选项配置

ALTER TABLE train_research_org
  ADD COLUMN collection_field_options TEXT NULL COMMENT '人员采集字段下拉选项 JSON' AFTER personnel_count;

ALTER TABLE train_sign_up_self
  ADD COLUMN collection_field_options TEXT NULL COMMENT '开启采集时的字段选项快照 JSON' AFTER qrcode_token;
