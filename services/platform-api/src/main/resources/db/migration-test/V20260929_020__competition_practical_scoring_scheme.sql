-- Distinguish manually composed practical scoring schemes from historical simple make plans.
ALTER TABLE competition_make_exam_plan
  ADD COLUMN plan_purpose VARCHAR(32) NOT NULL DEFAULT 'EXAM_PLAN'
    COMMENT '计划用途：常规组卷计划或实操评分方案' AFTER content_type,
  ADD CONSTRAINT chk_competition_make_plan_purpose
    CHECK (plan_purpose IN ('EXAM_PLAN', 'PRACTICAL_SCORING_SCHEME')),
  DROP INDEX uk_competition_make_plan_version,
  ADD UNIQUE KEY uk_competition_make_plan_purpose_version (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, plan_purpose, plan_version, deleted
  ),
  ADD KEY idx_competition_make_plan_purpose_scope (
    tenant_id, event_id, event_work_type_id, event_work_type_level_id,
    content_type, plan_purpose, status, deleted
  );

UPDATE sys_menu
SET name = '实操评分方案',
    path = '/competition/make-exam-plans/simple/practical',
    component = 'competition/CompetitionPracticalScoringScheme',
    icon = 'ToolOutlined',
    updated_at = NOW(),
    updated_by = 1
WHERE code = 'competition_simple_practical_make_exam_plan'
  AND tenant_id = 10159
  AND deleted = 0;
