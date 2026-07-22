CREATE TABLE competition_outbox_event (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  payload_json TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  attempt_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  last_error VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_outbox_status
    CHECK (status IN ('pending', 'publishing', 'published', 'retry', 'failed')),
  KEY idx_competition_outbox_dispatch (status, next_attempt_at, created_at, deleted),
  KEY idx_competition_outbox_aggregate (tenant_id, aggregate_type, aggregate_id, event_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='竞赛领域事件可靠投递 Outbox';

CREATE TABLE competition_player_message (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  notice_id BIGINT NOT NULL,
  type VARCHAR(64) NOT NULL,
  received_at DATETIME NOT NULL,
  read_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_competition_player_message_type
    CHECK (type IN ('notice_published')),
  UNIQUE KEY uk_competition_player_message_notice (tenant_id, notice_id, player_id, deleted),
  KEY idx_competition_player_message_inbox (tenant_id, player_id, read_at, received_at, deleted),
  KEY idx_competition_player_message_event (tenant_id, event_id, received_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='竞赛选手站内消息收件箱';
