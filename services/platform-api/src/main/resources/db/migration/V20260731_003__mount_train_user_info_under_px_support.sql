-- 将「学员信息」train_user-info（TrainingStudentPage）挂到培训「支持保障」pxzhi_chi_bao_zhang 下并启用

UPDATE sys_menu
SET
  parent_id = (
    SELECT id FROM (
      SELECT id FROM sys_menu
      WHERE tenant_id = 1 AND code = 'pxzhi_chi_bao_zhang' AND deleted = 0
      LIMIT 1
    ) t
  ),
  menu_type = 'page',
  name = '学员信息',
  path = '/training/user-info',
  component = 'training/TrainingStudentPage',
  permission_code = 'train:user-info:view',
  icon = 'TeamOutlined',
  sort_order = 2,
  visible = 1,
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_user-info';

-- 恢复平台管理员角色菜单关联（若曾软删则复活）
UPDATE sys_role_menu
SET deleted = 0, updated_at = NOW(), updated_by = 1
WHERE tenant_id = 1
  AND role_id = 2000
  AND menu_id = (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'train_user-info' AND deleted = 0 LIMIT 1)
  AND deleted = 1;

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  93000 + m.id, m.tenant_id, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'train_user-info'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 1 AND rm.role_id = 2000 AND rm.menu_id = m.id
  );

-- 确保学员信息相关权限仍挂在该菜单，并恢复管理员角色权限
UPDATE sys_permission
SET
  menu_id = (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'train_user-info' AND deleted = 0 LIMIT 1),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW()
WHERE tenant_id = 1
  AND code IN (
    'train:user-info:view',
    'train:user-info:create',
    'train:user-info:update',
    'train:user-info:delete'
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  94000 + p.id, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.code IN (
    'train:user-info:view',
    'train:user-info:create',
    'train:user-info:update',
    'train:user-info:delete'
  )
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 1 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );

UPDATE sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
SET rp.deleted = 0, rp.updated_at = NOW()
WHERE rp.tenant_id = 1
  AND rp.role_id = 2000
  AND p.code IN (
    'train:user-info:view',
    'train:user-info:create',
    'train:user-info:update',
    'train:user-info:delete'
  )
  AND rp.deleted = 1;
