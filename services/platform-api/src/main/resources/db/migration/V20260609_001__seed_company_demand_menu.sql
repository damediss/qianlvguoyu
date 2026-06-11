-- 对齐 yun_shang：需求管理 → 培训需求 / 人才需求（企业申报入口）

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  5045, 1, 0, 'directory', 'company_demand', '需求管理', '/demand', NULL, 'train:company-demand:view', 'FolderOutlined', 29, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET parent_id = 5045,
    name = '培训需求',
    path = '/training/demand-trains',
    component = 'training/EnterpriseTrainingDemandPage',
    sort_order = 1,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_demand-train'
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = 5045,
    name = '人才需求',
    sort_order = 2,
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_demand-employee'
  AND deleted = 0;

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  5670, 1, 5045, 'train:company-demand:view', '需求管理入口', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  604945 + role_id,
  tenant_id,
  role_id,
  5045,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu
WHERE tenant_id = 1
  AND menu_id = 5015
  AND deleted = 0;

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  605670 + role_id,
  tenant_id,
  role_id,
  5670,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu
WHERE tenant_id = 1
  AND menu_id = 5015
  AND deleted = 0;
