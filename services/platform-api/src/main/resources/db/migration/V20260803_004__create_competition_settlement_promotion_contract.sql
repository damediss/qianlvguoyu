-- COMP-STAGE-01F: settlement/promotion snapshots and auditable stage unlock events.

CREATE TABLE competition_stage_settlement (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  score_rule_id BIGINT NOT NULL,
  settlement_version INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  readiness_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  ranking_snapshot_id BIGINT NULL,
  promotion_snapshot_id BIGINT NULL,
  published_at DATETIME NULL,
  published_by BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_settlement_version CHECK (settlement_version > 0),
  CONSTRAINT chk_comp_stage_settlement_status CHECK (status IN ('draft', 'published', 'withdrawn')),
  CONSTRAINT chk_comp_stage_settlement_readiness CHECK (readiness_status IN ('pending', 'blocked', 'passed')),
  UNIQUE KEY uk_comp_stage_settlement_version (
    tenant_id, plan_id, settlement_version, deleted
  ),
  KEY idx_comp_stage_settlement_scope (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_settlement_item (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  settlement_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  total_score_snapshot_id BIGINT NULL,
  total_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  contestant_status VARCHAR(32) NOT NULL,
  display_rank INT NULL,
  tied_flag TINYINT NOT NULL DEFAULT 0,
  promotion_status VARCHAR(32) NOT NULL DEFAULT 'not_selected',
  reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_settlement_item_status CHECK (contestant_status IN ('completed', 'absent', 'no_result')),
  CONSTRAINT chk_comp_stage_settlement_item_score CHECK (total_score BETWEEN 0 AND 100),
  CONSTRAINT chk_comp_stage_settlement_item_rank CHECK (display_rank IS NULL OR display_rank > 0),
  CONSTRAINT chk_comp_stage_settlement_item_tied CHECK (tied_flag IN (0, 1)),
  CONSTRAINT chk_comp_stage_settlement_item_promotion CHECK (promotion_status IN ('not_selected', 'selected', 'blocked')),
  UNIQUE KEY uk_comp_stage_settlement_item_member (tenant_id, settlement_id, contestant_member_id, deleted),
  KEY idx_comp_stage_settlement_item_rank (tenant_id, settlement_id, display_rank, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_promotion_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  settlement_id BIGINT NOT NULL,
  source_stage_id BIGINT NOT NULL,
  target_stage_id BIGINT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  quota INT NOT NULL,
  promotion_version INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  published_at DATETIME NULL,
  published_by BIGINT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_promotion_quota CHECK (quota >= 0),
  CONSTRAINT chk_comp_stage_promotion_version CHECK (promotion_version > 0),
  CONSTRAINT chk_comp_stage_promotion_status CHECK (status IN ('draft', 'published', 'withdrawn')),
  UNIQUE KEY uk_comp_stage_promotion_version (tenant_id, settlement_id, promotion_version, deleted),
  KEY idx_comp_stage_promotion_scope (tenant_id, source_stage_id, target_stage_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_promotion_item (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  promotion_snapshot_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  source_rank INT NOT NULL,
  selection_status VARCHAR(32) NOT NULL,
  reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_promotion_item_rank CHECK (source_rank > 0),
  CONSTRAINT chk_comp_stage_promotion_item_status CHECK (selection_status IN ('selected', 'not_selected', 'blocked')),
  UNIQUE KEY uk_comp_stage_promotion_item_member (tenant_id, promotion_snapshot_id, contestant_member_id, deleted),
  KEY idx_comp_stage_promotion_item_rank (tenant_id, promotion_snapshot_id, source_rank, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_unlock_outbox (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  source_stage_id BIGINT NOT NULL,
  target_stage_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  promotion_snapshot_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL DEFAULT 'promotion_published',
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  processed_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_unlock_outbox_status CHECK (status IN ('pending', 'processing', 'completed', 'failed')),
  CONSTRAINT chk_comp_stage_unlock_outbox_attempts CHECK (attempts >= 0),
  UNIQUE KEY uk_comp_stage_unlock_outbox_member (
    tenant_id, promotion_snapshot_id, contestant_member_id, deleted
  ),
  KEY idx_comp_stage_unlock_outbox_pending (tenant_id, status, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
