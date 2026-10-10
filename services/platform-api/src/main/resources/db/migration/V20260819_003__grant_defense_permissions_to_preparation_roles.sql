-- 综合答辩权限补授：认定筹备可见角色 + 平台管理员 role_id=2000
-- 避免只按 PLATFORM_ADMIN 代码授权导致管理端打开综合答辩配置 403 Forbidden

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608190201, 1, 6310, 'certification.defense.config', '配置综合答辩', 'config', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.defense.config' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608190202, 1, 6310, 'certification.defense.upload', '上传业绩评估报告', 'upload', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.defense.upload' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608190203, 1, 6310, 'certification.defense.review', '考评员阅览业绩评估报告', 'review', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.defense.review' AND deleted = 0
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202608193000 + ROW_NUMBER() OVER (ORDER BY rp.role_id, p.id),
  rp.tenant_id,
  rp.role_id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role_permission rp
JOIN sys_permission base_permission
  ON base_permission.id = rp.permission_id
  AND base_permission.tenant_id = rp.tenant_id
  AND base_permission.deleted = 0
JOIN sys_permission p
  ON p.tenant_id = rp.tenant_id
  AND p.deleted = 0
  AND p.code IN (
    'certification.defense.config',
    'certification.defense.upload'
  )
WHERE rp.tenant_id = 1
  AND rp.deleted = 0
  AND base_permission.code = 'certification.preparation.task.view'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = rp.tenant_id
      AND existing.role_id = rp.role_id
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202608193500 + ROW_NUMBER() OVER (ORDER BY p.id),
  1,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'certification.defense.config',
    'certification.defense.upload',
    'certification.defense.review'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = 2000
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202608193800 + ROW_NUMBER() OVER (ORDER BY r.id, p.id),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.deleted = 0
  AND p.code IN (
    'certification.defense.config',
    'certification.defense.upload',
    'certification.defense.review'
  )
WHERE r.deleted = 0
  AND UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN', 'CERTIFICATION_DEV', 'CERTIFICATION_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = r.tenant_id
      AND existing.role_id = r.id
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202608193900 + ROW_NUMBER() OVER (ORDER BY r.id),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.deleted = 0
  AND p.code = 'certification.defense.review'
WHERE r.deleted = 0
  AND r.code = 'CERTIFICATION_ASSESSOR'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.tenant_id = r.tenant_id
      AND existing.role_id = r.id
      AND existing.permission_id = p.id
      AND existing.deleted = 0
  );
