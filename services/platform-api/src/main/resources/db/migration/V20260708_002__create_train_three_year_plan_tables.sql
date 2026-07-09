-- 培训管理 · 三年规划主数据表

CREATE TABLE IF NOT EXISTS train_three_year_plan (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  plan_no VARCHAR(32) NOT NULL COMMENT '规划编号',
  title VARCHAR(200) NOT NULL COMMENT '规划标题',
  target_org_id BIGINT NOT NULL COMMENT '目标企业 sys_org.id',
  research_org_id BIGINT NULL COMMENT '关联企业调研 train_research_org.id',
  rencai_task_id BIGINT NULL COMMENT 'rencaishibie analysis_task.id',
  rencai_input_file_id BIGINT NULL COMMENT 'rencaishibie 输入文件 ID',
  plan_years VARCHAR(32) NOT NULL DEFAULT '2026,2027,2028',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1分析中 2待机构审核 3待企业确认 4企业已确认 5已驳回 6已作废',
  analysis_status VARCHAR(32) NULL COMMENT 'pending/running/done/failed',
  reject_reason VARCHAR(500) NULL,
  company_confirm_time DATETIME NULL,
  company_confirm_user_id BIGINT NULL,
  company_confirm_username VARCHAR(64) NULL,
  created_by BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_typ_tenant_org (tenant_id, target_org_id, deleted),
  KEY idx_typ_rencai_task (rencai_task_id),
  KEY idx_typ_status (tenant_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三年培训规划';

CREATE TABLE IF NOT EXISTS train_three_year_plan_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  person_name VARCHAR(64) NULL,
  id_card VARCHAR(32) NULL,
  company_name VARCHAR(128) NULL,
  job_name VARCHAR(128) NULL,
  level_name VARCHAR(32) NULL,
  train_mode VARCHAR(32) NULL COMMENT '自费认定/新型学徒制/岗位技能提升',
  plan_year INT NULL,
  train_year INT NULL,
  cert_year INT NULL,
  eligible_date VARCHAR(32) NULL,
  row_index INT NULL,
  source VARCHAR(16) NOT NULL DEFAULT 'engine',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_typ_item_plan (plan_id, deleted),
  KEY idx_typ_item_year (plan_id, plan_year, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三年规划明细';

CREATE TABLE IF NOT EXISTS train_three_year_plan_matrix (
  id BIGINT NOT NULL AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  job_name VARCHAR(128) NOT NULL,
  plan_year INT NOT NULL,
  train_mode VARCHAR(32) NOT NULL DEFAULT '',
  level_name VARCHAR(32) NOT NULL,
  headcount INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_typ_matrix_cell (plan_id, job_name, plan_year, train_mode, level_name, deleted),
  KEY idx_typ_matrix_plan (plan_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三年规划矩阵汇总';
