INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  6360, 1, 6300, 'page', 'certification_dashboard', '数据面板',
  '/certification/dashboard', 'certification/CertificationDataPanel',
  'certification.dashboard.view', 'DashboardOutlined', 1, 1,
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
  WHEN 'certification_project' THEN 2
  WHEN 'certification_preparation' THEN 3
  WHEN 'certification_pre_exam' THEN 4
  WHEN 'certification_info' THEN 5
  WHEN 'certification_archive' THEN 6
  WHEN 'certification_task_archive' THEN 7
  ELSE sort_order
END,
updated_at = NOW(),
updated_by = 1
WHERE tenant_id = 1
  AND parent_id = 6300
  AND code IN (
    'certification_project',
    'certification_preparation',
    'certification_pre_exam',
    'certification_info',
    'certification_archive',
    'certification_task_archive'
  )
  AND deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  710100, 1, 6360, 'certification.dashboard.view', '查看认定数据面板', 'view',
  'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_permission
  WHERE tenant_id = 1
    AND code = 'certification.dashboard.view'
    AND deleted = 0
);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (806360, 1, 2000, 6360, NOW(), NOW(), 1, 1, 0),
  (999360, 1, 2604, 6360, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 911000, 1, 2000, id, NOW(), NOW(), 1, 1, 0
FROM sys_permission
WHERE tenant_id = 1
  AND code = 'certification.dashboard.view'
  AND deleted = 0;

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 911001, 1, 2604, id, NOW(), NOW(), 1, 1, 0
FROM sys_permission
WHERE tenant_id = 1
  AND code = 'certification.dashboard.view'
  AND deleted = 0;
