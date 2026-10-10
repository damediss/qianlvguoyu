INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  6370, 1, 6300, 'page', 'certification_practical_config', '实操配置',
  '/certification/practical-config', 'certification/PracticalConfiguration',
  'certification.practical.config', 'FileProtectOutlined', 4, 1,
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
  deleted = VALUES(deleted),
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET sort_order = CASE code
  WHEN 'certification_dashboard' THEN 1
  WHEN 'certification_project' THEN 2
  WHEN 'certification_preparation' THEN 3
  WHEN 'certification_practical_config' THEN 4
  WHEN 'certification_pre_exam' THEN 5
  WHEN 'certification_info' THEN 6
  WHEN 'certification_archive' THEN 7
  WHEN 'certification_task_archive' THEN 8
  ELSE sort_order
END,
updated_at = NOW(),
updated_by = 1
WHERE tenant_id = 1
  AND parent_id = 6300
  AND deleted = 0;

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202606150400 + r.id,
  r.tenant_id,
  r.id,
  6370,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
WHERE r.tenant_id = 1
  AND UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN', 'CERTIFICATION_DEV')
  AND r.deleted = 0;
