UPDATE sys_menu
SET name = '实操管理',
    permission_code = 'certification.practical.progress',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'certification_practical_config'
  AND deleted = 0;

UPDATE sys_permission
SET name = '查看实操考试进度',
    updated_at = NOW(),
    updated_by = 1
WHERE tenant_id = 1
  AND code = 'certification.practical.progress'
  AND deleted = 0;

SET @next_practical_progress_role_permission_id = (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_practical_progress_role_permission_id := @next_practical_progress_role_permission_id + 1),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.code = 'certification.practical.progress'
  AND p.deleted = 0
WHERE r.tenant_id = 1
  AND UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN', 'CERTIFICATION_DEV')
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
