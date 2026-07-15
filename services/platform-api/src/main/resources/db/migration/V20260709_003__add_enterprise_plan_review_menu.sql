-- 需求管理 → 企业审核（企业端查看机构提交的三年规划方案）

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  207393809000002001, 1, 5045, 'page', 'company_plan_review', '企业审核', '/demand/plan-review',
  'training/EnterprisePlanReviewPage', 'pxqiyeshenhe:view', 'AuditOutlined', 3, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (207393809000102001, 1, 207393809000002001, 'pxqiyeshenhe:view',    '查看企业审核', 'view',    'enabled', NOW(), NOW(), 1, 1, 0),
  (207393809000102002, 1, 207393809000002001, 'pxqiyeshenhe:confirm', '企业确认方案', 'confirm', 'enabled', NOW(), NOW(), 1, 1, 0)
ON DUPLICATE KEY UPDATE
  menu_id = VALUES(menu_id),
  name = VALUES(name),
  action = VALUES(action),
  status = VALUES(status),
  updated_at = NOW(),
  updated_by = 1,
  deleted = 0;

-- 拥有「培训需求」菜单的企业角色，同步授予企业审核菜单
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000302001 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  207393809000002001,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_demand-train';

-- 补齐企业审核菜单下 API 权限
SET @next_role_perm_id := (SELECT IFNULL(MAX(id), 0) FROM sys_role_permission);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_role_perm_id := @next_role_perm_id + 1),
  pairs.tenant_id,
  pairs.role_id,
  pairs.permission_id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM (
  SELECT DISTINCT rm.tenant_id, rm.role_id, p.id AS permission_id
  FROM sys_role_menu rm
  INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
  INNER JOIN sys_permission p ON p.menu_id = 207393809000002001 AND p.tenant_id = m.tenant_id AND p.deleted = 0
  WHERE rm.deleted = 0 AND m.code = 'company_plan_review'
  UNION
  SELECT DISTINCT rm.tenant_id, rm.role_id, p.id AS permission_id
  FROM sys_role_menu rm
  INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
  INNER JOIN sys_permission p ON p.code = 'pxanfangguihua:enterprise_confirm' AND p.tenant_id = m.tenant_id AND p.deleted = 0
  WHERE rm.deleted = 0 AND m.code = 'company_plan_review'
) pairs
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_permission x
  WHERE x.tenant_id = pairs.tenant_id AND x.role_id = pairs.role_id
    AND x.permission_id = pairs.permission_id AND x.deleted = 0
);
