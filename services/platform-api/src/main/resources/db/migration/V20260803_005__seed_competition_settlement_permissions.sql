-- COMP-STAGE-01F: settlement, ranking publish and promotion permissions.
SET @competition_stage_menu_id := (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'competition_stage' AND deleted = 0 LIMIT 1);
SET @next_competition_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_permission);
INSERT INTO sys_permission (id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:settlement:view', '查看赛段结算', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:settlement:manage', '管理赛段结算', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (@next_competition_permission_id := @next_competition_permission_id + 1, 1, @competition_stage_menu_id, 'competition:settlement:publish', '发布赛段结算', 'publish', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id), name = VALUES(name), action = VALUES(action), status = 'enabled', deleted = 0, updated_at = NOW(), updated_by = 1;
SET @next_competition_role_permission_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);
INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted)
SELECT @next_competition_role_permission_id := @next_competition_role_permission_id + 1, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.code IN ('competition:settlement:view', 'competition:settlement:manage', 'competition:settlement:publish')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission existing WHERE existing.tenant_id = p.tenant_id AND existing.role_id = 2000 AND existing.permission_id = p.id AND existing.deleted = 0);
