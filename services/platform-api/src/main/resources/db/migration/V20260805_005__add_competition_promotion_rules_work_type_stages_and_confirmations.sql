-- Additive COMP-STAGE promotion extension. Existing event-level stages and
-- fixed-count promotionQuota remain the source of truth for legacy rows.

ALTER TABLE competition_stage_execution_plan
  ADD COLUMN promotion_rule_type VARCHAR(32) NULL AFTER promotion_quota,
  ADD COLUMN promotion_percentage INT NULL AFTER promotion_rule_type,
  ADD COLUMN legacy_rule TINYINT NOT NULL DEFAULT 1 AFTER promotion_percentage,
  ADD CONSTRAINT chk_comp_stage_plan_promotion_rule_type CHECK (
    promotion_rule_type IS NULL OR promotion_rule_type IN ('FIXED_COUNT', 'PERCENTAGE', 'FINAL_PODIUM', 'LEGACY')
  ),
  ADD CONSTRAINT chk_comp_stage_plan_promotion_percentage CHECK (
    promotion_percentage IS NULL OR promotion_percentage BETWEEN 1 AND 100
  ),
  ADD CONSTRAINT chk_comp_stage_plan_legacy_rule CHECK (legacy_rule IN (0, 1));

ALTER TABLE competition_stage_promotion_snapshot
  ADD COLUMN promotion_rule_type VARCHAR(32) NULL AFTER quota,
  ADD COLUMN promotion_percentage INT NULL AFTER promotion_rule_type,
  ADD COLUMN calculated_target_count INT NULL AFTER promotion_percentage,
  ADD COLUMN reference_status VARCHAR(32) NOT NULL DEFAULT 'not_generated' AFTER calculated_target_count,
  ADD COLUMN promotion_status VARCHAR(32) NOT NULL DEFAULT 'not_generated' AFTER reference_status,
  ADD COLUMN final_podium_status VARCHAR(32) NOT NULL DEFAULT 'not_generated' AFTER promotion_status,
  ADD CONSTRAINT chk_comp_stage_promotion_rule_type CHECK (
    promotion_rule_type IS NULL OR promotion_rule_type IN ('FIXED_COUNT', 'PERCENTAGE', 'FINAL_PODIUM', 'LEGACY')
  ),
  ADD CONSTRAINT chk_comp_stage_promotion_percentage CHECK (
    promotion_percentage IS NULL OR promotion_percentage BETWEEN 1 AND 100
  ),
  ADD CONSTRAINT chk_comp_stage_promotion_target_count CHECK (
    calculated_target_count IS NULL OR calculated_target_count >= 0
  ),
  ADD CONSTRAINT chk_comp_stage_promotion_reference_status CHECK (
    reference_status IN ('not_generated', 'draft', 'confirmed', 'withdrawn')
  ),
  ADD CONSTRAINT chk_comp_stage_promotion_result_status CHECK (
    promotion_status IN ('not_generated', 'candidate', 'confirmed', 'published')
  ),
  ADD CONSTRAINT chk_comp_stage_promotion_podium_status CHECK (
    final_podium_status IN ('not_generated', 'draft', 'confirmed', 'published')
  );

CREATE TABLE competition_event_work_type_stage (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  event_work_type_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT NULL,
  promotion_rule_type VARCHAR(32) NULL,
  promotion_percentage INT NULL,
  legacy_rule TINYINT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_event_work_type_stage_enabled CHECK (enabled IN (0, 1)),
  CONSTRAINT chk_comp_event_work_type_stage_rule_type CHECK (
    promotion_rule_type IS NULL OR promotion_rule_type IN ('FIXED_COUNT', 'PERCENTAGE', 'FINAL_PODIUM', 'LEGACY')
  ),
  CONSTRAINT chk_comp_event_work_type_stage_percentage CHECK (
    promotion_percentage IS NULL OR promotion_percentage BETWEEN 1 AND 100
  ),
  CONSTRAINT chk_comp_event_work_type_stage_legacy CHECK (legacy_rule IN (0, 1)),
  UNIQUE KEY uk_comp_event_work_type_stage (
    tenant_id, event_work_type_id, stage_id, deleted
  ),
  KEY idx_comp_event_work_type_stage_scope (
    tenant_id, event_id, event_work_type_id, enabled, sort_order, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_reference_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  settlement_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  target_stage_id BIGINT NULL,
  event_work_type_id BIGINT NOT NULL,
  event_work_type_level_id BIGINT NULL,
  promotion_rule_type VARCHAR(32) NOT NULL,
  promotion_quota INT NULL,
  promotion_percentage INT NULL,
  base_count INT NOT NULL DEFAULT 0,
  calculated_target_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  confirmed_by BIGINT NULL,
  confirmed_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_reference_rule_type CHECK (
    promotion_rule_type IN ('FIXED_COUNT', 'PERCENTAGE', 'FINAL_PODIUM', 'LEGACY')
  ),
  CONSTRAINT chk_comp_stage_reference_base_count CHECK (base_count >= 0),
  CONSTRAINT chk_comp_stage_reference_target_count CHECK (calculated_target_count >= 0),
  CONSTRAINT chk_comp_stage_reference_status CHECK (status IN ('draft', 'confirmed', 'withdrawn')),
  UNIQUE KEY uk_comp_stage_reference_settlement (tenant_id, settlement_id, deleted),
  KEY idx_comp_stage_reference_scope (tenant_id, event_id, stage_id, event_work_type_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_reference_snapshot_item (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  reference_snapshot_id BIGINT NOT NULL,
  contestant_member_id BIGINT NOT NULL,
  source_rank INT NULL,
  selection_status VARCHAR(32) NOT NULL DEFAULT 'candidate',
  reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_reference_item_status CHECK (
    selection_status IN ('candidate', 'selected', 'not_selected', 'withdrawn')
  ),
  UNIQUE KEY uk_comp_stage_reference_item_member (
    tenant_id, reference_snapshot_id, contestant_member_id, deleted
  ),
  KEY idx_comp_stage_reference_item_rank (
    tenant_id, reference_snapshot_id, source_rank, selection_status, deleted
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE competition_stage_final_podium (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  settlement_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL,
  first_contestant_member_id BIGINT NULL,
  second_contestant_member_id BIGINT NULL,
  third_contestant_member_id BIGINT NULL,
  special_explanation VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  confirmed_by BIGINT NULL,
  confirmed_at DATETIME NULL,
  published_by BIGINT NULL,
  published_at DATETIME NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_comp_stage_final_podium_status CHECK (
    status IN ('draft', 'confirmed', 'published')
  ),
  UNIQUE KEY uk_comp_stage_final_podium_settlement (tenant_id, settlement_id, deleted),
  KEY idx_comp_stage_final_podium_scope (tenant_id, event_id, stage_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
