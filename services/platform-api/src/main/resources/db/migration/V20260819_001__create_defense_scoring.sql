CREATE TABLE IF NOT EXISTS certification_defense_config (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  group_id BIGINT NOT NULL COMMENT '综合答辩考评组id',
  status VARCHAR(40) NOT NULL DEFAULT 'configured' COMMENT 'configured/ready',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_defense_config (tenant_id, task_id, deleted),
  KEY idx_cert_defense_config_group (tenant_id, group_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='综合答辩配置';

CREATE TABLE IF NOT EXISTS certification_defense_report (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL COMMENT '认定任务id',
  candidate_id BIGINT NOT NULL COMMENT '考生id',
  group_id BIGINT NULL COMMENT '绑定考评组id',
  file_id BIGINT NOT NULL COMMENT '业绩评估报告文件id',
  file_name VARCHAR(300) NULL COMMENT '原始文件名',
  match_key VARCHAR(80) NULL COMMENT '匹配关键字(身份证/姓名/准考证)',
  status VARCHAR(40) NOT NULL DEFAULT 'uploaded' COMMENT 'uploaded',
  uploaded_at DATETIME NULL COMMENT '上传时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_defense_report (tenant_id, task_id, candidate_id, deleted),
  KEY idx_cert_defense_report_file (tenant_id, file_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='综合答辩业绩评估报告';
