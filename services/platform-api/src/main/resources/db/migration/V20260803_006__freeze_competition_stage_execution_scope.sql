-- Freeze the score rule, promotion quota, next-stage topology and promotion source
-- at execution-plan/roster publication time. Existing rows remain readable and are
-- completed by the next plan publication.
ALTER TABLE competition_stage_execution_plan
  ADD COLUMN score_rule_id BIGINT NULL AFTER preflight_status,
  ADD COLUMN promotion_quota INT NULL AFTER score_rule_id,
  ADD COLUMN target_stage_id BIGINT NULL AFTER promotion_quota,
  ADD CONSTRAINT chk_comp_stage_plan_promotion_quota
    CHECK (promotion_quota IS NULL OR promotion_quota >= 0),
  ADD KEY idx_comp_stage_plan_frozen_scope
    (tenant_id, event_id, stage_id, score_rule_id, target_stage_id, deleted);

ALTER TABLE competition_stage_roster_snapshot
  ADD COLUMN source_promotion_snapshot_id BIGINT NULL AFTER source_type,
  ADD UNIQUE KEY uk_comp_stage_roster_promotion_source
    (tenant_id, plan_id, source_promotion_snapshot_id, deleted),
  ADD KEY idx_comp_stage_roster_promotion_source
    (tenant_id, plan_id, source_promotion_snapshot_id, deleted);
