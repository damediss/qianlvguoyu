-- Add task management menu hierarchy under question-bank (parent 4700)
-- ???? (level 2 directory, sorted first)
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (4704, 1, 4700, 'directory', 'task_management', '任务管理', '/task-management', NULL, 'task:view', 'FileSearchOutlined', 0, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4705, 1, 4704, 'directory', 'dev_task', '开发任务', '/task-management/dev-task', NULL, 'task:dev:view', 'ProjectOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4706, 1, 4705, 'page', 'dev_task_detail', '细目表', '/task-management/dev-task/detail', 'exam/DevTaskDetail', 'task:dev:detail:view', 'TableOutlined', 1, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (5367, 1, 4704, 'task:view', '查看任务管理', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5368, 1, 4705, 'task:dev:view', '查看开发任务', 'view', 'enabled', NOW(), NOW(), 1, 1, 0),
  (5369, 1, 4706, 'task:dev:detail:view', '查看细目表', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- Grant to PLATFORM_ADMIN role (id=2000, tenant_id=1)
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604918, 1, 2000, 4704, NOW(), NOW(), 1, 1, 0),
  (604919, 1, 2000, 4705, NOW(), NOW(), 1, 1, 0),
  (604920, 1, 2000, 4706, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (705572, 1, 2000, 5367, NOW(), NOW(), 1, 1, 0),
  (705573, 1, 2000, 5368, NOW(), NOW(), 1, 1, 0),
  (705574, 1, 2000, 5369, NOW(), NOW(), 1, 1, 0);
