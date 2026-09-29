-- Add no-blueprint theory and practical make-plan pages under competition make plans.
SET @competition_scope_tenant_id := 10159;

SET @competition_make_plan_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_make_exam_plan'
    AND deleted = 0
  ORDER BY id LIMIT 1
);

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609240401, @competition_scope_tenant_id, @competition_make_plan_menu_id, 'directory',
 'competition_simple_make_exam_plan', '简易组卷', '/competition/make-exam-plans/simple', NULL,
 'competition:make-plan:view', 'ThunderboltOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609240402, @competition_scope_tenant_id, 202609240401, 'page',
 'competition_simple_theory_make_exam_plan', '理论组卷', '/competition/make-exam-plans/simple/theory',
 'competition/CompetitionMakeExamPlan', 'competition:make-plan:view', 'ProfileOutlined', 1, 1,
 'enabled', NOW(), NOW(), 1, 1, 0),
(202609240403, @competition_scope_tenant_id, 202609240401, 'page',
 'competition_simple_practical_make_exam_plan', '实操组卷', '/competition/make-exam-plans/simple/practical',
 'competition/CompetitionMakeExamPlan', 'competition:make-plan:view', 'ToolOutlined', 2, 1,
 'enabled', NOW(), NOW(), 1, 1, 0);

SET @competition_simple_make_plan_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_simple_make_exam_plan'
    AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @competition_simple_theory_make_plan_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_simple_theory_make_exam_plan'
    AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @competition_simple_practical_make_plan_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_simple_practical_make_exam_plan'
    AND deleted = 0
  ORDER BY id LIMIT 1
);

UPDATE sys_menu
SET parent_id = @competition_make_plan_menu_id,
    menu_type = 'directory',
    name = '简易组卷',
    path = '/competition/make-exam-plans/simple',
    component = NULL,
    permission_code = 'competition:make-plan:view',
    icon = 'ThunderboltOutlined',
    sort_order = 3,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_simple_make_exam_plan'
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = @competition_simple_make_plan_menu_id,
    menu_type = 'page',
    name = '理论组卷',
    path = '/competition/make-exam-plans/simple/theory',
    component = 'competition/CompetitionMakeExamPlan',
    permission_code = 'competition:make-plan:view',
    icon = 'ProfileOutlined',
    sort_order = 1,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_simple_theory_make_exam_plan'
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = @competition_simple_make_plan_menu_id,
    menu_type = 'page',
    name = '实操组卷',
    path = '/competition/make-exam-plans/simple/practical',
    component = 'competition/CompetitionMakeExamPlan',
    permission_code = 'competition:make-plan:view',
    icon = 'ToolOutlined',
    sort_order = 2,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_simple_practical_make_exam_plan'
  AND deleted = 0;

-- Give the new menu pages to roles that could already view the make-plan area.
SET @next_competition_make_plan_role_menu_id := (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_menu
);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_competition_make_plan_role_menu_id := @next_competition_make_plan_role_menu_id + 1),
       grants.tenant_id, grants.role_id, grants.menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT DISTINCT source_grant.tenant_id, source_grant.role_id, target_menu.id AS menu_id,
         target_menu.sort_order
  FROM sys_role_menu source_grant
  INNER JOIN sys_menu source_menu
    ON source_menu.id = source_grant.menu_id
   AND source_menu.tenant_id = source_grant.tenant_id
  INNER JOIN sys_menu target_menu
    ON target_menu.tenant_id = @competition_scope_tenant_id
   AND target_menu.id IN (
     @competition_simple_make_plan_menu_id,
     @competition_simple_theory_make_plan_menu_id,
     @competition_simple_practical_make_plan_menu_id
   )
   AND target_menu.deleted = 0
  LEFT JOIN sys_role_menu existing_grant
    ON existing_grant.tenant_id = source_grant.tenant_id
   AND existing_grant.role_id = source_grant.role_id
   AND existing_grant.menu_id = target_menu.id
   AND existing_grant.deleted = 0
  WHERE source_grant.tenant_id = @competition_scope_tenant_id
    AND source_menu.code IN (
      'competition_make_exam_plan',
      'competition_theory_make_exam_plan',
      'competition_practical_make_exam_plan'
    )
    AND source_menu.deleted = 0
    AND source_grant.deleted = 0
    AND existing_grant.id IS NULL
  ORDER BY source_grant.role_id, target_menu.sort_order
) grants;
