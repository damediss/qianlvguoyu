-- 为 PLATFORM_ADMIN / SUPER_ADMIN 授予方案规划（三年规划）完整权限

SET @next_typ_role_menu_id = (SELECT COALESCE(MAX(id), 0) FROM sys_role_menu);

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_typ_role_menu_id := @next_typ_role_menu_id + 1),
  r.tenant_id,
  r.id,
  207393809000001002,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
WHERE r.tenant_id = 1
  AND UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN')
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = r.tenant_id
      AND rm.role_id = r.id
      AND rm.menu_id = 207393809000001002
      AND rm.deleted = 0
  );

SET @next_typ_role_perm_id = (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_typ_role_perm_id := @next_typ_role_perm_id + 1),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.code IN ('pxanfangguihua:view', 'pxanfangguihua:manage', 'pxanfangguihua:enterprise_confirm')
  AND p.deleted = 0
WHERE r.tenant_id = 1
  AND UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN')
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
