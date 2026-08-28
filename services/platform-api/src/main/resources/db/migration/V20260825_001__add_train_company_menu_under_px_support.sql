-- 培训管理 → 支持保障：新增「企业管理」页面
-- 注意：当前环境库菜单均在实际租户下（非硬编码 tenant_id=1），按父菜单 code 动态挂载

-- 1) 已存在则对齐挂载到「支持保障」
UPDATE sys_menu m
INNER JOIN sys_menu p
  ON p.tenant_id = m.tenant_id
 AND p.code = 'pxzhi_chi_bao_zhang'
 AND p.deleted = 0
SET
  m.parent_id = p.id,
  m.menu_type = 'page',
  m.name = '企业管理',
  m.path = '/training/companies',
  m.component = 'training/EnterpriseManagePage',
  m.permission_code = 'train:company:view',
  m.icon = 'BankOutlined',
  m.sort_order = 1,
  m.visible = 1,
  m.status = 'enabled',
  m.deleted = 0,
  m.updated_at = NOW(),
  m.updated_by = 1
WHERE m.code = 'train_company';

-- 2) 各租户「支持保障」下若不存在则插入
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  208773825000001001 + ROW_NUMBER() OVER (ORDER BY p.tenant_id) - 1,
  p.tenant_id,
  p.id,
  'page',
  'train_company',
  '企业管理',
  '/training/companies',
  'training/EnterpriseManagePage',
  'train:company:view',
  'BankOutlined',
  1,
  1,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu p
WHERE p.code = 'pxzhi_chi_bao_zhang'
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu x
    WHERE x.tenant_id = p.tenant_id AND x.code = 'train_company'
  );

-- 3) 权限（按菜单租户补齐）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  base.id_base + ROW_NUMBER() OVER (ORDER BY m.tenant_id, v.ord) - 1,
  m.tenant_id,
  m.id,
  v.code,
  v.name,
  v.action,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
CROSS JOIN (
  SELECT 1 AS ord, 'train:company:view' AS code, '查看企业管理' AS name, 'view' AS action
  UNION ALL SELECT 2, 'train:company:create', '新增企业', 'create'
  UNION ALL SELECT 3, 'train:company:update', '编辑企业', 'update'
  UNION ALL SELECT 4, 'train:company:delete', '删除企业', 'delete'
) v
CROSS JOIN (SELECT 208773825000401001 AS id_base) base
WHERE m.code = 'train_company'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = m.tenant_id AND p.code = v.code
  );

UPDATE sys_permission p
INNER JOIN sys_menu m ON m.tenant_id = p.tenant_id AND m.code = 'train_company' AND m.deleted = 0
SET
  p.menu_id = m.id,
  p.status = 'enabled',
  p.deleted = 0,
  p.updated_at = NOW(),
  p.updated_by = 1
WHERE p.code IN (
    'train:company:view',
    'train:company:create',
    'train:company:update',
    'train:company:delete'
  );

-- 4) 有学员信息/教务管理菜单的角色 → 同步企业管理菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  208773825000861000 + ROW_NUMBER() OVER (ORDER BY rm.tenant_id, rm.role_id) - 1,
  rm.tenant_id,
  rm.role_id,
  company.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM (
  SELECT DISTINCT rm.tenant_id, rm.role_id
  FROM sys_role_menu rm
  INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
  WHERE rm.deleted = 0
    AND m.code IN ('train_user-info', 'train_teacher-bank')
) rm
INNER JOIN sys_menu company
  ON company.tenant_id = rm.tenant_id AND company.code = 'train_company' AND company.deleted = 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu x
  WHERE x.tenant_id = rm.tenant_id AND x.role_id = rm.role_id AND x.menu_id = company.id AND x.deleted = 0
);

-- 平台管理员兜底（按租户找 PLATFORM_ADMIN / id=2000）
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  208773825000861900 + ROW_NUMBER() OVER (ORDER BY r.tenant_id, r.id) - 1,
  r.tenant_id,
  r.id,
  m.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role r
INNER JOIN sys_menu m ON m.tenant_id = r.tenant_id AND m.code = 'train_company' AND m.deleted = 0
WHERE r.deleted = 0
  AND (r.id = 2000 OR r.code = 'PLATFORM_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = r.tenant_id AND rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 5) 权限授予有该菜单的角色
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  208773825000864000 + ROW_NUMBER() OVER (ORDER BY rm.tenant_id, rm.role_id, p.id) - 1,
  rm.tenant_id,
  rm.role_id,
  p.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
INNER JOIN sys_permission p ON p.tenant_id = rm.tenant_id
  AND p.code IN (
    'train:company:view',
    'train:company:create',
    'train:company:update',
    'train:company:delete'
  )
  AND p.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_company'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = rm.tenant_id AND rp.role_id = rm.role_id AND rp.permission_id = p.id AND rp.deleted = 0
  );
