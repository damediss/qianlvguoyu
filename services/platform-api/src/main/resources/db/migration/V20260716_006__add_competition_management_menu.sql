-- FND-06: expose the implemented competition management page in the dynamic menu tree.
-- The event detail page owns organization, stage, roster, registration and notice sub-pages.

SET @competition_root_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_foundation' AND deleted = 0
  LIMIT 1
);
SET @competition_root_menu_id := COALESCE(
  @competition_root_menu_id,
  (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_menu)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_root_menu_id, 1, 0, 'directory', 'competition_foundation', '竞赛管理', '/competition', NULL,
  'competition:event:view', 'TrophyOutlined', 25, 1, 'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

SET @competition_event_menu_id := (
  SELECT id
  FROM sys_menu
  WHERE tenant_id = 1 AND code = 'competition_event' AND deleted = 0
  LIMIT 1
);
SET @competition_event_menu_id := COALESCE(
  @competition_event_menu_id,
  (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_menu)
);

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  @competition_event_menu_id, 1, @competition_root_menu_id, 'page', 'competition_event', '赛事管理', '/competition/events',
  'competition/CompetitionEventManagementPage', 'competition:event:view', 'TrophyOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

-- All FND-06 permissions belong to the implemented event management page.
UPDATE sys_permission
SET menu_id = @competition_event_menu_id,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code IN (
    'competition:event:view',
    'competition:event:create',
    'competition:event:update',
    'competition:event:publish',
    'competition:event-org:view',
    'competition:event-org:manage',
    'competition:stage:view',
    'competition:stage:manage',
    'competition:task-template:view',
    'competition:task-template:manage',
    'competition:roster:view',
    'competition:roster:import',
    'competition:roster:confirm',
    'competition:registration:view',
    'competition:registration:audit',
    'competition:notice:view',
    'competition:notice:publish'
  );

-- Bind the menu to every role that already has the event view permission.
-- This preserves the existing permission contract and makes the menu visible to
-- future non-platform roles when the existing permission is granted to them.
SET @next_competition_role_menu_id := (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_role_menu_id := @next_competition_role_menu_id + 1,
  rp.tenant_id,
  rp.role_id,
  @competition_root_menu_id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
JOIN sys_permission p
  ON p.tenant_id = rp.tenant_id
 AND p.id = rp.permission_id
 AND p.code = 'competition:event:view'
 AND p.deleted = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_menu existing
  WHERE existing.tenant_id = rp.tenant_id
    AND existing.role_id = rp.role_id
    AND existing.menu_id = @competition_root_menu_id
    AND existing.deleted = 0
);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  @next_competition_role_menu_id := @next_competition_role_menu_id + 1,
  rp.tenant_id,
  rp.role_id,
  @competition_event_menu_id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
JOIN sys_permission p
  ON p.tenant_id = rp.tenant_id
 AND p.id = rp.permission_id
 AND p.code = 'competition:event:view'
 AND p.deleted = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_menu existing
  WHERE existing.tenant_id = rp.tenant_id
    AND existing.role_id = rp.role_id
    AND existing.menu_id = @competition_event_menu_id
    AND existing.deleted = 0
);
