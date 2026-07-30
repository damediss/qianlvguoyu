INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 908000 + p.id - 6900, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'certification.project.document.submit',
    'certification.project.document.initialReview',
    'certification.project.document.finalReview',
    'certification.project.document.workflowView'
  );
