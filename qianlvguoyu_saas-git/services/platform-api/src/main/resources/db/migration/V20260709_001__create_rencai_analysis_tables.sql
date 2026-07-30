-- rencaishibie 人才识别引擎表（与 qualification_mvp 结构一致，共库 gjjddb2026）
-- DDL 仅由 Flyway 管理；Python 侧只做 DML，不再对 gjjddb2026 执行 Alembic

CREATE TABLE IF NOT EXISTS analysis_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(1000) NOT NULL,
  file_type VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-分析文件';

CREATE TABLE IF NOT EXISTS analysis_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status VARCHAR(32) NOT NULL,
  total_rows INT NOT NULL DEFAULT 0,
  recommended_rows INT NOT NULL DEFAULT 0,
  not_recommended_rows INT NOT NULL DEFAULT 0,
  pending_manual_rows INT NOT NULL DEFAULT 0,
  strict_failed_rows INT NOT NULL DEFAULT 0,
  error_message TEXT NULL,
  input_file_id BIGINT NOT NULL,
  output_file_id BIGINT NULL,
  review_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
  reviewed_at DATETIME NULL,
  finalized_at DATETIME NULL,
  tenant_id BIGINT NULL COMMENT 'platform 租户 ID',
  platform_plan_id BIGINT NULL COMMENT 'train_three_year_plan.id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_analysis_task_input (input_file_id),
  KEY idx_analysis_task_tenant (tenant_id),
  KEY idx_analysis_task_plan (platform_plan_id),
  KEY idx_analysis_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-分析任务';

CREATE TABLE IF NOT EXISTS analysis_pending_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  row_index INT NOT NULL,
  person_name VARCHAR(255) NOT NULL,
  recommended_level VARCHAR(64) NOT NULL,
  eligible_date DATETIME NULL,
  reason TEXT NOT NULL,
  candidate_jobs_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_api_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-待人工选择';

CREATE TABLE IF NOT EXISTS analysis_manual_selection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  pending_item_id BIGINT NOT NULL,
  selected_job VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ams_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-人工选择结果';

CREATE TABLE IF NOT EXISTS job_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_name VARCHAR(255) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-工种配置';

CREATE TABLE IF NOT EXISTS related_major_mapping (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_name VARCHAR(255) NOT NULL,
  major_name VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-相关专业映射';

CREATE TABLE IF NOT EXISTS rule_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  version_name VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-规则版本';

CREATE TABLE IF NOT EXISTS job_class_threshold (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_name VARCHAR(255) NOT NULL,
  level_name VARCHAR(64) NOT NULL,
  min_students INT NOT NULL DEFAULT 15,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rencai_job_level (job_name, level_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-开班阈值';

CREATE TABLE IF NOT EXISTS self_certification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  row_index INT NOT NULL,
  person_name VARCHAR(255) NOT NULL,
  original_work_start DATE NULL,
  claimed_work_start DATE NOT NULL,
  supplementary_years DECIMAL(4,1) NOT NULL,
  evidence_description TEXT NOT NULL,
  evidence_file_path VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  reviewed_by VARCHAR(255) NULL,
  reviewed_at DATETIME NULL,
  review_comment TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sc_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-工作年限自证';

CREATE TABLE IF NOT EXISTS review_adjustment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  row_index INT NOT NULL,
  person_name VARCHAR(255) NOT NULL,
  adjustment_type VARCHAR(32) NOT NULL,
  original_job_name VARCHAR(255) NULL,
  original_level VARCHAR(64) NULL,
  original_eligible_date DATE NULL,
  new_job_name VARCHAR(255) NULL,
  new_level VARCHAR(64) NULL,
  new_eligible_date DATE NULL,
  new_reason TEXT NULL,
  instructor_name VARCHAR(255) NULL,
  adjustment_reason TEXT NOT NULL,
  applied_to_excel TINYINT(1) NOT NULL DEFAULT 0,
  created_by VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ra_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-审核调整';

CREATE TABLE IF NOT EXISTS analysis_result_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  row_index INT NOT NULL,
  person_name VARCHAR(255) NOT NULL,
  person_id_number VARCHAR(32) NULL,
  company VARCHAR(255) NULL,
  original_job VARCHAR(255) NULL,
  actual_job VARCHAR(255) NULL,
  major VARCHAR(255) NULL,
  education_level VARCHAR(128) NULL,
  work_start DATE NULL,
  determined_job VARCHAR(255) NULL,
  determined_level VARCHAR(64) NULL,
  eligible_date DATE NULL,
  status VARCHAR(32) NULL,
  reason TEXT NULL,
  certificate_count INT NULL,
  context_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ari_task_job (task_id, determined_job),
  KEY idx_ari_task_level (task_id, determined_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rencai-分析结果明细';
