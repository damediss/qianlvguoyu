INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (7003, 1, 6310, 'certification.preparation.equipment.bind', '绑定任务设备', 'bind', 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 9074000 + rp.role_id * 10 + (p.id - 7000), rp.tenant_id, rp.role_id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
CROSS JOIN sys_permission p
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND rp.permission_id IN (7001, 7002, 6933, 6935, 6921)
  AND p.tenant_id = 1
  AND p.deleted = 0
  AND p.id = 7003;
