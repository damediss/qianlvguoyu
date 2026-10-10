INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202608190201, 1, 6310, 'certification.defense.config', '配置综合答辩', 'config', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202608190202, 1, 6310, 'certification.defense.upload', '上传业绩评估报告', 'upload', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202608190203, 1, 6310, 'certification.defense.review', '考评员阅览业绩评估报告', 'review', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW();

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608191000 + (p.id % 1000) + (r.id % 100), 1, r.id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.code IN (
    'certification.defense.config',
    'certification.defense.upload',
    'certification.defense.review'
  )
  AND p.tenant_id = 1
  AND p.deleted = 0
WHERE UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN')
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608190300 + (p.id % 100), 1, r.id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.code = 'certification.defense.review'
  AND p.tenant_id = 1
  AND p.deleted = 0
WHERE r.code = 'CERTIFICATION_ASSESSOR'
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0
  );
