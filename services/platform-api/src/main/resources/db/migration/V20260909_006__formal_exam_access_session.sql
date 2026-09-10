-- Formal exam AccessSession + command audit.
-- Token fingerprint is SHA-256 hex; raw tokens are never stored.
-- One ACTIVE row per CandidateExam is enforced in the application lock, not a partial unique index.

CREATE TABLE competition_formal_exam_access_session (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  candidate_exam_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  generation INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  login_at DATETIME NOT NULL,
  last_seen_at DATETIME NULL,
  ended_at DATETIME NULL,
  ended_reason VARCHAR(64) NULL,
  token_fingerprint VARCHAR(64) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_exam_access_status CHECK (
    status IN ('active', 'kicked', 'logged_out', 'expired', 'replaced')
  ),
  CONSTRAINT chk_comp_formal_exam_access_generation CHECK (generation > 0),
  KEY idx_comp_formal_exam_access_candidate (
    tenant_id, candidate_exam_id, status, deleted
  ),
  KEY idx_comp_formal_exam_access_player (
    tenant_id, player_id, status, deleted
  ),
  KEY idx_comp_formal_exam_access_session (
    tenant_id, exam_session_id, status, deleted
  ),
  KEY idx_comp_formal_exam_access_fp (
    tenant_id, token_fingerprint, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛选手接入会话';

CREATE TABLE competition_formal_exam_access_command (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  candidate_exam_id BIGINT NOT NULL,
  access_session_id BIGINT NOT NULL,
  generation INT NOT NULL,
  command_type VARCHAR(32) NOT NULL,
  reason VARCHAR(500) NULL,
  operator_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  ack_at DATETIME NULL,
  expires_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_exam_access_cmd_type CHECK (
    command_type IN ('force_offline', 'login_replace', 'logout')
  ),
  CONSTRAINT chk_comp_formal_exam_access_cmd_status CHECK (
    status IN ('issued', 'acked', 'expired')
  ),
  KEY idx_comp_formal_exam_access_cmd_target (
    tenant_id, access_session_id, generation, deleted
  ),
  KEY idx_comp_formal_exam_access_cmd_session (
    tenant_id, exam_session_id, command_type, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛接入会话指令与审计';
