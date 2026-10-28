-- 企业管理：开设账号权限 + 默认企业角色 TRAIN_ENTERPRISE
-- 多租户：按已有 train_company 菜单租户播种

-- 1) 权限码 train:company:account
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027001000100000 + ROW_NUMBER() OVER (ORDER BY m.tenant_id) - 1,
  m.tenant_id,
  m.id,
  'train:company:account',
  '企业开设账号',
  'update',
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
WHERE m.code = 'train_company'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = m.tenant_id AND p.code = 'train:company:account' AND p.deleted = 0
  );

UPDATE sys_permission p
INNER JOIN sys_menu m ON m.tenant_id = p.tenant_id AND m.code = 'train_company' AND m.deleted = 0
SET
  p.menu_id = m.id,
  p.name = '企业开设账号',
  p.action = 'update',
  p.status = 'enabled',
  p.deleted = 0,
  p.updated_at = NOW(),
  p.updated_by = 1
WHERE p.code = 'train:company:account';

-- 2) 已有企业管理菜单的角色 → 授予 account
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027001000200000 + ROW_NUMBER() OVER (ORDER BY rm.tenant_id, rm.role_id, p.id) - 1,
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
  AND p.code = 'train:company:account'
  AND p.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_company'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = rm.tenant_id
      AND rp.role_id = rm.role_id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

-- 3) 默认企业角色 TRAIN_ENTERPRISE（每租户一个）
INSERT INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027001000300000 + ROW_NUMBER() OVER (ORDER BY t.tenant_id) - 1,
  t.tenant_id,
  'TRAIN_ENTERPRISE',
  '企业运营账号',
  'business',
  'enabled',
  '培训企业管理开设账号默认角色；数据范围由所属企业 org 决定',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM (
  SELECT DISTINCT tenant_id FROM sys_menu WHERE code = 'train_company' AND deleted = 0
) t
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role r
  WHERE r.tenant_id = t.tenant_id AND r.code = 'TRAIN_ENTERPRISE' AND r.deleted = 0
);

-- 4) TRAIN_ENTERPRISE：挂培训业务常用菜单（企业管理 + 需求管理树 + 跟班考试等已存在菜单）
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027001000400000 + ROW_NUMBER() OVER (ORDER BY r.tenant_id, r.id, m.id) - 1,
  r.tenant_id,
  r.id,
  m.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role r
INNER JOIN sys_menu m ON m.tenant_id = r.tenant_id AND m.deleted = 0
  AND m.code IN (
    'train_company',
    'company_demand',
    'train_demand-train',
    'train_demand-employee',
    'pxyan_jiu',
    'pxgen_ban',
    'train_follow_exam',
    'train_follow_attendance',
    'pxzhi_chi_bao_zhang',
    'pxyun_ying'
  )
WHERE r.code = 'TRAIN_ENTERPRISE'
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = r.tenant_id
      AND rm.role_id = r.id
      AND rm.menu_id = m.id
      AND rm.deleted = 0
  );

-- 5) TRAIN_ENTERPRISE：授予上述菜单下全部权限 + train:company:account
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027001000500000 + ROW_NUMBER() OVER (ORDER BY r.tenant_id, r.id, p.id) - 1,
  r.tenant_id,
  r.id,
  p.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role r
INNER JOIN sys_permission p ON p.tenant_id = r.tenant_id AND p.deleted = 0 AND p.status = 'enabled'
INNER JOIN sys_menu m ON m.id = p.menu_id AND m.tenant_id = p.tenant_id AND m.deleted = 0
  AND m.code IN (
    'train_company',
    'company_demand',
    'train_demand-train',
    'train_demand-employee',
    'pxyan_jiu',
    'pxgen_ban',
    'train_follow_exam',
    'train_follow_attendance',
    'pxzhi_chi_bao_zhang',
    'pxyun_ying'
  )
WHERE r.code = 'TRAIN_ENTERPRISE'
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
