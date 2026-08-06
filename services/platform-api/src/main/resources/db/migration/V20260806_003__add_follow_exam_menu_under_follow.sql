-- 跟班改为目录，下挂「跟班工作台」「考试」

-- 1) 跟班 → directory
UPDATE sys_menu
SET
  menu_type = 'directory',
  name = '跟班',
  path = '/training/follow',
  component = NULL,
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

-- 2) 跟班工作台（原跟班页）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000003210,
  1,
  m.id,
  'page',
  'train_follow_workbench',
  '跟班工作台',
  '/training/follow/workbench',
  'training/TrainingFollowClassPage',
  'train:follow:view',
  'PlayCircleOutlined',
  1,
  1,
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
    SELECT 1 FROM sys_menu c
    WHERE c.tenant_id = 1 AND c.code = 'train_follow_workbench' AND c.deleted = 0
  );

UPDATE sys_menu
SET
  parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'train_follow' AND deleted = 0 LIMIT 1) t),
  menu_type = 'page',
  name = '跟班工作台',
  path = '/training/follow/workbench',
  component = 'training/TrainingFollowClassPage',
  permission_code = 'train:follow:view',
  icon = 'PlayCircleOutlined',
  sort_order = 1,
  visible = 1,
  status = 'enabled',
  deleted = 0,
  updated_at = NOW()
WHERE tenant_id = 1
  AND code = 'train_follow_workbench';

-- 3) 考试
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000003211,
  1,
  m.id,
  'page',
  'train_follow_exam',
  '考试',
  '/training/follow/exam',
  'training/TrainingFollowExamPage',
  'train:follow-exam:view',
  'FormOutlined',
  2,
  1,
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
    SELECT 1 FROM sys_menu c
    WHERE c.tenant_id = 1 AND c.code = 'train_follow_exam' AND c.deleted = 0
  );

UPDATE sys_menu
SET
  parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'train_follow' AND deleted = 0 LIMIT 1) t),
  menu_type = 'page',
  name = '考试',
  path = '/training/follow/exam',
  component = 'training/TrainingFollowExamPage',
  permission_code = 'train:follow-exam:view',
  icon = 'FormOutlined',
  sort_order = 2,
  visible = 1,
  status = 'enabled',
  deleted = 0,
  updated_at = NOW()
WHERE tenant_id = 1
  AND code = 'train_follow_exam';

-- 4) 考试权限
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000403301,
  1,
  m.id,
  'train:follow-exam:view',
  '查看跟班考试',
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

-- 5) 有跟班菜单的角色 → 同步工作台 + 考试菜单
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000610000 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  child.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu parent ON parent.id = rm.menu_id AND parent.tenant_id = rm.tenant_id AND parent.deleted = 0
INNER JOIN sys_menu child ON child.tenant_id = rm.tenant_id AND child.code = 'train_follow_workbench' AND child.deleted = 0
WHERE rm.deleted = 0
  AND parent.code = 'train_follow'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x
    WHERE x.tenant_id = rm.tenant_id AND x.role_id = rm.role_id AND x.menu_id = child.id AND x.deleted = 0
  );

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000620000 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  child.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu parent ON parent.id = rm.menu_id AND parent.tenant_id = rm.tenant_id AND parent.deleted = 0
INNER JOIN sys_menu child ON child.tenant_id = rm.tenant_id AND child.code = 'train_follow_exam' AND child.deleted = 0
WHERE rm.deleted = 0
  AND parent.code = 'train_follow'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu x
    WHERE x.tenant_id = rm.tenant_id AND x.role_id = rm.role_id AND x.menu_id = child.id AND x.deleted = 0
  );

-- 平台管理员兜底
INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000503210 + m.id % 1000,
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
  AND m.code IN ('train_follow_workbench', 'train_follow_exam')
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 1 AND rm.role_id = 2000 AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 6) 考试权限授予有「跟班」目录菜单的角色（勿同时匹配子菜单，否则同一 role 会生成重复主键）
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000850000 + ROW_NUMBER() OVER (ORDER BY rm.role_id),
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

-- 7) 同步考试计划 API 权限（页面复用 exam-lists）
INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000851000 + ROW_NUMBER() OVER (ORDER BY rm.role_id, p.code),
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
