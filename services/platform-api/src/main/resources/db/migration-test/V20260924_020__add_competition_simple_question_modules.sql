-- Add a no-blueprint question branch under competition question management.
-- Keep the existing blueprint-based theory/practical pages as siblings.
SET @competition_scope_tenant_id := COALESCE(
  (
    SELECT tenant_id FROM sys_menu
    WHERE code = 'competition_question_bank' AND deleted = 0
    ORDER BY id LIMIT 1
  ),
  (
    SELECT tenant_id FROM sys_menu
    WHERE code = 'competition_foundation' AND deleted = 0
    ORDER BY id LIMIT 1
  ),
  10159
);

SET @competition_question_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_question_bank'
    AND deleted = 0
  ORDER BY id LIMIT 1
);

INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(202609240301, @competition_scope_tenant_id, @competition_question_menu_id, 'directory',
 'competition_simple_question', '简易试题', '/competition/question-bank/simple', NULL,
 'competition:question:view', 'FolderOpenOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
(202609240302, @competition_scope_tenant_id, 202609240301, 'page',
 'competition_simple_theory_question', '理论试题', '/competition/question-bank/simple/theory',
 'competition/CompetitionQuestionBank', 'competition:question:view', 'ProfileOutlined', 1, 1,
 'enabled', NOW(), NOW(), 1, 1, 0),
(202609240303, @competition_scope_tenant_id, 202609240301, 'page',
 'competition_simple_practical_question', '实操试题', '/competition/question-bank/simple/practical',
 'competition/CompetitionQuestionBank', 'competition:question:view', 'ToolOutlined', 2, 1,
 'enabled', NOW(), NOW(), 1, 1, 0);

SET @competition_simple_question_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_simple_question'
    AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @competition_simple_theory_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_simple_theory_question'
    AND deleted = 0
  ORDER BY id LIMIT 1
);
SET @competition_simple_practical_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = @competition_scope_tenant_id
    AND code = 'competition_simple_practical_question'
    AND deleted = 0
  ORDER BY id LIMIT 1
);

UPDATE sys_menu
SET parent_id = @competition_question_menu_id,
    menu_type = 'directory',
    name = '简易试题',
    path = '/competition/question-bank/simple',
    component = NULL,
    permission_code = 'competition:question:view',
    icon = 'FolderOpenOutlined',
    sort_order = 5,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_simple_question'
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = @competition_simple_question_menu_id,
    menu_type = 'page',
    name = '理论试题',
    path = '/competition/question-bank/simple/theory',
    component = 'competition/CompetitionQuestionBank',
    permission_code = 'competition:question:view',
    icon = 'ProfileOutlined',
    sort_order = 1,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_simple_theory_question'
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = @competition_simple_question_menu_id,
    menu_type = 'page',
    name = '实操试题',
    path = '/competition/question-bank/simple/practical',
    component = 'competition/CompetitionQuestionBank',
    permission_code = 'competition:question:view',
    icon = 'ToolOutlined',
    sort_order = 2,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = @competition_scope_tenant_id
  AND code = 'competition_simple_practical_question'
  AND deleted = 0;

-- Preserve question-bank visibility for every role that already has access to
-- the competition question-management directory or either existing question page.
SET @next_competition_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT (@next_competition_role_menu_id := @next_competition_role_menu_id + 1),
       grants.tenant_id, grants.role_id, grants.menu_id, NOW(), NOW(), 1, 1, 0
FROM (
  SELECT DISTINCT parent_grant.tenant_id, parent_grant.role_id, target_menu.id AS menu_id,
         target_menu.sort_order
  FROM sys_role_menu parent_grant
  INNER JOIN sys_menu question_menu
    ON question_menu.id = parent_grant.menu_id
   AND question_menu.tenant_id = parent_grant.tenant_id
  INNER JOIN sys_menu target_menu
    ON target_menu.tenant_id = @competition_scope_tenant_id
   AND target_menu.id IN (
     @competition_simple_question_menu_id,
     @competition_simple_theory_menu_id,
     @competition_simple_practical_menu_id
   )
   AND target_menu.deleted = 0
  LEFT JOIN sys_role_menu existing_grant
    ON existing_grant.tenant_id = parent_grant.tenant_id
   AND existing_grant.role_id = parent_grant.role_id
   AND existing_grant.menu_id = target_menu.id
   AND existing_grant.deleted = 0
  WHERE parent_grant.tenant_id = @competition_scope_tenant_id
    AND question_menu.code IN (
      'competition_question_bank', 'competition_theory_question', 'competition_practical_question'
    )
    AND (
      question_menu.code = 'competition_question_bank'
      OR (
        question_menu.code = 'competition_theory_question'
        AND target_menu.id IN (@competition_simple_question_menu_id, @competition_simple_theory_menu_id)
      )
      OR (
        question_menu.code = 'competition_practical_question'
        AND target_menu.id IN (@competition_simple_question_menu_id, @competition_simple_practical_menu_id)
      )
    )
    AND parent_grant.deleted = 0
    AND question_menu.deleted = 0
    AND existing_grant.id IS NULL
  ORDER BY parent_grant.role_id, target_menu.sort_order
) grants;
