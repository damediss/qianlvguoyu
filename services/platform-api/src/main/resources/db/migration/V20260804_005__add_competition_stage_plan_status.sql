-- Stage arrangement has an explicit draft/confirmed boundary independent of event publication.
ALTER TABLE competition_event
  ADD COLUMN stage_plan_status VARCHAR(32) NOT NULL DEFAULT 'draft' AFTER status,
  ADD COLUMN stage_plan_confirmed_by BIGINT NULL AFTER stage_plan_status,
  ADD COLUMN stage_plan_confirmed_at DATETIME NULL AFTER stage_plan_confirmed_by,
  ADD CONSTRAINT chk_competition_event_stage_plan_status
    CHECK (stage_plan_status IN ('draft', 'confirmed'));
