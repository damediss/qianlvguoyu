-- Formal exam realtime outbox. Redis presence/tickets are ephemeral; this table is the durable event fact.
-- payload_json must not contain answers, phone numbers, ID cards, or raw tokens.

CREATE TABLE competition_formal_exam_outbox (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  exam_session_id BIGINT NOT NULL,
  candidate_exam_id BIGINT NULL,
  access_session_id BIGINT NULL,
  generation INT NULL,
  seq INT NOT NULL,
  payload_json TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  published_at DATETIME NULL,
  last_error VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_formal_exam_outbox_status CHECK (
    status IN ('pending', 'publishing', 'published', 'dropped')
  ),
  CONSTRAINT chk_comp_formal_exam_outbox_seq CHECK (seq > 0),
  UNIQUE KEY uk_comp_formal_exam_outbox_event (tenant_id, event_id, deleted),
  KEY idx_comp_formal_exam_outbox_dispatch (
    tenant_id, status, created_at, deleted
  ),
  KEY idx_comp_formal_exam_outbox_session (
    tenant_id, exam_session_id, seq, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='正式比赛实时事件发件箱';
