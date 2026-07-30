-- 毕业管理菜单（TrainingGraduationPage）
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission, icon,
  sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  5040, 1, 4300, 'page', 'train_graduation', '毕业管理', '/training/graduation',
  'training/TrainingGraduation', 'train:graduation:view', 'SolutionOutlined', 43, 1, 'enabled',
  NOW(), NOW(), 1, 1, 0
);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, permission_type, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5680, 1, 5040, 'train:graduation:view', '查看毕业管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5681, 1, 5040, 'train:graduation:update', '毕业操作', 'update', 'enabled', NOW(), NOW(), 1, 1, 0);
