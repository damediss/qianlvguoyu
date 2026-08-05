-- COMP-STAGE-01A: level-scoped scoring with legacy columns retained for read compatibility.

ALTER TABLE competition_score_rule
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD COLUMN simulation_weight DECIMAL(5,2) NULL,
  ADD COLUMN practical_weight DECIMAL(5,2) NULL,
  ADD KEY idx_competition_score_rule_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  ),
  ADD UNIQUE KEY uk_competition_score_rule_level_version (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, rule_version, deleted
  );

ALTER TABLE competition_score_source
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_score_source_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, source_type, deleted
  );

ALTER TABLE competition_total_score_snapshot
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_total_score_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, confirmation_status, deleted
  );

ALTER TABLE competition_ranking_snapshot
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_ranking_snapshot_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  );

ALTER TABLE competition_theory_requirement
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_theory_requirement_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, deleted
  );

ALTER TABLE competition_theory_paper_snapshot
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_theory_snapshot_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  );

ALTER TABLE competition_theory_schedule_version
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_theory_schedule_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  );

ALTER TABLE competition_theory_exam_session
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_theory_session_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  );

ALTER TABLE competition_theory_candidate_assignment
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_theory_assignment_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, status, deleted
  );

ALTER TABLE competition_practical_score
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_practical_score_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, contestant_member_id, deleted
  );

ALTER TABLE competition_referee_assignment
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_referee_assignment_level (
    tenant_id, event_id, stage_id, event_work_type_id, referee_member_id, event_work_type_level_id, deleted
  );

ALTER TABLE competition_referee_avoidance
  ADD COLUMN event_work_type_level_id BIGINT NULL,
  ADD KEY idx_comp_referee_avoidance_level (
    tenant_id, event_id, stage_id, event_work_type_id, event_work_type_level_id, deleted
  );
