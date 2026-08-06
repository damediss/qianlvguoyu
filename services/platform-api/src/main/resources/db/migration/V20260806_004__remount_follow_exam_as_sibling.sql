-- 撤回「跟班」目录化：跟班恢复为页面；考试与跟班同级，排在跟班下方

-- 1) 软删「跟班工作台」子菜单
UPDATE sys_menu
SET
  deleted = 1,
  visible = 0,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_follow_workbench'
  AND deleted = 0;

UPDATE sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id
SET
  rm.deleted = 1,
  rm.updated_at = NOW(),
  rm.updated_by = 1
WHERE rm.deleted = 0
  AND m.code = 'train_follow_workbench';

-- 2) 跟班恢复为页面（与 V20260801_001 一致）
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

-- 3) 考试改为与跟班同级，挂在运营流程下，排在跟班之后
UPDATE sys_menu
SET
  parent_id = COALESCE(
    (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxyun_ying_liu_cheng' AND deleted = 0 LIMIT 1) t),
    (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'training_operation_flow' AND deleted = 0 LIMIT 1) t),
    parent_id
  ),
  menu_type = 'page',
  name = '考试',
  path = '/training/follow-exam',
  component = 'training/TrainingFollowExamPage',
  permission_code = 'train:follow-exam:view',
  icon = 'FormOutlined',
  sort_order = 54,
  visible = 1,
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_follow_exam';

-- 若不存在则插入（兼容未跑过 003 的环境）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000003211,
  1,
  COALESCE(
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxyun_ying_liu_cheng' AND deleted = 0 LIMIT 1),
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'training_operation_flow' AND deleted = 0 LIMIT 1),
    4300
  ),
  'page',
  'train_follow_exam',
  '考试',
  '/training/follow-exam',
  'training/TrainingFollowExamPage',
  'train:follow-exam:view',
  'FormOutlined',
  54,
  1,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE tenant_id = 1 AND code = 'train_follow_exam' AND deleted = 0
);

-- 4) 权限（若不存在则补齐；已存在则挂到考试菜单）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000403301,
  1,
  m.id,
  'train:follow-exam:view',
  '查看考试',
  'view',
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'train_follow_exam'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission p
    WHERE p.tenant_id = 1 AND p.code = 'train:follow-exam:view' AND p.deleted = 0
  );

UPDATE sys_permission p
INNER JOIN sys_menu m ON m.tenant_id = p.tenant_id AND m.code = 'train_follow_exam' AND m.deleted = 0
SET
  p.menu_id = m.id,
  p.name = '查看考试',
  p.updated_at = NOW(),
  p.updated_by = 1
WHERE p.tenant_id = 1
  AND p.code = 'train:follow-exam:view'
  AND p.deleted = 0;

-- 5) 有跟班菜单的角色 → 同步考试菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000860000 + ROW_NUMBER() OVER (ORDER BY rm.role_id),
  rm.tenant_id,
  rm.role_id,
  exam.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu follow ON follow.id = rm.menu_id AND follow.tenant_id = rm.tenant_id AND follow.deleted = 0
INNER JOIN sys_menu exam ON exam.tenant_id = rm.tenant_id AND exam.code = 'train_follow_exam' AND exam.deleted = 0
WHERE rm.deleted = 0
  AND follow.code = 'train_follow'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x
    WHERE x.tenant_id = rm.tenant_id AND x.role_id = rm.role_id AND x.menu_id = exam.id AND x.deleted = 0
  );

-- 平台管理员兜底
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000860900,
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
  AND m.code = 'train_follow_exam'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 1 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 6) 考试权限授予有跟班菜单的角色
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000861000 + ROW_NUMBER() OVER (ORDER BY rm.role_id),
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
INNER JOIN sys_permission p ON p.tenant_id = rm.tenant_id AND p.code = 'train:follow-exam:view' AND p.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_follow'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = rm.tenant_id AND rp.role_id = rm.role_id AND rp.permission_id = p.id AND rp.deleted = 0
  );

-- 7) 同步考试计划 API 权限
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000862000 + ROW_NUMBER() OVER (ORDER BY rm.role_id, p.code),
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
INNER JOIN sys_permission p ON p.tenant_id = rm.tenant_id
  AND p.code IN (
    'train:exam-list:view',
    'train:exam-list:create',
    'train:exam-list:update',
    'train:exam-list:delete'
  )
  AND p.deleted = 0
WHERE rm.deleted = 0
  AND m.code = 'train_follow'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = rm.tenant_id AND rp.role_id = rm.role_id AND rp.permission_id = p.id AND rp.deleted = 0
  );
