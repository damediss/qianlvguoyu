-- 培训管理 → 运营流程 → 需求管理 / 分班

INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  207393809000003001, 1, 4300, 'directory', 'training_operation_flow', '运营流程',
  '/training/operation-flow', NULL, NULL, 'BranchesOutlined', 2, 1,
  'enabled', NOW(), NOW(), 1, 1, 0
)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_type = VALUES(menu_type),
  name = VALUES(name),
  path = VALUES(path),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1;

UPDATE sys_menu
SET
  parent_id = 207393809000003001,
  name = '需求管理',
  component = 'training/RequirementsManagementPage',
  sort_order = 1,
  visible = 1,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code = 'train_needs';

UPDATE sys_menu
SET
  parent_id = 207393809000003001,
  name = '分班',
  component = 'training/TrainingClassPeoplePage',
  sort_order = 2,
  visible = 1,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND deleted = 0
  AND code = 'train_class-list';

-- 拥有「需求管理」的角色，同步授予「分班」菜单
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000303001 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  4902,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_needs'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu x
    INNER JOIN sys_menu cm ON cm.id = x.menu_id AND cm.tenant_id = x.tenant_id AND cm.deleted = 0
    WHERE x.deleted = 0
      AND x.tenant_id = rm.tenant_id
      AND x.role_id = rm.role_id
      AND cm.code = 'train_class-list'
  );

-- 拥有「需求管理」的角色，补齐分班相关 API 权限
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
  INNER JOIN sys_permission p ON p.menu_id = 4902 AND p.tenant_id = m.tenant_id AND p.deleted = 0
  WHERE rm.deleted = 0 AND m.code = 'train_needs'
) pairs
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_permission x
  WHERE x.tenant_id = pairs.tenant_id AND x.role_id = pairs.role_id
    AND x.permission_id = pairs.permission_id AND x.deleted = 0
);
