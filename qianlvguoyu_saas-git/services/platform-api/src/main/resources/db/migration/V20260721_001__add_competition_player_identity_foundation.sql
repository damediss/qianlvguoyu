-- R1 Player activation foundation: identity is isolated from legacy platform_user data.
CREATE TABLE competition_player_identity (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  id_card_ciphertext TEXT NOT NULL,
  id_card_lookup_hash CHAR(64) NOT NULL,
  mobile_ciphertext TEXT NOT NULL,
  mobile_lookup_hash CHAR(64) NOT NULL,
  crypto_key_version VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending_activation',
  activated_at DATETIME NULL,
  disabled_at DATETIME NULL,
  disabled_reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_player_identity_status
    CHECK (status IN ('pending_activation', 'active', 'disabled')),
  UNIQUE KEY uk_competition_player_identity_id_card
    (tenant_id, id_card_lookup_hash, deleted),
  UNIQUE KEY uk_competition_player_identity_mobile
    (tenant_id, mobile_lookup_hash, deleted),
  KEY idx_competition_player_identity_status (tenant_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛选手身份（敏感字段加密存储）';

ALTER TABLE competition_event
  ADD COLUMN player_entry_code VARCHAR(64) NULL COMMENT '选手端安全入口码' AFTER code,
  ADD CONSTRAINT uk_competition_event_player_entry_code UNIQUE (player_entry_code);

ALTER TABLE competition_registration
  ADD COLUMN player_id BIGINT NULL COMMENT '竞赛选手身份 ID' AFTER roster_item_id,
  ADD KEY idx_competition_registration_player_event_status
    (tenant_id, player_id, event_id, status, deleted);

ALTER TABLE competition_roster_item
  ADD COLUMN player_id BIGINT NULL COMMENT '竞赛选手身份 ID' AFTER batch_id,
  ADD KEY idx_competition_roster_item_player (tenant_id, player_id, deleted);

ALTER TABLE competition_registration
  DROP CHECK chk_competition_registration_status,
  ADD CONSTRAINT chk_competition_registration_status CHECK (
    status IN (
      'imported_pending_confirm',
      'pending_audit',
      'supplement_required',
      'rejected',
      'eligible_unactivated',
      'eligible_activated',
      'withdrawn',
      'expired'
    )
  );
