-- 修复 train_sign-up 被软删除导致「培训管理」下无子菜单

UPDATE sys_menu child
INNER JOIN sys_menu parent
  ON parent.tenant_id = child.tenant_id
 AND parent.code = 'train_management'
 AND parent.deleted = 0
SET child.parent_id = parent.id,
    child.deleted = 0,
    child.visible = 1,
    child.status = 'enabled',
    child.name = '自主报名',
    child.component = 'training/TrainingInformationPage',
    child.sort_order = 1,
    child.updated_at = NOW(),
    child.updated_by = 1
WHERE child.tenant_id = 1
  AND child.code = 'train_sign-up';

SET @next_role_menu_id := (SELECT IFNULL(MAX(id), 0) FROM sys_role_menu);

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_role_menu_id := @next_role_menu_id + 1),
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
 AND m.code IN ('train_management', 'train_sign-up')
WHERE r.tenant_id = 1
  AND r.deleted = 0
  AND r.code IN ('PLATFORM_ADMIN', 'train', 'TRAINING_MANAGER')
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.tenant_id = r.tenant_id
      AND rm.role_id = r.id
      AND rm.menu_id = m.id
      AND rm.deleted = 0
  );

SET @next_role_perm_id := (SELECT IFNULL(MAX(id), 0) FROM sys_role_permission);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_role_perm_id := @next_role_perm_id + 1),
  pairs.tenant_id,
  pairs.role_id,
  pairs.permission_id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM (
  SELECT DISTINCT
    rm.tenant_id,
    rm.role_id,
    p.id AS permission_id
  FROM sys_role_menu rm
  INNER JOIN sys_menu m
    ON m.id = rm.menu_id
   AND m.tenant_id = rm.tenant_id
   AND m.deleted = 0
  INNER JOIN sys_permission p
    ON p.menu_id = m.id
   AND p.tenant_id = m.tenant_id
   AND p.deleted = 0
   AND p.status = 'enabled'
  WHERE rm.deleted = 0
    AND m.code IN ('train_management', 'train_sign-up')
) pairs
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_permission x
  WHERE x.tenant_id = pairs.tenant_id
    AND x.role_id = pairs.role_id
    AND x.permission_id = pairs.permission_id
    AND x.deleted = 0
);
