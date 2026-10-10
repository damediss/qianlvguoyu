-- 统一运营流程「分班」菜单：fen_ban → TrainingClassDivisionPage

-- 1) 若已有 fen_ban：对齐组件与可见性
UPDATE sys_menu
SET
  name = '分班',
  component = 'training/TrainingClassDivisionPage',
  path = COALESCE(NULLIF(path, ''), '/training/fen-ban'),
  visible = 1,
  status = 'enabled',
  deleted = 0,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'fen_ban';

-- 2) 若无 fen_ban：挂到运营流程目录（优先 pxyun_ying_liu_cheng，其次 training_operation_flow）
INSERT INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000003101,
  1,
  COALESCE(
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'pxyun_ying_liu_cheng' AND deleted = 0 LIMIT 1),
    (SELECT id FROM sys_menu WHERE tenant_id = 1 AND code = 'training_operation_flow' AND deleted = 0 LIMIT 1),
    4300
  ),
  'page',
  'fen_ban',
  '分班',
  '/training/fen-ban',
  'training/TrainingClassDivisionPage',
  'train:division:view',
  'ApartmentOutlined',
  4,
  1,
  'enabled',
  NOW(),
  NOW(),
  1,
  1,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE tenant_id = 1 AND code = 'fen_ban' AND deleted = 0
);

-- 3) 隐藏遗留 train_class-list，避免与 fen_ban 双入口
UPDATE sys_menu
SET
  visible = 0,
  deleted = 1,
  updated_at = NOW(),
  updated_by = 1
WHERE tenant_id = 1
  AND code = 'train_class-list'
  AND deleted = 0;

-- 4) 拥有需求管理/方案规划的角色授予 fen_ban
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  207393809000403000 + rm.role_id,
  rm.tenant_id,
  rm.role_id,
  m_fen.id,
  NOW(),
  NOW(),
  1,
  1,
  0
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.tenant_id = rm.tenant_id AND m.deleted = 0
INNER JOIN sys_menu m_fen ON m_fen.tenant_id = rm.tenant_id AND m_fen.code = 'fen_ban' AND m_fen.deleted = 0
WHERE rm.deleted = 0
  AND m.code IN ('train_needs', 'pxan_fang_gui_hua', 'pxyun_ying_liu_cheng', 'training_operation_flow')
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu x
    WHERE x.deleted = 0
      AND x.tenant_id = rm.tenant_id
      AND x.role_id = rm.role_id
      AND x.menu_id = m_fen.id
  );
