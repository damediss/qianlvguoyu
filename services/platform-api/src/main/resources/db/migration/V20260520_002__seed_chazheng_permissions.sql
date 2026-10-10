-- Add chazheng menu and permissions for demo tenant
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4820, 1, 0, 'directory', 'chazheng', '查证管理', '/chazheng', NULL, 'chazheng:user:view', 'SafetyCertificateOutlined', 23, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4821, 1, 4800, 'page', 'chazheng_user_list', '用户列表', '/chazheng/user-list', 'chazheng/UserList', 'chazheng:user:view', 'SearchOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5362, 1, 4821, 'chazheng:user:view', '查看查证用户', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5363, 1, 4821, 'chazheng:user:import', '导入查证用户', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5364, 1, 4821, 'chazheng:user:export', '导出查证用户', 'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5365, 1, 4821, 'chazheng:user:cert', '查证', 'cert', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604607, 1, 2000, 4820, NOW(), NOW(), 1, 1, 0),
  (604608, 1, 2000, 4821, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705129, 1, 2000, 5362, NOW(), NOW(), 1, 1, 0),
  (705130, 1, 2000, 5363, NOW(), NOW(), 1, 1, 0),
  (705131, 1, 2000, 5364, NOW(), NOW(), 1, 1, 0),
  (705132, 1, 2000, 5365, NOW(), NOW(), 1, 1, 0);