-- COMP-STAGE-01F: stage planning permissions are explicit and platform-admin owned.

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
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage:view', '查看赛段', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage:manage', '管理赛段', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage-plan:view', '查看赛段执行方案', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage-plan:manage', '管理赛段执行方案', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage-plan:publish', '发布赛段执行方案', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage-plan:exception', '处理赛段延期补录', 'exception', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage-roster:manage', '管理赛段名单', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:stage-session:manage', '管理赛段场次', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:practical-rule:manage', '管理竞赛实操规则', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id,
    'competition:judge-group:manage', '管理竞赛裁判组', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action),
  status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = 1;

SET @next_competition_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_role_permission_id := @next_competition_role_permission_id + 1,
  p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'competition:stage:view', 'competition:stage:manage', 'competition:stage-plan:view',
    'competition:stage-plan:manage', 'competition:stage-plan:publish',
    'competition:stage-plan:exception', 'competition:stage-roster:manage',
    'competition:stage-session:manage', 'competition:practical-rule:manage',
    'competition:judge-group:manage'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = p.tenant_id
      AND existing.role_id = 2000
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );
