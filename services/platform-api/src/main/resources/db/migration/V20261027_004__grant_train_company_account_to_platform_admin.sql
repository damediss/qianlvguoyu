-- 兜底：确保 PLATFORM_ADMIN / SUPER_ADMIN 拥有 train:company:account（开设企业账号）

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027002000100000 + ROW_NUMBER() OVER (ORDER BY m.tenant_id) - 1,
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

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  210027002000200000 + ROW_NUMBER() OVER (ORDER BY r.tenant_id, r.id, p.id) - 1,
  r.tenant_id,
  r.id,
  p.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role r
INNER JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
 AND p.code = 'train:company:account'
 AND p.deleted = 0
WHERE r.deleted = 0
  AND (
    r.id = 2000
    OR UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN')
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
