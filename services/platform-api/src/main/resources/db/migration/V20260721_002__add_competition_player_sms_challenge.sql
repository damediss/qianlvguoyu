CREATE TABLE competition_player_sms_challenge (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  mobile_lookup_hash CHAR(64) NOT NULL,
  code_hash CHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  expires_at DATETIME NOT NULL,
  sent_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  delivery_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  provider_message_id VARCHAR(128) NULL,
  request_ip_hash CHAR(64) NULL,
  request_device_hash CHAR(64) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_player_sms_purpose
    CHECK (purpose IN ('activation', 'login', 'change_mobile_old', 'change_mobile_new')),
  CONSTRAINT chk_competition_player_sms_status
    CHECK (status IN ('pending', 'verified', 'expired', 'locked', 'cancelled')),
  CONSTRAINT chk_competition_player_sms_delivery
    CHECK (delivery_status IN ('pending', 'accepted', 'failed')),
  KEY idx_competition_player_sms_player (tenant_id, player_id, purpose, status, deleted),
  KEY idx_competition_player_sms_mobile_sent (tenant_id, mobile_lookup_hash, purpose, sent_at, deleted),
  KEY idx_competition_player_sms_locked (tenant_id, player_id, purpose, locked_until, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='竞赛选手短信验证码挑战与审计记录';
