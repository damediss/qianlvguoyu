CREATE TABLE IF NOT EXISTS certification_exam_session (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  task_id BIGINT NOT NULL,
  task_scope_id BIGINT NULL,
  exam_type VARCHAR(20) NOT NULL COMMENT 'THEORY or PRACTICE',
  session_name VARCHAR(160) NOT NULL,
  exam_date DATE NULL,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  exam_site_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  identity_check_required TINYINT NOT NULL DEFAULT 1,
  fingerprint_required TINYINT NOT NULL DEFAULT 0,
  signature_required TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  active_session_name VARCHAR(160)
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN session_name ELSE NULL END) VIRTUAL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_exam_session_active_name (tenant_id, task_id, active_session_name),
  KEY idx_cert_exam_session_task (tenant_id, task_id, deleted),
  KEY idx_cert_exam_session_scope_type (tenant_id, task_id, task_scope_id, exam_type, deleted),
  KEY idx_cert_exam_session_time (tenant_id, exam_date, start_time, end_time, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='certification exam session';

CREATE TABLE IF NOT EXISTS certification_candidate_exam_session (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  task_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  participation_status VARCHAR(24) NOT NULL DEFAULT 'ASSIGNED',
  collection_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  collection_source_session_id BIGINT NULL,
  exempt_reason VARCHAR(500) NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted INT NOT NULL DEFAULT 0,
  active_candidate_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN candidate_id ELSE NULL END) VIRTUAL,
  active_exam_session_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN exam_session_id ELSE NULL END) VIRTUAL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_candidate_exam_session_active (tenant_id, active_candidate_id, active_exam_session_id),
  KEY idx_cert_candidate_exam_session_task (tenant_id, task_id, deleted),
  KEY idx_cert_candidate_exam_session_session (tenant_id, exam_session_id, participation_status, deleted),
  KEY idx_cert_candidate_exam_session_candidate (tenant_id, candidate_id, participation_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='candidate exam session participation';

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (6990, 1, 6301, 'certification.preparation.exam-session.view', 'View certification exam sessions', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6991, 1, 6301, 'certification.preparation.exam-session.edit', 'Edit certification exam sessions', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6992, 1, 6301, 'certification.preparation.exam-session.delete', 'Delete certification exam sessions', 'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (6993, 1, 6301, 'certification.preparation.candidate.exam-session.edit', 'Edit candidate exam sessions', 'edit', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 969900 + p.id, 1, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.id BETWEEN 6990 AND 6993;
