-- COMP-STAGE-01E/01F: explicit permission codes for the execution and scoring workbench.
SET @competition_stage_menu_id := (
  SELECT id FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_stage' AND deleted = 0
  LIMIT 1
);
SET @next_competition_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:practical-rule:view', '查看实操规则', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:practical-rule:publish', '发布实操规则', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:practical-session:view', '查看实操场次', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:practical-session:manage', '管理实操场次', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:judge-group:view', '查看裁判组', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:practical-assignment:manage', '管理实操分配', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:practical-score:manage', '管理实操评分', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:practical-score:unlock', '解锁实操评分', 'unlock', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action), status = 'enabled', deleted = 0,
  updated_at = NOW(), updated_by = 1;

SET @next_competition_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_competition_role_permission_id := @next_competition_role_permission_id + 1),
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1 AND p.deleted = 0
  AND p.code IN (
    'competition:practical-rule:view', 'competition:practical-rule:manage', 'competition:practical-rule:publish',
    'competition:practical-session:view', 'competition:practical-session:manage', 'competition:judge-group:view',
    'competition:judge-group:manage', 'competition:practical-assignment:manage', 'competition:practical-score:manage',
    'competition:practical-score:unlock'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = p.tenant_id AND existing.role_id = 2000
      AND existing.permission_id = p.id AND existing.deleted = 0
  );
