-- Split the competition make-exam plan page into theory and practical modules.
-- Keep the existing menu id as the parent so its permission records and grants remain valid.
SET @competition_scope_tenant_id := 10159;

SET @competition_root_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_foundation'
    AND parent_id = 0
    AND deleted = 0
  ORDER BY id LIMIT 1
);

SET @competition_make_plan_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_make_exam_plan'
    AND deleted = 0
  ORDER BY id LIMIT 1
);

UPDATE sys_menu
SET parent_id = @competition_root_menu_id,
    menu_type = 'directory',
    name = '组卷计划',
    path = NULL,
    component = NULL,
    permission_code = 'competition:make-plan:view',
    icon = 'FileTextOutlined',
    sort_order = 31,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_make_exam_plan'
  AND deleted = 0;

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609240201, @competition_scope_tenant_id, @competition_make_plan_menu_id, 'page',
 'competition_theory_make_exam_plan', '理论组卷', '/competition/make-exam-plans',
 'competition/CompetitionMakeExamPlan', 'competition:make-plan:view',
 'ProfileOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609240202, @competition_scope_tenant_id, @competition_make_plan_menu_id, 'page',
 'competition_practical_make_exam_plan', '实操组卷', '/competition/make-exam-plans/practical',
 'competition/CompetitionMakeExamPlan', 'competition:make-plan:view',
 'ToolOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

UPDATE sys_menu
SET parent_id = @competition_make_plan_menu_id,
    menu_type = 'page',
    name = '理论组卷',
    path = '/competition/make-exam-plans',
    component = 'competition/CompetitionMakeExamPlan',
    permission_code = 'competition:make-plan:view',
    icon = 'ProfileOutlined',
    sort_order = 1,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_theory_make_exam_plan'
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = @competition_make_plan_menu_id,
    menu_type = 'page',
    name = '实操组卷',
    path = '/competition/make-exam-plans/practical',
    component = 'competition/CompetitionMakeExamPlan',
    permission_code = 'competition:make-plan:view',
    icon = 'ToolOutlined',
    sort_order = 2,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_practical_make_exam_plan'
  AND deleted = 0;

-- Preserve each role's former access to the page by granting its new child pages.
SET @next_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_role_menu_id := @next_role_menu_id + 1), grants.tenant_id, grants.role_id,
       grants.menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT DISTINCT old_grant.tenant_id, old_grant.role_id, child_menu.id AS menu_id,
         child_menu.sort_order
  FROM sys_role_menu old_grant
  INNER JOIN sys_menu parent_menu
    ON parent_menu.id = old_grant.menu_id
   AND parent_menu.tenant_id = old_grant.tenant_id
  INNER JOIN sys_menu child_menu
    ON child_menu.parent_id = parent_menu.id
   AND child_menu.tenant_id = parent_menu.tenant_id
  LEFT JOIN sys_role_menu existing_grant
    ON existing_grant.tenant_id = old_grant.tenant_id
   AND existing_grant.role_id = old_grant.role_id
   AND existing_grant.menu_id = child_menu.id
   AND existing_grant.deleted = 0
  WHERE old_grant.tenant_id = @competition_scope_tenant_id
    AND old_grant.deleted = 0
    AND parent_menu.code = 'competition_make_exam_plan'
    AND child_menu.code IN ('competition_theory_make_exam_plan', 'competition_practical_make_exam_plan')
    AND child_menu.deleted = 0
    AND existing_grant.id IS NULL
  ORDER BY old_grant.role_id, child_menu.sort_order
) grants;
