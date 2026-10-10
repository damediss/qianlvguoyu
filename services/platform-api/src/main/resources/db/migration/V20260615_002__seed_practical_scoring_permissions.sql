INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202606150201, 1, 6310, 'certification.practical.config', '配置实操评分', 'config', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202606150202, 1, 6310, 'certification.practical.generate', '生成实操评分任务', 'generate', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202606150203, 1, 6310, 'certification.practical.progress', '查看实操评分进度', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202606150204, 1, 6310, 'certification.practical.return', '退回实操评分', 'return', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202606150205, 1, 6310, 'certification.practical.score', '考评员实操评分', 'score', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW();

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202606151000 + p.id % 1000 + r.id, 1, r.id, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.code IN (
    'certification.practical.config',
    'certification.practical.generate',
    'certification.practical.progress',
    'certification.practical.return',
    'certification.practical.score'
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

INSERT INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  202606150210, 1, 'CERTIFICATION_ASSESSOR', '实操考评员', 'business', 'enabled',
  '仅用于平板实操独立评分', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  updated_at = NOW();

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202606150300 + p.id % 100, 1, 202606150210, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.code = 'certification.practical.score'
  AND p.tenant_id = 1
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 202606150210 AND rp.permission_id = p.id AND rp.deleted = 0
  );

SET @next_practical_user_role_id = (
  SELECT COALESCE(MAX(id), 0) FROM sys_user_role
);

INSERT INTO sys_user_role (
  id, tenant_id, user_id, role_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_practical_user_role_id := @next_practical_user_role_id + 1),
  a.tenant_id,
  a.user_id,
  202606150210,
  NOW(), NOW(), 1, 1, 0
FROM certification_assessor a
WHERE a.tenant_id = 1
  AND a.user_id IS NOT NULL
  AND a.audit_status = 'approved'
  AND a.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur
    WHERE ur.tenant_id = a.tenant_id
      AND ur.user_id = a.user_id
      AND ur.role_id = 202606150210
      AND ur.deleted = 0
  );
