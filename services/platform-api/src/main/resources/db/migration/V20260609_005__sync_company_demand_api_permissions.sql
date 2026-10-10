-- 拥有「需求管理 / 培训需求 / 人才需求」菜单的角色，补齐菜单下全部 API 权限
-- 此前仅授予 train:company-demand:view，调用 /training/demand-trains 会 403 Forbidden

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
    AND m.code IN ('company_demand', 'train_demand-train', 'train_demand-employee')
) pairs
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_permission x
  WHERE x.tenant_id = pairs.tenant_id
    AND x.role_id = pairs.role_id
    AND x.permission_id = pairs.permission_id
    AND x.deleted = 0
);
