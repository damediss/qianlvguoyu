-- 分班一期：待分班池

CREATE TABLE IF NOT EXISTS train_plan_roster (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL COMMENT '三年规划ID',
  train_info_id BIGINT NULL COMMENT '关联培训信息ID',
  source VARCHAR(32) NOT NULL COMMENT 'enterprise_roster/plan_item/manual',
  person_name VARCHAR(64) NOT NULL,
  id_card VARCHAR(32) NULL,
  phone VARCHAR(32) NULL,
  job_name VARCHAR(128) NULL,
  level_name VARCHAR(32) NULL,
  workshop VARCHAR(64) NULL COMMENT '车间',
  shift_team VARCHAR(32) NULL COMMENT '生产班组',
  plan_year INT NULL,
  train_mode VARCHAR(32) NULL,
  assign_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待分 1已分班 2排除',
  train_class_id BIGINT NULL,
  row_json TEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tpr_plan (tenant_id, plan_id, assign_status, deleted),
  KEY idx_tpr_idcard (tenant_id, id_card),
  KEY idx_tpr_train_info (train_info_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三年规划待分班池';
