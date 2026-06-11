INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202606100301, 1, 6310, 'certification.preparation.document.generate', '生成报名信息表', 'generate', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.preparation.document.generate' AND deleted = 0
);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, created_at, updated_at, created_by, updated_by, deleted)
SELECT 202606100401 + r.id, r.id, 202606100301, 1, NOW(), NOW(), 1, 1, 0
FROM sys_role r
WHERE r.code IN ('platform_admin', 'super_admin')
  AND EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = 202606100301 AND p.deleted = 0)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = 202606100301 AND rp.deleted = 0
  );
