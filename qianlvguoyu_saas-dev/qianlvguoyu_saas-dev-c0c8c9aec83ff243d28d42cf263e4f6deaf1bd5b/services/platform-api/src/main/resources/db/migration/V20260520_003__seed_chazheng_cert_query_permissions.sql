-- Add chazheng cert query menu and permissions for demo tenant
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4822, 1, 4820, 'page', 'chazheng_cert_query', '证书查询', '/chazheng/cert-query', 'chazheng/CertQuery', 'chazheng:cert:view', 'IdcardOutlined', 2, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5366, 1, 4822, 'chazheng:cert:view', '查看证书', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604609, 1, 2000, 4822, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705133, 1, 2000, 5366, NOW(), NOW(), 1, 1, 0);