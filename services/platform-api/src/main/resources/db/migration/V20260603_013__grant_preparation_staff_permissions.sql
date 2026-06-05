-- 修复 V20260603_012 中 role_permission 主键冲突导致考务配置权限未授予的问题
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9072000 + rp.role_id * 10 + (p.id - 6996), rp.tenant_id, rp.role_id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
CROSS JOIN sys_permission p
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND rp.permission_id IN (6933, 6935, 6921)
  AND p.tenant_id = 1
  AND p.deleted = 0
  AND p.id IN (6997, 6998);
