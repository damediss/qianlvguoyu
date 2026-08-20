-- 认定运营流程新增「答辩管理」
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  6395, 1, 6391, 'page', 'certification_defense_management', '答辩管理',
  '/certification/defense-management', 'certification/DefenseManagement',
  'certification.defense.progress', 'AuditOutlined', 5, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = VALUES(deleted),
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET sort_order = CASE code
  WHEN 'certification_project' THEN 1
  WHEN 'certification_preparation' THEN 2
  WHEN 'certification_practical_collection' THEN 3
  WHEN 'certification_practical_config' THEN 4
  WHEN 'certification_defense_management' THEN 5
  WHEN 'certification_archive' THEN 6
  ELSE sort_order
END,
updated_at = NOW(),
updated_by = 1
WHERE tenant_id = 1
  AND parent_id = 6391
  AND deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608190204, 1, 6395, 'certification.defense.progress', '查看答辩管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.defense.progress' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608190205, 1, 6395, 'certification.defense.complete', '完成答辩', 'complete', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.defense.complete' AND deleted = 0
);

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 202608190206, 1, 6395, 'certification.defense.export', '导出答辩成绩汇总表', 'export', 'enabled', NOW(), NOW(), 1, 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE tenant_id = 1 AND code = 'certification.defense.export' AND deleted = 0
);

INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202608194000 + r.id,
  r.tenant_id,
  r.id,
  6395,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
WHERE r.tenant_id = 1
  AND UPPER(r.code) IN ('PLATFORM_ADMIN', 'SUPER_ADMIN', 'CERTIFICATION_DEV', 'CERTIFICATION_ADMIN')
  AND r.deleted = 0;

-- 能看到认定筹备或实操管理的角色同步授予答辩管理，避免只按角色码漏授
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202608195000 + ROW_NUMBER() OVER (ORDER BY roles.role_id),
  roles.tenant_id,
  roles.role_id,
  6395,
  NOW(), NOW(), 1, 1, 0
FROM (
  SELECT DISTINCT rm.tenant_id, rm.role_id
  FROM sys_role_menu rm
  WHERE rm.tenant_id = 1
    AND rm.deleted = 0
    AND rm.menu_id = 6370
) roles
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu existing
  WHERE existing.tenant_id = roles.tenant_id
    AND existing.role_id = roles.role_id
    AND existing.menu_id = 6395
    AND existing.deleted = 0
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202608194100 + ROW_NUMBER() OVER (ORDER BY rp.role_id, p.id),
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
    'certification.defense.progress',
    'certification.defense.complete',
    'certification.defense.export'
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
  202608194500 + ROW_NUMBER() OVER (ORDER BY p.id),
  1,
  2000,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.deleted = 0
  AND p.code IN (
    'certification.defense.progress',
    'certification.defense.complete',
    'certification.defense.export'
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
  202608194800 + ROW_NUMBER() OVER (ORDER BY r.id, p.id),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.deleted = 0
  AND p.code IN (
    'certification.defense.progress',
    'certification.defense.complete',
    'certification.defense.export'
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
