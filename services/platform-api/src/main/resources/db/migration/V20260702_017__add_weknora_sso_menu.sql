-- Add WeKnora SSO external menu under knowledge directory
-- menu_type='external' triggers frontend SSO bridge flow instead of pageMap rendering
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  4604, 1, 4600, 'external', 'knowledge_weknora', 'AI知识引擎',
  'http://43.138.163.7:8088', NULL, 'knowledge.weknora:view', 'RobotOutlined', 4, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
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
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

-- Grant PLATFORM_ADMIN role access
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 600461, tenant_id, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_menu
WHERE tenant_id = 1 AND id = 4604 AND deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
