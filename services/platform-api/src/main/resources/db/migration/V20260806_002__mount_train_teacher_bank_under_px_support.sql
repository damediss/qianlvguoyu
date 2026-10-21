-- 将「教师库」train_teacher-bank 挂到培训「支持保障」pxzhi_chi_bao_zhang 下，改名为「教务管理」
-- 空库幂等化（踩坑 #34 修复，同 V20260731_003 手法；2026-10-21 补漏——9/29 批次 16 个断裂
-- 迁移清单漏了本文件）：父菜单 pxzhi_chi_bao_zhang 是存量库 legacy 菜单，空库不存在，
-- 子查询返 NULL 撞上 parent_id NOT NULL 断链。父菜单在才挂载/新建；不在则只做属性归一不动 parent_id。
-- 存量库本版本已应用不会重跑（validate-on-migrate=false，application.yml 全局口径）。
SET @db := DATABASE();
SET @has_parent := (SELECT COUNT(*) FROM sys_menu
  WHERE tenant_id = 1 AND code = 'pxzhi_chi_bao_zhang' AND deleted = 0);

SET @sql := IF(@has_parent > 0,
  'UPDATE sys_menu
   SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu
                     WHERE tenant_id = 1 AND code = ''pxzhi_chi_bao_zhang'' AND deleted = 0 LIMIT 1) t),
       menu_type = ''page'', name = ''教务管理'', path = ''/training/academic-mgmt'',
       component = ''training/TrainingAcademicManagePage'', permission_code = ''train:teacher-bank:view'',
       icon = ''ReadOutlined'', sort_order = 3, visible = 1, status = ''enabled'',
       deleted = 0, updated_at = NOW(), updated_by = 1
   WHERE tenant_id = 1 AND code = ''train_teacher-bank''',
  'UPDATE sys_menu
   SET menu_type = ''page'', name = ''教务管理'', path = ''/training/academic-mgmt'',
       component = ''training/TrainingAcademicManagePage'', permission_code = ''train:teacher-bank:view'',
       icon = ''ReadOutlined'', sort_order = 3, visible = 1, status = ''enabled'',
       deleted = 0, updated_at = NOW(), updated_by = 1
   WHERE tenant_id = 1 AND code = ''train_teacher-bank''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 若不存在历史菜单则新建（父菜单在才新建；空库无父菜单时跳过，避免 parent_id NULL 断链）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  4958,
  1,
  (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxzhi_chi_bao_zhang' AND deleted = 0 LIMIT 1),
  'page',
  'train_teacher-bank',
  '教务管理',
  '/training/academic-mgmt',
  'training/TrainingAcademicManagePage',
  'train:teacher-bank:view',
  'ReadOutlined',
  3,
  1,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM DUAL
WHERE @has_parent > 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE tenant_id = 1 AND code = 'train_teacher-bank'
  );

-- 恢复平台管理员角色菜单关联
UPDATE sys_role_menu
SET deleted = 0, updated_at = NOW(), updated_by = 1
WHERE tenant_id = 1
  AND role_id = 2000
  AND menu_id = (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'train_teacher-bank' AND deleted = 0 LIMIT 1)
  AND deleted = 1;

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  93100 + m.id, m.tenant_id, 2000, m.id, NOW(), NOW(), 1, 1, 0
FROM sys_menu m
WHERE m.tenant_id = 1
  AND m.code = 'train_teacher-bank'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.tenant_id = 1 AND rm.role_id = 2000 AND rm.menu_id = m.id
  );

-- 确保师资库权限挂在该菜单并启用
UPDATE sys_permission
SET
  menu_id = (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'train_teacher-bank' AND deleted = 0 LIMIT 1),
  status = 'enabled',
  deleted = 0,
  updated_at = NOW()
WHERE tenant_id = 1
  AND code IN (
    'train:teacher-bank:view',
    'train:teacher-bank:create',
    'train:teacher-bank:update',
    'train:teacher-bank:delete'
  );

-- 权限记录若不存在则补齐（兼容旧库未 seed 的情况）
INSERT INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  v.id,
  1,
  (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'train_teacher-bank' AND deleted = 0 LIMIT 1),
  v.code,
  v.name,
  v.action,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM (
  SELECT 5474 AS id, 'train:teacher-bank:view' AS code, '查看教务管理' AS name, 'view' AS action
  UNION ALL SELECT 5475, 'train:teacher-bank:create', '新增教务人员', 'create'
  UNION ALL SELECT 5476, 'train:teacher-bank:update', '编辑教务人员', 'update'
  UNION ALL SELECT 5477, 'train:teacher-bank:delete', '删除教务人员', 'delete'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.tenant_id = 1 AND p.code = v.code
);

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  94100 + p.id, p.tenant_id, 2000, p.id, NOW(), NOW(), 1, 1, 0
FROM sys_permission p
WHERE p.tenant_id = 1
  AND p.code IN (
    'train:teacher-bank:view',
    'train:teacher-bank:create',
    'train:teacher-bank:update',
    'train:teacher-bank:delete'
  )
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.tenant_id = 1 AND rp.role_id = 2000 AND rp.permission_id = p.id AND rp.deleted = 0
  );

UPDATE sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
SET rp.deleted = 0, rp.updated_at = NOW()
WHERE rp.tenant_id = 1
  AND rp.role_id = 2000
  AND p.code IN (
    'train:teacher-bank:view',
    'train:teacher-bank:create',
    'train:teacher-bank:update',
    'train:teacher-bank:delete'
  )
  AND rp.deleted = 1;
