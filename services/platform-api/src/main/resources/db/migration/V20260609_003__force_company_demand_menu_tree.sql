-- 强制修复「需求管理 → 培训需求/人才需求」树形结构（按 code 关联，避免 id 不一致）

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  5045, 1, 0, 'directory', 'company_demand', '需求管理', '/demand', NULL, 'train:company-demand:view', 'FolderOutlined', 29, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE tenant_id = 1 AND code = 'company_demand' AND deleted = 0
);

UPDATE sys_menu
SET parent_id = 0,
    menu_type = 'directory',
    name = '需求管理',
    path = '/demand',
    permission_code = 'train:company-demand:view',
    icon = 'FolderOutlined',
    sort_order = 29,
    visible = 1,
    status = 'enabled',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'company_demand'
  AND deleted = 0;

UPDATE sys_menu child
INNER JOIN sys_menu parent
  ON parent.tenant_id = child.tenant_id
 AND parent.code = 'company_demand'
 AND parent.deleted = 0
SET child.parent_id = parent.id,
    child.visible = 1,
    child.status = 'enabled',
    child.updated_at = NOW(),
    child.updated_by = 1,
    child.name = CASE child.code
      WHEN 'train_demand-train' THEN '培训需求'
      WHEN 'train_demand-employee' THEN '人才需求'
      ELSE child.name
    END,
    child.sort_order = CASE child.code
      WHEN 'train_demand-train' THEN 1
      WHEN 'train_demand-employee' THEN 2
      ELSE child.sort_order
    END,
    child.component = CASE child.code
      WHEN 'train_demand-train' THEN 'training/EnterpriseTrainingDemandPage'
      WHEN 'train_demand-employee' THEN 'training/DemandEmployee'
      ELSE child.component
    END
WHERE child.tenant_id = 1
  AND child.deleted = 0
  AND child.code IN ('train_demand-train', 'train_demand-employee');

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  5670, 1, m.id, 'train:company-demand:view', '需求管理入口', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'company_demand'
  AND m.deleted = 0;

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  9830000 + (r.id * 100) + m.id,
  r.tenant_id,
  r.id,
  m.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role r
INNER JOIN sys_menu m
  ON m.tenant_id = r.tenant_id
 AND m.deleted = 0
 AND m.status = 'enabled'
 AND m.code IN ('company_demand', 'train_demand-train', 'train_demand-employee')
WHERE r.tenant_id = 1
  AND r.deleted = 0
  AND r.code = 'PLATFORM_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.tenant_id = r.tenant_id
      AND rm.role_id = r.id
      AND rm.menu_id = m.id
      AND rm.deleted = 0
  );
