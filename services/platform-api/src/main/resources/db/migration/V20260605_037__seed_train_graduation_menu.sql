-- 毕业管理菜单（TrainingGraduationPage）
-- 空库幂等化（踩坑 #34 修复）：链上建表（V20260505_001）的列名是 permission_code / action，
-- 本种子原写的 permission / permission_type 是 PHP 老表列名，空库必报 Unknown column。
-- 改为链上列名（也正是 MenuEntity/权限码加载实际读取的列）；存量库本版本已应用不会重跑。
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  5040, 1, 4300, 'page', 'train_graduation', '毕业管理', '/training/graduation',
  'training/TrainingGraduation', 'train:graduation:view', 'SolutionOutlined', 43, 1, 'enabled',
  NOW(), NOW(), 1, 1, 0
);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5680, 1, 5040, 'train:graduation:view', '查看毕业管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5681, 1, 5040, 'train:graduation:update', '毕业操作', 'update', 'enabled', NOW(), NOW(), 1, 1, 0);
