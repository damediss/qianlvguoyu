-- Repair preparation equipment permissions that may have been skipped when
-- historical permission IDs 7001-7003 were already occupied.
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 7601, 1, 6310, 'certification.preparation.equipment.view', '查看配置设备', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission
  WHERE tenant_id = 1 AND code = 'certification.preparation.equipment.view' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 7602, 1, 6310, 'certification.preparation.equipment.assign', '配置任务设备', 'assign', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission
  WHERE tenant_id = 1 AND code = 'certification.preparation.equipment.assign' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 7603, 1, 6310, 'certification.preparation.equipment.bind', '绑定任务设备', 'bind', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission
  WHERE tenant_id = 1 AND code = 'certification.preparation.equipment.bind' AND deleted = 0
);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  9100000 + rp.role_id * 10 +
    CASE p.code
      WHEN 'certification.preparation.equipment.view' THEN 1
      WHEN 'certification.preparation.equipment.assign' THEN 2
      WHEN 'certification.preparation.equipment.bind' THEN 3
    END,
  rp.tenant_id,
  rp.role_id,
  p.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_permission rp
JOIN sys_permission base_permission
  ON base_permission.id = rp.permission_id
  AND base_permission.tenant_id = rp.tenant_id
  AND base_permission.deleted = 0
JOIN sys_permission p
  ON p.tenant_id = rp.tenant_id
  AND p.deleted = 0
  AND p.status = 'enabled'
  AND p.code IN (
    'certification.preparation.equipment.view',
    'certification.preparation.equipment.assign',
    'certification.preparation.equipment.bind'
  )
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND base_permission.code = 'certification.preparation.task.view';
