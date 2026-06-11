-- 修复「需求管理 → 培训需求」侧栏不显示：
-- 1. 提升为顶级菜单（对齐 yun_shang company.demand）
-- 2. 为拥有子菜单的角色补齐父级目录授权
-- 3. 平台管理员同步目录与权限

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  5045, 1, 0, 'directory', 'company_demand', '需求管理', '/demand', NULL, 'train:company-demand:view', 'FolderOutlined', 29, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = 0,
  menu_type = 'directory',
  name = '需求管理',
  path = '/demand',
  permission_code = 'train:company-demand:view',
  icon = 'FolderOutlined',
  sort_order = 29,
  visible = 1,
  status = 'enabled',
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET parent_id = 5045,
    name = '培训需求',
    path = '/training/demand-trains',
    component = 'training/EnterpriseTrainingDemandPage',
    sort_order = 1,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_demand-train'
  AND deleted = 0;

UPDATE sys_menu
SET parent_id = 5045,
    name = '人才需求',
    path = '/training/demand-employees',
    component = 'training/DemandEmployee',
    sort_order = 2,
    visible = 1,
    status = 'enabled',
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

-- 拥有培训/人才需求子菜单的角色，自动补齐「需求管理」目录
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  9820000 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  5045,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
WHERE rm.tenant_id = 1
  AND rm.deleted = 0
  AND rm.menu_id IN (5015, 5016)
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu x
    WHERE x.tenant_id = rm.tenant_id
      AND x.role_id = rm.role_id
      AND x.menu_id = 5045
      AND x.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  9821000 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  5670,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
WHERE rm.tenant_id = 1
  AND rm.deleted = 0
  AND rm.menu_id IN (5015, 5016)
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission x
    WHERE x.tenant_id = rm.tenant_id
      AND x.role_id = rm.role_id
      AND x.permission_id = 5670
      AND x.deleted = 0
  );

-- 平台超级管理员兜底同步
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9801000 + m.id, m.tenant_id, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.deleted = 0
  AND m.status = 'enabled'
  AND m.code IN ('company_demand', 'train_demand-train', 'train_demand-employee')
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.tenant_id = m.tenant_id
      AND rm.role_id = 2000
      AND rm.menu_id = m.id
      AND rm.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9800000 + p.id, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.status = 'enabled'
  AND p.code IN (
    'train:company-demand:view',
    'train:demand-train:view',
    'train:demand-train:create',
    'train:demand-train:update',
    'train:demand-train:delete',
    'train:demand-employee:view'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
