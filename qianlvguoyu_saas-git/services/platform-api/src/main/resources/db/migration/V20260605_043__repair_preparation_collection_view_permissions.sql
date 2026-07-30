-- Grant information collection read permissions to roles that can already
-- open the certification preparation page.
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  9200000 + rp.role_id * 100 +
    CASE p.code
      WHEN 'certification.preparation.candidate.view' THEN 1
      WHEN 'certification.preparation.document.view' THEN 2
      WHEN 'certification.preparation.material.view' THEN 3
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
    'certification.preparation.candidate.view',
    'certification.preparation.document.view',
    'certification.preparation.material.view'
  )
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND base_permission.code = 'certification.preparation.task.view';
