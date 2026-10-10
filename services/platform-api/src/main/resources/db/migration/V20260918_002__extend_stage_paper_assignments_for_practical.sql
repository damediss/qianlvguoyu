-- Distinguish theory and practical contestant-paper assignments on the same stage.
-- Existing rows are theory assignments and retain the default value.

ALTER TABLE competition_stage_paper_assignment
  ADD COLUMN content_type VARCHAR(16) NOT NULL DEFAULT 'theory'
    COMMENT '试卷类型：theory-理论，practical-实操'
    AFTER event_work_type_level_id,
  DROP INDEX uk_comp_stage_paper_assignment_contestant,
  DROP INDEX idx_comp_stage_paper_assignment_scope,
  ADD CONSTRAINT chk_comp_stage_paper_assignment_content_type
    CHECK (content_type IN ('theory', 'practical')),
  ADD UNIQUE KEY uk_comp_stage_paper_assignment_contestant (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, content_type, contestant_profile_id, deleted
  ),
  ADD KEY idx_comp_stage_paper_assignment_scope (
    tenant_id, event_id, stage_id, event_work_type_id,
    event_work_type_level_id, content_type, paper_snapshot_id, status, deleted
  );
