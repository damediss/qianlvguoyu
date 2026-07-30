-- 二期：排课分班任务与明细（禁止命名 train_schedule_*，与课表冲突）

CREATE TABLE IF NOT EXISTS train_shift_assign_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL COMMENT '三年规划ID',
  train_info_id BIGINT NULL COMMENT '关联培训信息ID',
  year INT NOT NULL,
  month INT NOT NULL,
  source_shift_file_id BIGINT NULL COMMENT '企业回传排班表文件ID',
  adapter_type VARCHAR(32) NOT NULL DEFAULT 'T0' COMMENT 'T0直通/后续企业Adapter',
  status VARCHAR(32) NOT NULL DEFAULT 'generated' COMMENT 'precheck/generated/patched',
  version INT NOT NULL DEFAULT 1 COMMENT '调课乐观锁',
  warning_json MEDIUMTEXT NULL,
  config_json TEXT NULL,
  unassigned_json MEDIUMTEXT NULL,
  theory_days_json TEXT NULL,
  practice_days_json TEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tsat_plan (tenant_id, plan_id, deleted),
  KEY idx_tsat_train (train_info_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排课分班任务';

CREATE TABLE IF NOT EXISTS train_shift_assign_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  person_name VARCHAR(64) NOT NULL,
  id_card VARCHAR(32) NULL COMMENT '若解析到身份证/证件号',
  employee_no VARCHAR(64) NULL,
  dept VARCHAR(64) NULL,
  shift_class VARCHAR(64) NULL COMMENT '生产班别',
  assign_date DATE NOT NULL,
  slot VARCHAR(16) NOT NULL COMMENT '上午/下午',
  algo_class_name VARCHAR(64) NOT NULL COMMENT '算法班名如上午1班',
  train_class_id BIGINT NULL COMMENT '绑定的培训班级ID',
  row_index INT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tsar_task (tenant_id, task_id, deleted),
  KEY idx_tsar_class (algo_class_name, train_class_id),
  KEY idx_tsar_date (task_id, assign_date, slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排课分班明细';
