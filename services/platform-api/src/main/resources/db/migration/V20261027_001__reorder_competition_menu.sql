-- Reorder the competition menu and hide referee-management navigation.
-- Keep the referee permissions and feature data intact.

UPDATE sys_menu menu
JOIN sys_menu root
  ON root.id = menu.parent_id
 AND root.tenant_id = menu.tenant_id
 AND root.code = 'competition_foundation'
 AND root.parent_id = 0
 AND root.deleted = 0
SET menu.sort_order = CASE menu.code
      WHEN 'competition_event' THEN 1
      WHEN 'competition_stage' THEN 2
      WHEN 'competition_notice' THEN 3
      WHEN 'competition_player' THEN 4
      WHEN 'competition_question_bank' THEN 5
      WHEN 'competition_make_exam_plan' THEN 6
      WHEN 'competition_formal_exam' THEN 7
      WHEN 'competition_score_ranking' THEN 8
    END,
    menu.updated_at = NOW(),
    menu.updated_by = 1
WHERE menu.tenant_id = 10159
  AND menu.deleted = 0
  AND menu.code IN (
    'competition_event',
    'competition_stage',
    'competition_notice',
    'competition_player',
    'competition_question_bank',
    'competition_make_exam_plan',
    'competition_formal_exam',
    'competition_score_ranking'
  );

UPDATE sys_menu
SET visible = 0,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 10159
  AND deleted = 0
  AND code IN (
    'competition_referee',
    'competition_referee_info',
    'competition_referee_group'
  );
