-- 运营流程下新增「申报补贴」页面，与跟班/考试同级，排在考试下方

-- 1) 已存在则对齐挂载
UPDATE sys_menu
SET
  parent_id = COALESCE(
    (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxyun_ying_liu_cheng' AND deleted = 0 LIMIT 1) t),
    (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'training_operation_flow' AND deleted = 0 LIMIT 1) t),
    parent_id
  ),
  menu_type = 'page',
  name = '申报补贴',
  path = '/training/subsidy-declare',
  component = 'training/TrainingSubsidyDeclarePage',
  permission_code = 'train:subsidy-declare:view',
  icon = 'AccountBookOutlined',
  sort_order = 55,
  visible = 1,
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_subsidy_declare';

-- 2) 不存在则插入
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000003212,
  1,
  COALESCE(
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxyun_ying_liu_cheng' AND deleted = 0 LIMIT 1),
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'training_operation_flow' AND deleted = 0 LIMIT 1),
    4300
  ),
  'page',
  'train_subsidy_declare',
  '申报补贴',
  '/training/subsidy-declare',
  'training/TrainingSubsidyDeclarePage',
  'train:subsidy-declare:view',
  'AccountBookOutlined',
  55,
  1,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE tenant_id = 1 AND code = 'train_subsidy_declare' AND deleted = 0
);

-- 3) 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000403302,
  1,
  m.id,
  'train:subsidy-declare:view',
  '查看申报补贴',
  'view',
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'train_subsidy_declare'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'train:subsidy-declare:view' AND p.deleted = 0
  );

UPDATE sys_permission p
INNER JOIN sys_menu m ON m.tenant_id = p.tenant_id AND m.code = 'train_subsidy_declare' AND m.deleted = 0
SET
  p.menu_id = m.id,
  p.name = '查看申报补贴',
  p.updated_at = NOW(),
  p.updated_by = 1
WHERE p.tenant_id = 1
  AND p.code = 'train:subsidy-declare:view'
  AND p.deleted = 0;

-- 4) 有跟班/考试菜单的角色 → 同步申报补贴菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000863000 + ROW_NUMBER() OVER (ORDER BY rm.role_id),
  rm.tenant_id,
  rm.role_id,
  subsidy.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM (
  SELECT DISTINCT rm.tenant_id, rm.role_id
  FROM sys_role_menu rm
  INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
  WHERE rm.deleted = 0
    AND m.code IN ('train_follow', 'train_follow_exam')
) rm
INNER JOIN sys_menu subsidy ON subsidy.tenant_id = rm.tenant_id AND subsidy.code = 'train_subsidy_declare' AND subsidy.deleted = 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu x
  WHERE x.tenant_id = rm.tenant_id AND x.role_id = rm.role_id AND x.menu_id = subsidy.id AND x.deleted = 0
);

-- 平台管理员兜底
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000863900,
  1,
  2000,
  m.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'train_subsidy_declare'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 1 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 5) 权限授予有跟班菜单的角色
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000864000 + ROW_NUMBER() OVER (ORDER BY rm.role_id),
  rm.tenant_id,
  rm.role_id,
  p.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
INNER JOIN sys_permission p ON p.tenant_id = rm.tenant_id AND p.code = 'train:subsidy-declare:view' AND p.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_follow'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = rm.tenant_id AND rp.role_id = rm.role_id AND rp.permission_id = p.id AND rp.deleted = 0
  );
