-- Preserve the exact execution plan that produced each immutable promotion snapshot.
ALTER TABLE competition_stage_promotion_snapshot
  ADD COLUMN source_plan_id BIGINT NULL AFTER settlement_id,
  ADD KEY idx_comp_stage_promotion_source_plan
    (tenant_id, source_plan_id, source_stage_id, target_stage_id, status, deleted);
