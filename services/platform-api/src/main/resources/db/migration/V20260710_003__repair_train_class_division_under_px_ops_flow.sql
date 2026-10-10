-- 修复「培训管理 → 运营流程 → 需求管理 → 分班」侧栏不显示
-- 原因：生产库运营流程目录 code=pxyun_ying_liu_cheng；train_class-list 曾被软删除且未挂到该目录

-- 1. 恢复「分班」并挂到 pxyun_ying_liu_cheng 下（需求管理之后）
UPDATE sys_menu child
INNER JOIN sys_menu parent
  ON parent.tenant_id = child.tenant_id
 AND parent.code = 'pxyun_ying_liu_cheng'
 AND parent.deleted = 0
SET
  child.parent_id = parent.id,
  child.name = '分班',
  child.component = 'training/TrainingClassPeoplePage',
  child.path = '/training/class-list',
  child.sort_order = 2,
  child.visible = 1,
  child.deleted = 0,
  child.status = 'enabled',
  child.updated_at = NOW(),
  child.updated_by = 1
WHERE child.tenant_id = 1
  AND child.code = 'train_class-list';

-- 2. 机构端「需求管理」挂到同一运营流程目录
UPDATE sys_menu child
INNER JOIN sys_menu parent
  ON parent.tenant_id = child.tenant_id
 AND parent.code = 'pxyun_ying_liu_cheng'
 AND parent.deleted = 0
SET
  child.parent_id = parent.id,
  child.name = '需求管理',
  child.component = 'training/RequirementsManagementPage',
  child.sort_order = 1,
  child.visible = 1,
  child.deleted = 0,
  child.status = 'enabled',
  child.updated_at = NOW(),
  child.updated_by = 1
WHERE child.tenant_id = 1
  AND child.code = 'train_needs';

-- 3. 企业端「需求管理」目录不应挂在培训运营流程下，移回顶层
UPDATE sys_menu
SET
  parent_id = 0,
  sort_order = 29,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'company_demand'
  AND deleted = 0;

-- 4. 隐藏重复的 training_operation_flow（与 pxyun_ying_liu_cheng 重复）
UPDATE sys_menu
SET
  visible = 0,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'training_operation_flow'
  AND deleted = 0;

-- 5. 拥有运营流程/需求管理相关菜单的角色，同步授予「分班」
SET @next_role_menu_id := (SELECT IFNULL(MAX(id), 0) FROM sys_role_menu);

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  (@next_role_menu_id := @next_role_menu_id + 1),
  pairs.tenant_id,
  pairs.role_id,
  pairs.menu_id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM (
  SELECT DISTINCT rm.tenant_id, rm.role_id, class_menu.id AS menu_id
  FROM sys_role_menu rm
  INNER JOIN sys_menu granted
    ON granted.id = rm.menu_id
   AND granted.tenant_id = rm.tenant_id
   AND granted.deleted = 0
  INNER JOIN sys_menu class_menu
    ON class_menu.tenant_id = rm.tenant_id
   AND class_menu.code = 'train_class-list'
   AND class_menu.deleted = 0
  WHERE rm.deleted = 0
    AND granted.code IN (
      'pxyun_ying_liu_cheng',
      'train_needs',
      'company_demand',
      'train_solutions',
      'train_info-list',
      'train_people-list',
      'train_declares',
      'train_sign-up'
    )
) pairs
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_menu x
  WHERE x.tenant_id = pairs.tenant_id
    AND x.role_id = pairs.role_id
    AND x.menu_id = pairs.menu_id
    AND x.deleted = 0
);

-- 6. 补齐分班 API 权限
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
  INNER JOIN sys_menu m
    ON m.id = rm.menu_id
   AND m.tenant_id = rm.tenant_id
   AND m.deleted = 0
  INNER JOIN sys_menu class_menu
    ON class_menu.tenant_id = m.tenant_id
   AND class_menu.code = 'train_class-list'
   AND class_menu.deleted = 0
  INNER JOIN sys_permission p
    ON p.menu_id = class_menu.id
   AND p.tenant_id = class_menu.tenant_id
   AND p.deleted = 0
  WHERE rm.deleted = 0
    AND m.code = 'train_class-list'
) pairs
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_permission x
  WHERE x.tenant_id = pairs.tenant_id
    AND x.role_id = pairs.role_id
    AND x.permission_id = pairs.permission_id
    AND x.deleted = 0
);
