-- 运营流程（pxyun_ying_liu_cheng）下新增「跟班」页面，排在信息收集之后
-- 注意：勿占用已使用的 V20260731_005（库中已有竞赛评分权限迁移）

-- 1) 已存在则对齐挂载与可见性
UPDATE sys_menu
SET
  parent_id = COALESCE(
    (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxyun_ying_liu_cheng' AND deleted = 0 LIMIT 1) t),
    parent_id
  ),
  menu_type = 'page',
  name = '跟班',
  path = '/training/follow',
  component = 'training/TrainingFollowClassPage',
  permission_code = 'train:follow:view',
  icon = 'PlayCircleOutlined',
  sort_order = 53,
  visible = 1,
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_follow';

-- 2) 不存在则插入
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000003201,
  1,
  COALESCE(
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxyun_ying_liu_cheng' AND deleted = 0 LIMIT 1),
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'training_operation_flow' AND deleted = 0 LIMIT 1),
    4300
  ),
  'page',
  'train_follow',
  '跟班',
  '/training/follow',
  'training/TrainingFollowClassPage',
  'train:follow:view',
  'PlayCircleOutlined',
  53,
  1,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE tenant_id = 1 AND code = 'train_follow' AND deleted = 0
);

-- 3) 权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000403201,
  1,
  m.id,
  'train:follow:view',
  '查看跟班',
  'view',
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'train_follow'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'train:follow:view' AND p.deleted = 0
  );

-- 4) 平台管理员角色-菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000503201,
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
  AND m.code = 'train_follow'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 1 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 5) 拥有运营流程/信息收集的角色同步授予跟班菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000603000 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  m_follow.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
INNER JOIN sys_menu m_follow ON m_follow.tenant_id = rm.tenant_id AND m_follow.code = 'train_follow' AND m_follow.deleted = 0
WHERE rm.deleted = 0
  AND m.code IN ('pxyun_ying_liu_cheng', 'train_sign-up', 'train_people-list', 'fen_ban')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x
    WHERE x.tenant_id = rm.tenant_id
      AND x.role_id = rm.role_id
      AND x.menu_id = m_follow.id
      AND x.deleted = 0
  );

-- 6) 同步权限给上述角色
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000703000 + rm.role_id,
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
INNER JOIN sys_permission p ON p.tenant_id = rm.tenant_id AND p.code = 'train:follow:view' AND p.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_follow'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = rm.tenant_id
      AND rp.role_id = rm.role_id
      AND rp.permission_id = p.id
      AND rp.deleted = 0
  );
