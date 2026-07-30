CREATE TABLE IF NOT EXISTS certification_practical_collection_status (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  session_id BIGINT NULL,
  collect_token VARCHAR(128) NULL,
  qr_issue_status VARCHAR(32) NOT NULL DEFAULT 'NOT_ISSUED',
  overall_status VARCHAR(32) NOT NULL DEFAULT 'NOT_ARRIVED',
  queue_status VARCHAR(32) NOT NULL DEFAULT 'NOT_QUEUED',
  fingerprint_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
  signature_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
  exception_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
  match_method VARCHAR(32) NULL,
  queue_no INT NULL,
  id_card_last4 VARCHAR(8) NULL,
  token_generated_at DATETIME NULL,
  qr_exported_at DATETIME NULL,
  qr_issued_at DATETIME NULL,
  arrived_at DATETIME NULL,
  collecting_started_at DATETIME NULL,
  completed_at DATETIME NULL,
  last_operator_id BIGINT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  active_candidate_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN candidate_id ELSE NULL END) VIRTUAL,
  active_collect_token VARCHAR(128)
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN collect_token ELSE NULL END) VIRTUAL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_practical_collect_status_candidate (
    tenant_id, task_id, active_candidate_id
  ),
  UNIQUE KEY uk_cert_practical_collect_status_token (
    tenant_id, active_collect_token
  ),
  KEY idx_cert_practical_collect_status_task (tenant_id, task_id, deleted),
  KEY idx_cert_practical_collect_status_overall (tenant_id, task_id, overall_status, deleted),
  KEY idx_cert_practical_collect_status_queue (tenant_id, task_id, queue_status, deleted),
  KEY idx_cert_practical_collect_status_last4 (tenant_id, task_id, id_card_last4, deleted),
  KEY idx_cert_practical_collect_status_candidate (tenant_id, candidate_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='practical collection status';

CREATE TABLE IF NOT EXISTS certification_practical_collection_queue (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  status_id BIGINT NULL,
  queue_no INT NOT NULL,
  queue_status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
  match_method VARCHAR(32) NOT NULL,
  joined_at DATETIME NOT NULL,
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  removed_at DATETIME NULL,
  operator_id BIGINT NULL,
  remove_reason VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  active_queue_candidate_id BIGINT
    GENERATED ALWAYS AS (
      CASE
        WHEN deleted = 0 AND queue_status IN ('QUEUED', 'COLLECTING') THEN candidate_id
        ELSE NULL
      END
    ) VIRTUAL,
  active_queue_no INT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN queue_no ELSE NULL END) VIRTUAL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_practical_collect_queue_candidate (
    tenant_id, task_id, active_queue_candidate_id
  ),
  UNIQUE KEY uk_cert_practical_collect_queue_no (
    tenant_id, task_id, active_queue_no
  ),
  KEY idx_cert_practical_collect_queue_status (tenant_id, task_id, queue_status, deleted),
  KEY idx_cert_practical_collect_queue_joined (tenant_id, task_id, joined_at, deleted),
  KEY idx_cert_practical_collect_queue_candidate (tenant_id, task_id, candidate_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='practical collection live queue';

CREATE TABLE IF NOT EXISTS certification_practical_fingerprint_trace (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  session_id BIGINT NULL,
  device_sn VARCHAR(128) NULL,
  device_model VARCHAR(128) NULL,
  adapter_type VARCHAR(32) NOT NULL DEFAULT 'MOCK',
  collect_status VARCHAR(32) NOT NULL,
  quality_score DECIMAL(6, 2) NULL,
  error_code VARCHAR(64) NULL,
  exception_reason VARCHAR(500) NULL,
  collected_by BIGINT NULL,
  collected_at DATETIME NOT NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_practical_fingerprint_candidate (tenant_id, task_id, candidate_id, deleted),
  KEY idx_cert_practical_fingerprint_status (tenant_id, task_id, collect_status, deleted),
  KEY idx_cert_practical_fingerprint_time (tenant_id, task_id, collected_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='practical fingerprint trace';

CREATE TABLE IF NOT EXISTS certification_practical_signature_record (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  session_id BIGINT NULL,
  signature_file_id BIGINT NOT NULL,
  signature_hash CHAR(64) NOT NULL,
  signer_type VARCHAR(32) NOT NULL,
  signer_name VARCHAR(64) NOT NULL,
  signed_at DATETIME NOT NULL,
  device_info JSON NULL,
  record_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_practical_signature_candidate (tenant_id, task_id, candidate_id, deleted),
  KEY idx_cert_practical_signature_file (tenant_id, signature_file_id, deleted),
  KEY idx_cert_practical_signature_time (tenant_id, task_id, signed_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='practical signature record';

CREATE TABLE IF NOT EXISTS certification_practical_collection_exception (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  exception_type VARCHAR(32) NOT NULL,
  exception_status VARCHAR(32) NOT NULL,
  release_scope VARCHAR(32) NULL,
  reason VARCHAR(1000) NOT NULL,
  device_sn VARCHAR(128) NULL,
  device_status VARCHAR(32) NULL,
  handled_by BIGINT NULL,
  handled_at DATETIME NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cert_practical_collect_exception_task (tenant_id, task_id, exception_status, deleted),
  KEY idx_cert_practical_collect_exception_candidate (tenant_id, task_id, candidate_id, deleted),
  KEY idx_cert_practical_collect_exception_type (tenant_id, task_id, exception_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='practical collection exception';
