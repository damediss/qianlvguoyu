-- 平台超级管理员（PLATFORM_ADMIN / admin）与库中全部菜单、权限保持同步
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9800000 + p.id, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.status = 'enabled'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = p.tenant_id
      AND rp.role_id = 2000
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9801000 + m.id, m.tenant_id, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.deleted = 0
  AND m.status = 'enabled'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.tenant_id = m.tenant_id
      AND rm.role_id = 2000
      AND rm.menu_id = m.id
      AND rm.deleted = 0
  );

INSERT IGNORE INTO sys_user_role (
  id, tenant_id, user_id, role_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  3000, 1, 1000, 2000, NOW(), NOW(), 1, 1, 0
);
