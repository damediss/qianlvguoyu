INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  6380, 1, 6300, 'page', 'certification_practical_collection', '实操采集',
  '/certification/practical-collection', 'certification/PracticalCollection',
  'certification.practical.collection.view', 'ScanOutlined', 4, 1,
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
  WHEN 'certification_dashboard' THEN 1
  WHEN 'certification_project' THEN 2
  WHEN 'certification_preparation' THEN 3
  WHEN 'certification_practical_collection' THEN 4
  WHEN 'certification_practical_config' THEN 5
  WHEN 'certification_pre_exam' THEN 6
  WHEN 'certification_info' THEN 7
  WHEN 'certification_archive' THEN 8
  WHEN 'certification_task_archive' THEN 9
  ELSE sort_order
END,
updated_at = NOW(),
updated_by = 1
WHERE tenant_id = 1
  AND parent_id = 6300
  AND deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (202607010201, 1, 6380, 'certification.practical.collection.view', '查看实操采集', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202607010202, 1, 6380, 'certification.practical.collection.operate', '操作实操采集', 'operate', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202607010203, 1, 6380, 'certification.practical.collection.manage', '管理实操采集', 'manage', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202607010204, 1, 6380, 'certification.practical.collection.exception', '处理实操采集异常', 'exception', 'enabled', NOW(), NOW(), 1, 1, 0),
  (202607010205, 1, 6380, 'certification.practical.collection.export', '导出实操采集二维码', 'export', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  deleted = VALUES(deleted),
  updated_at = NOW(),
  updated_by = 1;

INSERT INTO sys_role (
  id, tenant_id, code, name, role_type, status, remark,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (
    202607010210, 1, 'CERTIFICATION_COLLECTOR', '实操采集员', 'business', 'enabled',
    '用于平板端实操信息采集工作台', NOW(), NOW(), 1, 1, 0
  ),
  (
    202607010211, 1, 'CERTIFICATION_COLLECTION_LEADER', '实操采集组长', 'business', 'enabled',
    '用于平板端实操采集异常处理和现场协调', NOW(), NOW(), 1, 1, 0
  )
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  remark = VALUES(remark),
  deleted = VALUES(deleted),
  updated_at = NOW(),
  updated_by = 1;

SET @next_practical_collect_role_menu_id = (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_menu
);

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_practical_collect_role_menu_id := @next_practical_collect_role_menu_id + 1),
  r.tenant_id,
  r.id,
  6380,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
WHERE r.tenant_id = 1
  AND UPPER(r.code) IN (
    'PLATFORM_ADMIN',
    'SUPER_ADMIN',
    'CERTIFICATION_DEV',
    'CERTIFICATION_COLLECTOR',
    'CERTIFICATION_COLLECTION_LEADER'
  )
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.tenant_id = r.tenant_id
      AND rm.role_id = r.id
      AND rm.menu_id = 6380
      AND rm.deleted = 0
  );

SET @next_practical_collect_role_permission_id = (
  SELECT COALESCE(MAX(id), 0) FROM sys_role_permission
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_practical_collect_role_permission_id := @next_practical_collect_role_permission_id + 1),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.code IN (
    'certification.practical.collection.view',
    'certification.practical.collection.operate',
    'certification.practical.collection.manage',
    'certification.practical.collection.exception',
    'certification.practical.collection.export'
  )
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

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_practical_collect_role_permission_id := @next_practical_collect_role_permission_id + 1),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.code IN (
    'certification.practical.collection.view',
    'certification.practical.collection.operate'
  )
  AND p.deleted = 0
WHERE r.tenant_id = 1
  AND UPPER(r.code) = 'CERTIFICATION_COLLECTOR'
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_practical_collect_role_permission_id := @next_practical_collect_role_permission_id + 1),
  r.tenant_id,
  r.id,
  p.id,
  NOW(), NOW(), 1, 1, 0
FROM sys_role r
JOIN sys_permission p
  ON p.tenant_id = r.tenant_id
  AND p.code IN (
    'certification.practical.collection.view',
    'certification.practical.collection.operate',
    'certification.practical.collection.exception',
    'certification.practical.collection.export'
  )
  AND p.deleted = 0
WHERE r.tenant_id = 1
  AND UPPER(r.code) = 'CERTIFICATION_COLLECTION_LEADER'
  AND r.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
